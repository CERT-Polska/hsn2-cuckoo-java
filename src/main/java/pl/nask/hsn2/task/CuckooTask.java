/*
 * Copyright (c) NASK, NCSC
 *
 * This file is part of HoneySpider Network 2.0.
 *
 * This is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.nask.hsn2.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.InputDataException;
import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.TaskContext;
import pl.nask.hsn2.connector.CuckooConnection;
import pl.nask.hsn2.connector.CuckooException;
import pl.nask.hsn2.connector.CuckooRESTConnector;
import pl.nask.hsn2.wrappers.ObjectDataWrapper;
import pl.nask.hsn2.wrappers.ParametersWrapper;

public class CuckooTask implements Task {

	private static final Logger LOGGER = LoggerFactory.getLogger(CuckooTask.class);

	private final TaskContext jobContext;
	private final ObjectDataWrapper data;

	private long cuckooTaskId;

	private boolean save_pcap = false;
	private boolean save_report_json = true;
	private boolean save_report_html = false;
	private boolean save_screenshots = true;
	private boolean fail_on_error = false;
	private int retry = 3;
	private int retry_wait = 5;
	private ParametersWrapper parameters;
	private String cuckooProcPath;
	private CuckooRESTConnector cuckooConector;
	private Set<NameValuePair> cuckooParams = new HashSet<>();

	private boolean cleanJobData;

	public CuckooTask(TaskContext jobContext, ParametersWrapper parameters, ObjectDataWrapper data,
			String cuckooProcPath, boolean cleanJobData) throws ParameterException {
		this.jobContext = jobContext;
		this.data = data;
		this.cuckooProcPath = cuckooProcPath;
		this.parameters = parameters;
		this.cleanJobData = cleanJobData;
		applyParameters();
		cuckooConector = new CuckooRESTConnector();
	}

	private void applyParameters() throws ParameterException {
		this.save_pcap = parameters.getBoolean("save_pcap", save_pcap);
		this.save_report_json = parameters.getBoolean("save_report_json", save_report_json);
		this.save_report_html = parameters.getBoolean("save_report_html", save_report_html);
		this.save_screenshots = parameters.getBoolean("save_screenshots", save_screenshots);
		this.fail_on_error = parameters.getBoolean("fail_on_error", fail_on_error);
		this.retry = parameters.getInt("retry", retry);
		this.retry_wait = parameters.getInt("retry_wait", retry_wait);
		extractCuckooParam("timeout", cuckooParams);
		extractCuckooParam("priority", cuckooParams);
		extractCuckooParam("package", cuckooParams);
		extractCuckooParam("vm_id", "machine", cuckooParams);

	}

	private void extractCuckooParam(String paramName, Set<NameValuePair> cuckooParams) {
		extractCuckooParam(paramName, paramName, cuckooParams);
	}

	private void extractCuckooParam(String paramName, String cuckooParamName, Set<NameValuePair> cuckooParams) {
		if (parameters.hasParam(paramName)) {
			try {
				cuckooParams.add(new NameValuePair(cuckooParamName, parameters.get(paramName)));
			} catch (ParameterException e) {
				LOGGER.warn("Problem with parameter. Ignore: " + paramName, e);
			}
		}
	}

	public boolean takesMuchTime() {
		return true;
	}

	public void process() throws ParameterException, ResourceException, StorageException, InputDataException {
		Long contentId = data.getReferenceId("content");

		boolean sent = false;
		int retries = 0;
		while (!sent) {
			try {
				if (contentId != null) {
					File file = downloadFile(contentId);
					cuckooTaskId = cuckooConector.sendFile(file, cuckooParams);
				} else {
					String urlForProc = data.getUrlForProcessing();
					cuckooTaskId = cuckooConector.sendURL(urlForProc, cuckooParams);
				}
				sent = true;
			} catch (CuckooException e) {
				String msg = e.getMessage();
				if (this.retry > 0 && retries < this.retry) {
					LOGGER.warn("{} - retry in {} mins...", msg, this.retry_wait);
					retries++;
					try {
						TimeUnit.MINUTES.sleep(retry_wait);
					} catch (InterruptedException ie) {
						sent = true;
						return;
					}
					LOGGER.debug("Retrying ({})...", retries);
					//continue;
				} else {
					LOGGER.error("{} - retry limit ({}) exceeded, aborting...", msg, this.retry);
					if (fail_on_error) {
						throw new ResourceException(e.getMessage(), e);
					} else {
						jobContext.addAttribute("cuckoo_error", msg);
						return;
					}
				}
			}
		}

		boolean done = false;
		while (!done) {
			try {
				TimeUnit.SECONDS.sleep(30);
				done = isTaskDone();
			} catch (InterruptedException e) {
				done = true;
				return;
			}
		}

		processDataAndCalculateRating();
		if (save_report_html) {
			saveHtmlReport();
		}
		if (save_report_json) {
			saveJsonReport();
		}
		if (save_pcap) {
			savePcap();
		}
		if (save_screenshots) {
			saveScreenshots();
		}
		if (cleanJobData) {
			cuckooConector.deleteTaskData(cuckooTaskId);
		}
	}

	private File downloadFile(Long contentId) throws StorageException, ResourceException {
		try {
			byte[] fileInByte = IOUtils.toByteArray(jobContext.getFileAsInputStream(contentId));
			String fileName = data.getString("filename");

			if (fileName == null) {
				fileName = DigestUtils.md5Hex(fileInByte);
			}
			File file = new File(cuckooProcPath, fileName);
			FileUtils.writeByteArrayToFile(file, fileInByte);
			return file;
		} catch (IOException e) {
			throw new ResourceException(e.getMessage(), e);
		}
	}

	private void saveHtmlReport() throws StorageException, ResourceException {
		try (CuckooConnection conn = cuckooConector.getHtmlReportAsStream(cuckooTaskId)) {
			long refId = jobContext.saveInDataStore(conn.getBodyAsInputStream());
			jobContext.addReference("cuckoo_report_html", refId);
		} catch (CuckooException e) {
			if (fail_on_error) {
				throw new ResourceException(e.getMessage(), e);
			} else {
				jobContext.addAttribute("cuckoo_error", e.getMessage());
				return;
			}
		} catch (IOException e) {
			LOGGER.warn("Can not close connection.", e);
		}
	}

	private void saveJsonReport() throws StorageException, ResourceException {
		try (CuckooConnection conn = cuckooConector.getJsonReportAsStream(cuckooTaskId)) {
			LOGGER.info("Saving JSON report file, status from cuckoo: " + conn.getResultStatusCode());
			long refId = jobContext.saveInDataStore(conn.getBodyAsInputStream());
			jobContext.addReference("cuckoo_report_json", refId);
		} catch (CuckooException e) {
			if (fail_on_error) {
				throw new ResourceException(e.getMessage(), e);
			} else {
				jobContext.addAttribute("cuckoo_error", e.getMessage());
				return;
			}
		} catch (IOException e) {
			LOGGER.warn("Can not close connection.", e);
		}
	}

	private void savePcap() throws StorageException, ResourceException {
		try (CuckooConnection conn = cuckooConector.getPcapAsStream(cuckooTaskId)) {
			LOGGER.info("Saving PCAP file, status from cuckoo: " + conn.getResultStatusCode());
			Path tempFile = copyToTempFile(conn.getBodyAsInputStream());
			String md5 = DigestUtils.md5Hex(Files.readAllBytes(tempFile));
			String sha1 = DigestUtils.shaHex(Files.readAllBytes(tempFile));
			long refId = jobContext.saveInDataStore(new FileInputStream(tempFile.toFile()));
			tempFile.toFile().delete();

			jobContext.addReference("cuckoo_pcap", refId);
			jobContext.addAttribute("cuckoo_pcap_md5", md5);
			jobContext.addAttribute("cuckoo_pcap_sha1", sha1);
		} catch (CuckooException e) {
			if (fail_on_error) {
				throw new ResourceException(e.getMessage(), e);
			} else {
				jobContext.addAttribute("cuckoo_error", e.getMessage());
				return;
			}
		} catch (IOException e) {
			LOGGER.warn("Cannot store PCAP file", e);
		}
	}

	private void saveScreenshots() throws StorageException, ResourceException {
		try (CuckooConnection conn = cuckooConector.getScreenshotsAsStream(cuckooTaskId)) {
			LOGGER.info("Saving screenshots, status from cuckoo: " + conn.getResultStatusCode());
			long refId = jobContext.saveInDataStore(conn.getBodyAsInputStream());
			jobContext.addReference("cuckoo_screenshot", refId);
		} catch (CuckooException e) {
			if (fail_on_error) {
				throw new ResourceException(e.getMessage(), e);
			} else {
				jobContext.addAttribute("cuckoo_error", e.getMessage());
				return;
			}
		} catch (IOException e) {
			LOGGER.warn("Can not close connection.", e);
		}
	}

	private boolean isTaskDone() throws ResourceException {
		JSONObject taskInfo;
		try {
			taskInfo = cuckooConector.getTaskInfo(cuckooTaskId);
		} catch (CuckooException e) {
			if (fail_on_error) {
				throw new ResourceException(e.getMessage(), e);
			} else {
				jobContext.addAttribute("cuckoo_error", e.getMessage());
				return true;
			}
		}
		String status = taskInfo.getString("status");
		if ("reported".equals(status)) {
			jobContext.addAttribute("cuckoo_time_start", taskInfo.getString("started_on"));
			try {
				jobContext.addAttribute("cuckoo_time_stop", taskInfo.getString("completed_on"));
			} catch (JSONException e) {
				LOGGER.warn(e.getMessage());
				LOGGER.warn("Inserting current date as \"cuckoo_time_stop\"");
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date date = new Date();
				jobContext.addAttribute("cuckoo_time_stop", dateFormat.format(date));
			}
			return true;
		} else {
			return false;
		}
	}

	private void processDataAndCalculateRating() throws ResourceException {
		SignatureProcessor sigProcessor = new SignatureProcessor();
		try (CuckooConnection conn = cuckooConector.getJsonReportAsStream(cuckooTaskId)) {
			sigProcessor.process(conn.getBodyAsInputStream());
		} catch (IOException | CuckooException e) {
			if (fail_on_error) {
				throw new ResourceException(e.getMessage(), e);
			} else {
				jobContext.addAttribute("cuckoo_error", e.getMessage());
				return;
			}
		}
		Process process = sigProcessor.getMaxRateProcess();
		String reason = "";
		double score = 0.0;
		if (process != null) {
			reason = process.getSignatureNamesAsString();
			score = process.getScore();
		}
		for (Entry<String, Double> entry : sigProcessor.getAdditionalScores().entrySet()) {
			score += entry.getValue();
			if (!"".equals(reason)) {
				reason += ", ";
			}
			reason += entry.getKey();
		}

		jobContext.addAttribute("cuckoo_classification", calculate(score));
		jobContext.addAttribute("cuckoo_classification_reason", reason);
	}

	private String calculate(double score) {
		if (score < 1) {
			return "benign";
		} else if (score >= 1.5) {
			return "malicious";
		} else {
			return "suspicious";
		}
	}

	private Path copyToTempFile(InputStream is) throws IOException {
		Path tempFile = Files.createTempFile(jobContext.getJobId() + "_" + jobContext.getReqId(), "_cuckoo_pcap");
		Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
		is.close();
		return tempFile;
	}
}
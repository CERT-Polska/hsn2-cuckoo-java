/*
 * Copyright (c) NASK, NCSC
 *
 * This file is part of HoneySpider Network 2.1.
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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
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
	private static final String ERROR_KEY = "cuckoo_error";
	private static final double THRESHOLD_SUSPICIOUS = 1.0;
	private static final double THRESHOLD_MALICIOUS = 1.5;
	private static final int DEFAULT_RETRIES = 3;
	private static final int DEFAULT_RETRY_WAIT = 5;
	private static final int DEFAULT_ANALYSIS_WAIT_SECS = 30;

	private final TaskContext jobContext;
	private final ObjectDataWrapper data;

	private long cuckooTaskId;

	private boolean savePcap = false;
	private boolean saveReportJson = true;
	private boolean saveReportHtml = false;
	private boolean saveScreenshots = true;
	private boolean failOnError = false;
	private int retry = DEFAULT_RETRIES;
	private int retryWait = DEFAULT_RETRY_WAIT;
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
		savePcap = parameters.getBoolean("save_pcap", savePcap);
		saveReportJson = parameters.getBoolean("save_report_json", saveReportJson);
		saveReportHtml = parameters.getBoolean("save_report_html", saveReportHtml);
		saveScreenshots = parameters.getBoolean("save_screenshots", saveScreenshots);
		failOnError = parameters.getBoolean("fail_on_error", failOnError);
		retry = parameters.getInt("retry", retry);
		retryWait = parameters.getInt("retry_wait", retryWait);
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

	public final boolean takesMuchTime() {
		return true;
	}

	public final void process() throws ParameterException, ResourceException, StorageException, InputDataException {
		Long contentId = data.getReferenceId("content");

		boolean sent = false;
		int retries = 0;
		while (!sent) {
			try {
				if (contentId != null) {
					File file = downloadFile(contentId);
					cuckooTaskId = cuckooConector.sendFile(file, cuckooParams);
				} else {
					String urlForProc = prepareUrlForProcessing();
					cuckooTaskId = cuckooConector.sendURL(urlForProc, cuckooParams);
				}
				sent = true;
			} catch (CuckooException e) {
				String msg = e.getMessage();
				if (retry > 0 && retries < retry) {
					LOGGER.warn("{} - retry in {} mins...", msg, retryWait);
					retries++;
					try {
						TimeUnit.MINUTES.sleep(retryWait);
					} catch (InterruptedException ie) {
						sent = true;
						return;
					}
					LOGGER.debug("Retrying ({})...", retries);
					//continue;
				} else {
					LOGGER.error("{} - retry limit ({}) exceeded, aborting...", msg, retry);
					if (failOnError) {
						throw new ResourceException("Cannot connect to Cuckoo: " + msg, e);
					} else {
						jobContext.addAttribute(ERROR_KEY, "Cannot connect to Cuckoo: " + msg);
						return;
					}
				}
			} catch (JSONException e) {
				String msg = e.getMessage();
				LOGGER.error("Cuckoo rejected the task: {}", msg);
				if (failOnError) {
					throw new ResourceException("Cuckoo rejected the task: " + msg, e);
				} else {
					jobContext.addAttribute(ERROR_KEY, "Cuckoo rejected the task: " + msg);
					return;
				}
			}
		}

		boolean done = false;
		while (!done) {
			try {
				TimeUnit.SECONDS.sleep(DEFAULT_ANALYSIS_WAIT_SECS);
				done = isTaskDone();
			} catch (InterruptedException e) {
				done = true;
				return;
			}
		}

		processDataAndCalculateRating();

		saveHtmlReport();
		saveJsonReport();
		savePcap();
		saveScreenshots();

		cleanJobData();
	}

	private String prepareUrlForProcessing() {
		String urlForProc = data.getUrlForProcessing();
		try {
			URI.create(urlForProc);
		} catch (IllegalArgumentException e) {
			LOGGER.info("Trying to encode URL: {}", e.getMessage());
			int index = urlForProc.indexOf('?');
			if (index != -1 && index + 1 < urlForProc.length()) {
				String addr = urlForProc.substring(0, index + 1);
				String query = urlForProc.substring(index + 1);
				try {
					urlForProc = addr + URLEncoder.encode(query, "UTF-8");
				} catch (UnsupportedEncodingException ex) {
					// it should never happen
					LOGGER.error(ex.getMessage());
				}
			}
		}
		return urlForProc;
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
		if (saveReportHtml) {
			try (CuckooConnection conn = cuckooConector.getHtmlReportAsStream(cuckooTaskId)) {
				long refId = jobContext.saveInDataStore(conn.getBodyAsInputStream());
				jobContext.addReference("cuckoo_report_html", refId);
			} catch (CuckooException e) {
				if (failOnError) {
					throw new ResourceException(e.getMessage(), e);
				} else {
					jobContext.addAttribute(ERROR_KEY, e.getMessage());
					return;
				}
			} catch (IOException e) {
				LOGGER.warn("Can not close connection.", e);
			}
		}
	}

	private void saveJsonReport() throws StorageException, ResourceException {
		if (saveReportJson) {
			try (CuckooConnection conn = cuckooConector.getJsonReportAsStream(cuckooTaskId)) {
				LOGGER.info("Saving JSON report file, status from cuckoo: " + conn.getResultStatusCode());
				long refId = jobContext.saveInDataStore(conn.getBodyAsInputStream());
				jobContext.addReference("cuckoo_report_json", refId);
			} catch (CuckooException e) {
				if (failOnError) {
					throw new ResourceException(e.getMessage(), e);
				} else {
					jobContext.addAttribute(ERROR_KEY, e.getMessage());
					return;
				}
			} catch (IOException e) {
				LOGGER.warn("Can not close connection.", e);
			}
		}
	}

	private void savePcap() throws StorageException, ResourceException {
		if (savePcap) {
			try (CuckooConnection conn = cuckooConector.getPcapAsStream(cuckooTaskId)) {
				LOGGER.info("Saving PCAP file, status from cuckoo: " + conn.getResultStatusCode());
				Path tempFile = copyToTempFile(conn.getBodyAsInputStream());
				String md5 = DigestUtils.md5Hex(Files.readAllBytes(tempFile));
				String sha1 = DigestUtils.shaHex(Files.readAllBytes(tempFile));
				long refId = jobContext.saveInDataStore(new FileInputStream(tempFile.toFile()));
				if (!tempFile.toFile().delete()) {
					LOGGER.warn("Couldn't delete the file: {}", tempFile);
				}

				jobContext.addReference("cuckoo_pcap", refId);
				jobContext.addAttribute("cuckoo_pcap_md5", md5);
				jobContext.addAttribute("cuckoo_pcap_sha1", sha1);
			} catch (CuckooException e) {
				if (failOnError) {
					throw new ResourceException(e.getMessage(), e);
				} else {
					jobContext.addAttribute(ERROR_KEY, e.getMessage());
					return;
				}
			} catch (IOException e) {
				LOGGER.warn("Cannot store PCAP file", e);
			}
		}
	}

	private void saveScreenshots() throws StorageException, ResourceException {
		if (saveScreenshots) {
			try (CuckooConnection conn = cuckooConector.getScreenshotsAsStream(cuckooTaskId)) {
				LOGGER.info("Saving screenshots, status from cuckoo: " + conn.getResultStatusCode());
				long refId = jobContext.saveInDataStore(conn.getBodyAsInputStream());
				jobContext.addReference("cuckoo_screenshot", refId);
			} catch (CuckooException e) {
				if (failOnError) {
					throw new ResourceException(e.getMessage(), e);
				} else {
					jobContext.addAttribute(ERROR_KEY, e.getMessage());
					return;
				}
			} catch (IOException e) {
				LOGGER.warn("Can not close connection.", e);
			}
		}
	}

	private void cleanJobData() {
		if (cleanJobData) {
			cuckooConector.deleteTaskData(cuckooTaskId);
		}
	}

	private boolean isTaskDone() throws ResourceException {
		JSONObject taskInfo;
		try {
			taskInfo = cuckooConector.getTaskInfo(cuckooTaskId);
		} catch (CuckooException e) {
			if (failOnError) {
				throw new ResourceException(e.getMessage(), e);
			} else {
				jobContext.addAttribute(ERROR_KEY, e.getMessage());
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
			if (failOnError) {
				throw new ResourceException(e.getMessage(), e);
			} else {
				jobContext.addAttribute(ERROR_KEY, e.getMessage());
				return;
			}
		}
		Process process = sigProcessor.getMaxRateProcess();
		StringBuffer reason = new StringBuffer("");
		double score = 0.0;
		if (process != null) {
			reason.append(process.getSignatureNamesAsString());
			score = process.getScore();
		}
		for (Entry<String, Double> entry : sigProcessor.getAdditionalScores().entrySet()) {
			score += entry.getValue();
			if (reason.length() > 0) {
				reason.append(", ");
			}
			reason.append(entry.getKey());
		}

		jobContext.addAttribute("cuckoo_classification", calculate(score));
		jobContext.addAttribute("cuckoo_classification_reason", reason.toString());
	}

	private String calculate(double score) {
		if (score >= THRESHOLD_MALICIOUS) {
			return "malicious";
		}
		if (score >= THRESHOLD_SUSPICIOUS) {
			return "suspicious";
		}
		return "benign";
	}

	private Path copyToTempFile(InputStream is) throws IOException {
		Path tempFile = Files.createTempFile(jobContext.getJobId() + "_" + jobContext.getReqId(), "_cuckoo_pcap");
		Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
		is.close();
		return tempFile;
	}
}
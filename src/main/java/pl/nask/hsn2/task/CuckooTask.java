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
import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
	
	private String urlForProc;
	private long cuckooTaskId;
	
	private boolean save_pcap = false;
	private boolean save_report_json = true;
	private boolean save_report_html = false;
	private boolean save_screenshots = true;
	private ParametersWrapper parameters;
	private String cuckooProcPath;
	private CuckooRESTConnector cuckooConector;
	private Set<NameValuePair> cuckooParams = new HashSet<>();

	public CuckooTask(TaskContext jobContext, ParametersWrapper parameters, ObjectDataWrapper data, String cuckooProcPath) {
		this.jobContext = jobContext;
		this.data = data;
		this.cuckooProcPath = cuckooProcPath;
        this.parameters = parameters;
        applyParameters();
        cuckooConector = new CuckooRESTConnector();
	}

	private void applyParameters() {
		this.save_pcap = parameters.getBoolean("save_pcap", save_pcap);
		this.save_report_json = parameters.getBoolean("save_report_json", save_report_json);
		this.save_report_html = parameters.getBoolean("save_report_html", save_report_html);
		this.save_screenshots = parameters.getBoolean("save_screenshots", save_screenshots);
		extractCuckooParam("timeout", cuckooParams);
		extractCuckooParam("priority", cuckooParams);
		extractCuckooParam("package", cuckooParams);
		extractCuckooParam("vm_id", "machine", cuckooParams);
		
	}
	
	private void extractCuckooParam (String paramName, Set<NameValuePair> cuckooParams){
		extractCuckooParam(paramName, paramName, cuckooParams);
	}
	
	private void extractCuckooParam (String paramName,String cuckooParamName, Set<NameValuePair> cuckooParams){
		if (parameters.hasParam(paramName)){
			try{
				cuckooParams.add(new NameValuePair(cuckooParamName, parameters.get(paramName)));
			}
			catch (ParameterException e){
				LOGGER.warn("Problem with parameter. Ignore: "+ paramName, e);
			}
		}
	}
	
	public boolean takesMuchTime() {
		return true;
	}

	public void process() throws ParameterException, ResourceException,	StorageException, InputDataException {
		Long contentId = data.getReferenceId("content");
		
		try{
			if (contentId != null){
				File file = downloadFile(contentId);
				cuckooTaskId = cuckooConector.sendFile(file, cuckooParams);
			}
			else{
				cuckooTaskId = cuckooConector.sendURL(urlForProc, cuckooParams);
			}
		}
		catch(CuckooException e){
			throw new ResourceException(e.getMessage(), e);
		}
		
		boolean done = false;
		while(!done){
			try {
				Thread.sleep(30 * 1000);
				done = isTaskDone();
			} catch (InterruptedException e) {
				done = true;
				return;
			}
		}
		processDataAndCalculateRating();
		if (save_report_html){
			saveHtmlReport();
		}
		if (save_report_json){
			saveJsonReport();
		}
		if (save_pcap){
			savePcap();
		}
		if (save_screenshots){
			saveScreenshots();
		}
	}

	private File downloadFile(Long contentId) throws StorageException, ResourceException {
		try{
			byte[] fileInByte = IOUtils.toByteArray(jobContext.getFileAsInputStream(contentId));
			String fileName = data.getString("filename");
			
			if (fileName == null){
				fileName = DigestUtils.md5Hex(fileInByte);
			}
			File file = new File(cuckooProcPath, fileName);
			FileUtils.writeByteArrayToFile(file, fileInByte);
			return file;
		} catch (IOException e) {
			throw new ResourceException(e.getMessage(),e);
		}
	}
	
	private void saveHtmlReport() throws StorageException, ResourceException {
		try(CuckooConnection conn = cuckooConector.getHtmlReportAsStream(cuckooTaskId)){
			long refId = jobContext.saveInDataStore(conn.getBodyAsInputStream());
			jobContext.addReference("cuckoo_report_html", refId);
		} catch (CuckooException e) {
			throw new ResourceException(e.getMessage(), e);
		} catch (IOException e) {
			LOGGER.warn("Can not close connection.", e);
		}
	}
	
	private void saveJsonReport() throws StorageException, ResourceException {
		try(CuckooConnection conn = cuckooConector.getJsonReportAsStream(cuckooTaskId)){
			long refId = jobContext.saveInDataStore(conn.getBodyAsInputStream());
			jobContext.addReference("cuckoo_report_json", refId);
		} catch (CuckooException e) {
			throw new ResourceException(e.getMessage(), e);
		} catch (IOException e) {
			LOGGER.warn("Can not close connection.", e);
		}
	}
	
	private void savePcap() throws  StorageException, ResourceException{
		try(CuckooConnection conn = cuckooConector.getPcapAsStream(cuckooTaskId)){
			long refId = jobContext.saveInDataStore(conn.getBodyAsInputStream());
			jobContext.addReference("cuckoo_pcap", refId);
		} catch (CuckooException e) {
			throw new ResourceException(e.getMessage(), e);
		} catch (IOException e) {
			LOGGER.warn("Can not close connection.", e);
		}
	}
	
	private void saveScreenshots() throws  StorageException, ResourceException{
		try(CuckooConnection conn = cuckooConector.getScreenshotsAsStream(cuckooTaskId)){
			long refId = jobContext.saveInDataStore(conn.getBodyAsInputStream());
			jobContext.addReference("cuckoo_pcap", refId);
		} catch (CuckooException e) {
			throw new ResourceException(e.getMessage(), e);
		} catch (IOException e) {
			LOGGER.warn("Can not close connection.", e);
		}
	}

	private boolean isTaskDone() throws ResourceException {
		JSONObject taskInfo;
		try {
			taskInfo = cuckooConector.getTaskInfo(cuckooTaskId);
		} catch (CuckooException e) {
			throw new ResourceException(e.getMessage(), e);
		}
		String status = taskInfo.getString("status");
		if ("reported".equals(status)){
			jobContext.addAttribute("cuckoo_time_start", taskInfo.getString("started_on"));
			jobContext.addAttribute("cuckoo_time_stop", taskInfo.getString("completed_on"));
			return true;
		}
		else {
			return false;
		}
	}
	
	private void processDataAndCalculateRating() throws ResourceException{
		SignatureProcessor sigProcessor = new SignatureProcessor();
		try(CuckooConnection conn = cuckooConector.getJsonReportAsStream(cuckooTaskId)){
			sigProcessor.process(conn.getBodyAsInputStream());
		} catch (IOException | CuckooException e) {
			throw new ResourceException(e.getMessage(), e);
		}
		Process process = sigProcessor.getMaxRateProcess();
		
		String reason = process.getSignatureNamesAsString();
		double score = process.getScore();
		for (Entry<String, Double> entry : sigProcessor.getAdditionalScores().entrySet()){
			score += entry.getValue();
			reason += ", "+ entry.getKey();
		}
		
		jobContext.addAttribute("cuckoo_classification", calculate(score));
		jobContext.addAttribute("cuckoo_classification_reason", reason);
	}
	
	private String calculate(double score){
		if(score < 1){
			return "benign";
		}else if(score >= 1.5){
			return "malicious";
		}
		else{
			return "suspicious";
		}
	}
}
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.InputDataException;
import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.TaskContext;
import pl.nask.hsn2.wrappers.ObjectDataWrapper;
import pl.nask.hsn2.wrappers.ParametersWrapper;

public class CuckooTask implements Task {

	private static final Logger LOGGER = LoggerFactory.getLogger(CuckooTask.class);
	private static final String SEND_TASK = "/tasks/create/url";
	private static final String CHECK_TASK = "/tasks/view/";
	private static final String GET_REPORT = "/tasks/report/";
	private static final String GET_PCAP = "/pcap/get/";
	private static final String GET_SCREENSHOTS = "/tasks/screenshots/";

	private final TaskContext jobContext;

	private String cuckooURL;
	private String urlForProc;
	private long cuckooTaskId;

	public CuckooTask(TaskContext jobContext, ParametersWrapper parameters, ObjectDataWrapper data, String cuckooURL) {
		this.jobContext = jobContext;
		this.urlForProc = data.getUrlForProcessing();
		this.cuckooURL = cuckooURL;
        applyParameters(parameters);
	}

	private void applyParameters(ParametersWrapper parameters) {
		//TODO: implement
		
	}

	public boolean takesMuchTime() {
		return true;
	}

	public void process() throws ParameterException, ResourceException,	StorageException, InputDataException {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(cuckooURL + SEND_TASK).openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			out.writeBytes("url="+ urlForProc);
			out.close();
			conn.connect();
			
			if (conn.getResponseCode() == 200){
				String result = getResult(conn);
				JSONObject taskIdObject = new JSONObject(result);
				cuckooTaskId = taskIdObject.getLong("task_id");
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
				saveReport("html");
				saveReport("json");
				//saveReport(taskId, "all");
				saveStream(new URL(cuckooURL + GET_PCAP + cuckooTaskId), "cuckoo_pcap");
				saveStream(new URL(cuckooURL + GET_SCREENSHOTS + cuckooTaskId), "cuckoo_screenshots");
			}
			else{
				throw new RuntimeException("Not implemented");
			}
		} catch (IOException e) {
			throw new RuntimeException("Not implemented");
		}
		
	}
	
	private void saveReport(String type) throws IOException, MalformedURLException, StorageException, ResourceException {
		saveStream(new URL(cuckooURL + GET_REPORT + cuckooTaskId +"/"+ type), "cuckoo_report_" +type);
	}
	
	private void saveStream(URL url, String attrName) throws IOException, StorageException, ResourceException{
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.connect();
		if (conn.getResponseCode() == 200){
			try(InputStream stream = conn.getInputStream()){
				long refId = jobContext.saveInDataStore(stream);
				
				jobContext.addReference(attrName, refId);
			}
		}
		else{
			throw new RuntimeException("Not implemented");
		}
	}

	private String getResult(HttpURLConnection conn) throws IOException {
		String result = "";
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
			for(String line = reader.readLine(); line != null; line = reader.readLine())
				result += line;
		}
		return result;
	}

	private boolean isTaskDone() throws MalformedURLException, IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(cuckooURL + CHECK_TASK + cuckooTaskId).openConnection();
		conn.connect();
		
		if (conn.getResponseCode() == 200){
			String result = getResult(conn);
			JSONObject taskObject = new JSONObject(result).getJSONObject("task");
			
			String status = taskObject.getString("status");
			if ("reported".equals(status)){
				jobContext.addAttribute("cuckoo_time_start", taskObject.getString("started_on"));
				jobContext.addAttribute("cuckoo_time_stop", taskObject.getString("completed_on"));
				return true;
			}
		}
		else{
			throw new RuntimeException("Not implemented");
		}
		return false;
	}
	
	private void processDataAndCalculateRating() throws IOException{
		SignatureProcessor sigProcessor = new SignatureProcessor(cuckooTaskId, cuckooURL + GET_REPORT);
		sigProcessor.process();
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
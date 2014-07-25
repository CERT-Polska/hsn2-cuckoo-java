package pl.nask.hsn2.task;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
				long taskId= taskIdObject.getLong("task_id");
				boolean done = false;
				while(!done){
					try {
						Thread.sleep(30 * 1000);
						done = isTaskDone(taskId);
					} catch (InterruptedException e) {
						done = true;
					}
				}
				saveReport(taskId, "html");
				saveReport(taskId, "json");
				saveReport(taskId, "all");
				saveStream(new URL(cuckooURL + GET_PCAP + taskId), "cuckoo_pcap");
				saveStream(new URL(cuckooURL + GET_SCREENSHOTS + taskId), "cuckoo_screenshots");
			}
			else{
				throw new RuntimeException("Not implemented");
			}
		} catch (IOException e) {
			throw new RuntimeException("Not implemented");
		}
		
	}

	private void saveReport(long taskId, String type) throws IOException, MalformedURLException, StorageException, ResourceException {
		saveStream(new URL(cuckooURL + GET_REPORT + taskId+ "/" +type), "cuckoo_report_" +type);
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

	private boolean isTaskDone(long taskId) throws MalformedURLException, IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(cuckooURL + CHECK_TASK + taskId).openConnection();
		conn.connect();
		
		if (conn.getResponseCode() == 200){
			String result = getResult(conn);
			JSONObject taskObject = new JSONObject(result).getJSONObject("task");
			
			String status = taskObject.getString("status");
			if ("reported".equals(status)){
				LOGGER.info("OK!!");
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

}

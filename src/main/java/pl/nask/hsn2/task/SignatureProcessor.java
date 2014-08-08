package pl.nask.hsn2.task;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class SignatureProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(SignatureProcessor.class);
	private long cuckooTaskId;
	private String reportURL;
	
	private Map<Long, Process> dataWithPid = new HashMap<>();
	private Map<String, Double> data = new HashMap<>();
	
	public SignatureProcessor(long cuckooTaskId, String reportURL) {
		this.cuckooTaskId = cuckooTaskId;
		this.reportURL = reportURL;
	}
	
	public void process() throws IOException {
		extractSignatures();
	}
	
	private void extractSignatures() throws IOException {
		URL url = new URL(reportURL + cuckooTaskId +"/json");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.connect();
		if (conn.getResponseCode() == 200){
			try(
					InputStream stream = conn.getInputStream();
					JsonReader reader = new JsonReader(new InputStreamReader(stream))
			){
				reader.beginObject();
				while (reader.hasNext()){
					String name = reader.nextName();
					if ("signatures".equals(name)){
						reader.beginArray();
						while (reader.hasNext()) {
							Signature<Map<String,Object>> signature = new Gson().fromJson(reader, new Signature<>().getClass());
							extractData(signature);
						}
						reader.endArray();
					}
					else{
						reader.skipValue();
					}
				}
				reader.endObject();
			}
		}
		else{
			throw new RuntimeException("Not implemented");
		}
	}
	
	private void extractData(Signature<Map<String,Object>> signature){
		for (Map<String,Object> oneData : signature.getData()){
			Double id = (Double)oneData.get("process_id");
			if (id != null){
				Long pid = id.longValue();
				if (dataWithPid.containsKey(pid)){
					dataWithPid.get(pid).addSignature(signature.getName());
				}
				else{
					dataWithPid.put(pid, new Process(pid, signature.getName()));
				}
			}
			else{
				String name = signature.getName();
				if (!data.containsKey(name)){
					try{
						data.put(name, Rating.getValue(name));
					}
					catch (NoSuchElementException e){
						LOGGER.warn(e.getMessage() +" Skipped.");
					}
				}
			}
		}
	}
	
	public Process getMaxRateProcess(){
		Process[] processesList = dataWithPid.values().toArray(new Process[dataWithPid.size()]);
		Arrays.sort(processesList);
		return processesList[0];
	}
	
	public Map<String, Double> getAdditionalScores(){
		return data;
	}
}

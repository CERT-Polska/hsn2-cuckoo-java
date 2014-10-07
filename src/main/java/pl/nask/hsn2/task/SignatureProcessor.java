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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
	
	private Map<Long, Process> dataWithPid = new HashMap<>();
	private Map<String, Double> data = new HashMap<>();
	
	public SignatureProcessor() {
	}
	
	public void process(InputStream stream) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(stream));
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
		return processesList.length == 0 ? null : processesList[0];
	}
	
	public Map<String, Double> getAdditionalScores(){
		return data;
	}
}

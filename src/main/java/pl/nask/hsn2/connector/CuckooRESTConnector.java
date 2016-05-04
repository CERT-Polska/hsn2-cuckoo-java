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
package pl.nask.hsn2.connector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.ResourceException;

public class CuckooRESTConnector {
	private static final Logger LOGGER = LoggerFactory.getLogger(CuckooRESTConnector.class);
	
	private static final String SEND_URL_TASK = "/tasks/create/url";
	private static final String SEND_FILE_TASK = "/tasks/create/file";
	private static final String CHECK_TASK = "/tasks/view/";
	private static final String GET_REPORT = "/tasks/report/";
	private static final String GET_PCAP = "/pcap/get/";
	private static final String GET_SCREENSHOTS = "/tasks/screenshots/";
	private static final String DELETE_TASK = "/tasks/delete/";
	private static String cuckooURL = null;
	
	public static void setCuckooURL(String cuckooURL){
		if (cuckooURL != null){
			CuckooRESTConnector.cuckooURL = cuckooURL;
		}
		else{
			LOGGER.warn("Cuckoo URL already set!");
		}
	}
	
	public final void deleteTaskData(long cuckooTaskId) {
		try(CuckooConnection connection = connect(cuckooURL + DELETE_TASK + cuckooTaskId)){
			int status = connection.getResultStatusCode();
			switch (status) {
			case HttpStatus.SC_OK: 
				LOGGER.info("Cuckoo: task data deleted: " + cuckooTaskId);
				break;
			case HttpStatus.SC_NOT_FOUND:
				LOGGER.warn("Cuckoo: error deleting task data, task not found: " + cuckooTaskId);
				break;
			case HttpStatus.SC_INTERNAL_SERVER_ERROR:
				LOGGER.warn("Cuckoo: error deleting task data, could not delete task data: " + cuckooTaskId);
				break;
			default:
				LOGGER.warn("Cuckoo: error deleting task data (unknown status code: " + status + "), task: " + cuckooTaskId);
			}
		} catch (CuckooException  e) {
			LOGGER.warn("Cuckoo: error deleting task data: " + e.getMessage() + ", task: " + cuckooTaskId, e);
		} catch (IOException e) {
			LOGGER.warn("Error closing connection while deleting task data for task: " + cuckooTaskId, e);
		}
	}
	
	public final long sendFile(File file, Set<NameValuePair> cuckooParams) throws CuckooException, ResourceException{
		
		PostMethod post = new PostMethod(cuckooURL + SEND_FILE_TASK);
		try {
			int size = cuckooParams.size();
			Part[] parts = new Part[size + 1];
            int i = 0;
			for (NameValuePair pair : cuckooParams){
            	parts[i] = new StringPart(pair.getName(), pair.getValue());
            	i++;
			}
            parts[i] = new FilePart("file", file);
            
			RequestEntity entity = new MultipartRequestEntity(parts, post.getParams());
			post.setRequestEntity(entity);
			return sendPost(post);
		} 
		catch (FileNotFoundException e){
			LOGGER.error("This should never happen!", e);
			throw new ResourceException(e.getMessage(), e);
		}
		
	}

	public final long sendURL(String urlForProc, Set<NameValuePair> cuckooParams) throws CuckooException {
		PostMethod post = new PostMethod(cuckooURL + SEND_URL_TASK);
		post.addParameter(new NameValuePair("url", urlForProc));
		for (NameValuePair pair : cuckooParams){
			post.addParameter(pair);
		}
		return sendPost(post);
	}
	
	private long sendPost(PostMethod post) throws CuckooException{
		try {
			HttpClient client = new HttpClient();
			int status = client.executeMethod(post);
			if (status == HttpStatus.SC_OK) {
				String result = post.getResponseBodyAsString();
				JSONObject taskIdObject = new JSONObject(result);
				return taskIdObject.getLong("task_id");
			}
			else {
				throw new CuckooException("Unexpected response status: "+ status);
			}
		} catch (IOException e) {
			throw new CuckooException("Error while sending post data to " + cuckooURL + " : " + e.getMessage(), e);
		} 
		
		finally{
			post.releaseConnection();
		}
	}
	
	public final CuckooConnection getJsonReportAsStream(long cuckooTaskId) throws CuckooException{
		return getReportAsStream(cuckooTaskId, "json");
	}
	
	public final CuckooConnection getHtmlReportAsStream(long cuckooTaskId) throws CuckooException{
		return getReportAsStream(cuckooTaskId, "html");
	}
	
	public final CuckooConnection getPcapAsStream(long cuckooTaskId) throws CuckooException{
		return connect(cuckooURL + GET_PCAP + cuckooTaskId);
	}
	
	public final CuckooConnection getScreenshotsAsStream(long cuckooTaskId) throws CuckooException{
		return connect(cuckooURL + GET_SCREENSHOTS + cuckooTaskId);
	}
	
	public final JSONObject getTaskInfo(long cuckooTaskId) throws CuckooException{
		try(CuckooConnection connection = connect(cuckooURL + CHECK_TASK + cuckooTaskId)){
			String result = connection.getBodyAsString();
			if (result != null){
				return new JSONObject(result).getJSONObject("task");
			}
			else{
				throw new CuckooException("No task data.");
			}
		} catch (IOException e) {
			throw new CuckooException(e.getMessage(), e);
		}
	}
	
	private CuckooConnection getReportAsStream(long cuckooTaskId, String type) throws CuckooException{
		return connect(cuckooURL + GET_REPORT + cuckooTaskId +"/"+ type);
	}
	
	private CuckooConnection connect(String url) throws CuckooException{
		CuckooConnection connection = new CuckooConnection(url);
		connection.connect();
		return connection;
	}
	
}

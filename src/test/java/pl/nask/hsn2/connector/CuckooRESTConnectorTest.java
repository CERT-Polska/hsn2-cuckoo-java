package pl.nask.hsn2.connector;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.testng.annotations.Test;

import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;

public class CuckooRESTConnectorTest {
	@Mocked
	HttpClient httpClient;

	@Mocked
	GetMethod getMethod;

	@Test
	public void testDeleteTaskData() throws IOException {
		new NonStrictExpectations() {
			{
				httpClient.executeMethod((HttpMethod) any); returns(200);
				getMethod.getStatusCode(); returns(200, 404, 500, 302);
			}
		};

		CuckooRESTConnector connector = new CuckooRESTConnector();
		CuckooRESTConnector.setCuckooURL("http://localhost/");

		connector.deleteTaskData(0);
		connector.deleteTaskData(0);
		connector.deleteTaskData(0);
		connector.deleteTaskData(0);
	}

	@Test(expectedExceptions = { CuckooException.class })
	public void testGetTaskInfo() throws IOException, CuckooException {
		new NonStrictExpectations() {
			{
				httpClient.executeMethod((HttpMethod) any); returns(200);
				getMethod.getResponseBodyAsString(); returns(null);
			}
		};
		CuckooRESTConnector connector = new CuckooRESTConnector();
		connector.getTaskInfo(0);
	}

	@Test(expectedExceptions = { CuckooException.class })
	public void testSendPost() throws IOException, CuckooException {
		new NonStrictExpectations() {
			{
				httpClient.executeMethod((HttpMethod) any); returns(500);
			}
		};
		CuckooRESTConnector connector = new CuckooRESTConnector();
		PostMethod post = new PostMethod();
		Deencapsulation.invoke(connector, "sendPost", post);
	}
}

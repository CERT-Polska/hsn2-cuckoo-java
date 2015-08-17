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

package pl.nask.hsn2.service.task;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import mockit.Deencapsulation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import pl.nask.hsn2.InputDataException;
import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.TaskContext;
import pl.nask.hsn2.connector.CuckooException;
import pl.nask.hsn2.connector.CuckooRESTConnector;
import pl.nask.hsn2.task.CuckooTask;
import pl.nask.hsn2.task.Task;
import pl.nask.hsn2.wrappers.ObjectDataWrapper;
import pl.nask.hsn2.wrappers.ParametersWrapper;

public class CuckooTaskTest {
	@Mocked
	TaskContext jobContext;

	@Mocked
	ParametersWrapper parameters;

	@Mocked
	ObjectDataWrapper data;

	@Mocked
	HttpClient httpClient;

	@Mocked
	PostMethod postMethod;

	@Mocked
	GetMethod getMethod;

	private void setup(final boolean pcap, final boolean json, final boolean html, final boolean screenshots,
			final boolean error) throws ParameterException, IOException {
		final String cuckooTaskView = FileUtils.readFileToString(new File("src/test/resources/cuckoo_task_view.json"), "UTF-8");
		final String cuckooReport = FileUtils.readFileToString(new File("src/test/resources/cuckoo_report.json"), "UTF-8");

		new NonStrictExpectations() {
			{
				data.getUrlForProcessing(); result = "http://www.google.com/";
				parameters.getBoolean("save_pcap", false); result = pcap;
				parameters.getBoolean("save_report_json", true); result = json;
				parameters.getBoolean("save_report_html", false); result = html;
				parameters.getBoolean("save_screenshots", true); result = screenshots;
				parameters.getBoolean("fail_on_error", false); result = error;
				parameters.getInt("retry", 3); result = 0;
				parameters.getInt("retry_wait", 5); result = 0;
				parameters.hasParam("timeout"); result = true;
				parameters.get("timeout"); result = "10";

				httpClient.executeMethod((PostMethod) any); returns(200);
				getMethod.getResponseBodyAsString(); returns("{\"task_id\" : 1}", cuckooTaskView);
				getMethod.getResponseBodyAsStream(); returns(IOUtils.toInputStream(cuckooReport));
				getMethod.getStatusCode(); returns(200);

				jobContext.addAttribute("cuckoo_time_start", anyString); times = 1;
				jobContext.addAttribute("cuckoo_time_stop", anyString); times = 1;
				jobContext.addReference("cuckoo_report_json", anyLong); times = json ? 1 : 0;
				jobContext.addReference("cuckoo_report_html", anyLong); times = html ? 1 : 0;
				jobContext.addReference("cuckoo_pcap", anyLong); times = pcap ? 1 : 0;
				jobContext.addReference("cuckoo_screenshot", anyLong); times = screenshots ? 1 : 0;
			}
		};
	}

	@Test
	public void urlTaskTest() throws ParameterException, ResourceException, StorageException, InputDataException, IOException {
		setup(false, false, false, false, false);

		Task task = new CuckooTask(jobContext, parameters, data, "/tmp", false);
		task.process();

		Assert.assertTrue(task.takesMuchTime());
	}

	@Test
	public void urlTaskTestWithSave() throws ParameterException, ResourceException, StorageException, InputDataException, IOException {
		setup(true, true, true, true, false);

		Task task = new CuckooTask(jobContext, parameters, data, "/tmp", false);
		task.process();

		Assert.assertTrue(task.takesMuchTime());
	}

	@Test
	public void fileTaskTest() throws ParameterException, ResourceException, StorageException, InputDataException, IOException {
		new MockUp<MultipartRequestEntity>() {
			@Mock
			public void $init(Part[] parts, HttpMethodParams params) {

			}
		};

		setup(true, true, true, true, true);
		new NonStrictExpectations() {
			{
				data.getReferenceId("content"); result = 1L;
				jobContext.getFileAsInputStream(1L); returns(IOUtils.toInputStream("hello"));
			}
		};

		Task task = new CuckooTask(jobContext, parameters, data, "/tmp", true);
		task.process();
	}

	@Test
	public void testPrepareUrl() throws ParameterException {
		new NonStrictExpectations() {
			{
				data.getUrlForProcessing(); result = "http://www.google.com/abcdef?J%V$#%*¨&$%JH4u5hfek95n89756rtr4´t467545t4@#$%¨&*&H¨GFA#$%Y*J%V$#%*¨&$%JH";
			}
		};
		CuckooTask task = new CuckooTask(jobContext, parameters, data, "/tmp", false);
		String preparedUrl = Deencapsulation.invoke(task, "prepareUrlForProcessing");
		Assert.assertEquals(preparedUrl, "http://www.google.com/abcdef?J%25V%24%23%25*%C2%A8%26%24%25JH4u5hfek95n89756rtr4%C2%B4t467545t4%40%23%24%25%C2%A8%26*%26H%C2%A8GFA%23%24%25Y*J%25V%24%23%25*%C2%A8%26%24%25JH");
	}

	@Test
	public void testSaveJsonReport() throws ParameterException, IOException {
		new NonStrictExpectations() {
			{
				data.getUrlForProcessing(); result = "http://www.google.com/";
				getMethod.getResponseBodyAsStream(); returns(CuckooException.class);
				parameters.getBoolean("fail_on_error", false); result = false;

				jobContext.addAttribute("cuckoo_error", anyString); times = 1;
			}
		};
		CuckooTask task = new CuckooTask(jobContext, parameters, data, "/tmp", false);
		Deencapsulation.invoke(task, "saveJsonReport");
	}

	@Test(expectedExceptions = { ResourceException.class })
	public void testSaveHtmlReport() throws ParameterException, IOException {
		new NonStrictExpectations() {
			{
				data.getUrlForProcessing(); result = "http://www.google.com/";
				getMethod.getResponseBodyAsStream(); returns(CuckooException.class);
				parameters.getBoolean("fail_on_error", false); result = true;

				jobContext.addAttribute("cuckoo_error", anyString); times = 0;
			}
		};
		CuckooTask task = new CuckooTask(jobContext, parameters, data, "/tmp", false);
		Deencapsulation.invoke(task, "saveHtmlReport");
	}

	@Test
	public void testCalculate() throws ParameterException {
		new NonStrictExpectations() {
			{
				data.getUrlForProcessing(); result = "http://www.google.com/";
			}
		};
		CuckooTask task = new CuckooTask(jobContext, parameters, data, "/tmp", false);
		Assert.assertEquals(Deencapsulation.invoke(task, "calculate", 0.0), "benign");
		Assert.assertEquals(Deencapsulation.invoke(task, "calculate", 0.99), "benign");
		Assert.assertEquals(Deencapsulation.invoke(task, "calculate", 1.5), "malicious");
		Assert.assertEquals(Deencapsulation.invoke(task, "calculate", 2.0), "malicious");
		Assert.assertEquals(Deencapsulation.invoke(task, "calculate", 1.0), "suspicious");
		Assert.assertEquals(Deencapsulation.invoke(task, "calculate", 1.25), "suspicious");
	}

	@Test(expectedExceptions = { ResourceException.class })
	public void testIsTaskDoneException() throws ParameterException {
		new MockUp<CuckooRESTConnector>() {
			@Mock
			public long sendURL(String urlForProc, Set<NameValuePair> cuckooParams) throws CuckooException {
				throw new CuckooException("Test exception");
			}
		};
		new NonStrictExpectations() {
			{
				data.getUrlForProcessing(); result = "http://www.google.com/";
				parameters.getBoolean("fail_on_error", false); result = true;
			}
		};
		CuckooTask task = new CuckooTask(jobContext, parameters, data, "/tmp", false);
		Deencapsulation.invoke(task, "isTaskDone");
	}

	@Test
	public void testIsTaskDoneNotReported() throws ParameterException {
		new MockUp<CuckooRESTConnector>() {
			@Mock
			public long sendURL(String urlForProc, Set<NameValuePair> cuckooParams) throws CuckooException {
				return 1L;
			}
			@Mock
			public JSONObject getTaskInfo(long cuckooTaskId) throws CuckooException {
				JSONObject ret = new JSONObject();
				ret.put("status", "pending");
				return ret;
			}
		};
		new NonStrictExpectations() {
			{
				data.getUrlForProcessing(); result = "http://www.google.com/";
			}
		};
		CuckooTask task = new CuckooTask(jobContext, parameters, data, "/tmp", false);
		boolean result = Deencapsulation.invoke(task, "isTaskDone");
		Assert.assertFalse(result);
	}
}

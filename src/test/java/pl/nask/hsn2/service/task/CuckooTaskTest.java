package pl.nask.hsn2.service.task;

import org.testng.annotations.Test;

import pl.nask.hsn2.InputDataException;
import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.protobuff.Object.ObjectData;
import pl.nask.hsn2.task.CuckooTask;
import pl.nask.hsn2.task.Task;
import pl.nask.hsn2.wrappers.ObjectDataWrapper;

public class CuckooTaskTest {

	@Test
	public void taskTest() throws ParameterException, ResourceException, StorageException, InputDataException{
		Task task = new CuckooTask(null, null, new ObjectDataWrapper(ObjectData.getDefaultInstance()){
			@Override
			public String getUrlForProcessing() {
				return "http://google.com";
			}
		},"http://192.168.57.101:1337");
		
		task.process();
	}
}

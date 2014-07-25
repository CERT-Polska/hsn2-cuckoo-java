package pl.nask.hsn2.task;

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.TaskContext;
import pl.nask.hsn2.service.CuckooCommandLineParams;
import pl.nask.hsn2.wrappers.ObjectDataWrapper;
import pl.nask.hsn2.wrappers.ParametersWrapper;

public class CuckooServiceTaskFactory implements TaskFactory {

	private static CuckooCommandLineParams cmd;
		
	public static void prepereForAllThreads(CuckooCommandLineParams cmd) {
		CuckooServiceTaskFactory.cmd = cmd;
	}

	public Task newTask(TaskContext jobContext, ParametersWrapper parameters, ObjectDataWrapper data) throws ParameterException {
		return new CuckooTask(jobContext, parameters, data, cmd.getCuckooAdress());
	}
}

package pl.nask.hsn2.service;

import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;

import pl.nask.hsn2.CommandLineParams;
import pl.nask.hsn2.ServiceMain;
import pl.nask.hsn2.task.CuckooServiceTaskFactory;
import pl.nask.hsn2.task.TaskFactory;

public class CuckooService extends ServiceMain {

	public static void main(final String[] args) throws DaemonInitException, InterruptedException {
		ServiceMain cus = new CuckooService();
		cus.init(new DaemonContext() {
			public DaemonController getController() {
				return null;
			}
			public String[] getArguments() {
				return args;
			}
		});
		cus.start();
		cus.getServiceRunner().join();
	}
	
	@Override
	protected void prepareService() {
		// TODO Auto-generated method stub

	}

	@Override
	protected Class<? extends TaskFactory> initializeTaskFactory() {
		CuckooServiceTaskFactory.prepereForAllThreads((CuckooCommandLineParams)getCommandLineParams());
		return CuckooServiceTaskFactory.class;
	}
	
	@Override
	protected CommandLineParams newCommandLineParams() {
		return new CuckooCommandLineParams();
	}
}

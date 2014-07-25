package pl.nask.hsn2.service;

import pl.nask.hsn2.CommandLineParams;

public class CuckooCommandLineParams extends CommandLineParams {
	private static final OptionNameWrapper CUCKOO_ADDRESS = new OptionNameWrapper("ca", "cuckooAddress");
    
	@Override
	public void initOptions() {
		super.initOptions();
		addOption(CUCKOO_ADDRESS, "url", "API server address");
	}
	
	@Override
	protected void initDefaults() {
		super.initDefaults();
		setDefaultServiceNameAndQueueName("cuckoo");
		setDefaultValue(CUCKOO_ADDRESS, "http://localhost:1337");
	}
	
	public String getCuckooAdress(){
		return getOptionValue(CUCKOO_ADDRESS);
	}
	
	@Override
	protected void validate(){
		super.validate();
		//TODO: implement
	}
}

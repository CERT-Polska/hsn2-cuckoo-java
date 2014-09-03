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

package pl.nask.hsn2.service;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.CommandLineParams;

public class CuckooCommandLineParams extends CommandLineParams {
	private static final OptionNameWrapper CUCKOO_ADDRESS = new OptionNameWrapper("ca", "cuckooAddress");
	private static final OptionNameWrapper CUCKOO_PROC_PATH = new OptionNameWrapper("cpp", "cuckooProcPath");
	private static final Logger LOGGER = LoggerFactory.getLogger(CuckooCommandLineParams.class);
	@Override
	public void initOptions() {
		super.initOptions();
		addOption(CUCKOO_ADDRESS, "url", "API server address");
		addOption(CUCKOO_PROC_PATH, "path", "Path for processing files");
	}
	
	@Override
	protected void initDefaults() {
		super.initDefaults();
		setDefaultServiceNameAndQueueName("cuckoo");
		setDefaultValue(CUCKOO_ADDRESS, "http://localhost:1337");
		setDefaultValue(CUCKOO_PROC_PATH, "/tmp");
	}
	
	public String getCuckooAdress(){
		return getOptionValue(CUCKOO_ADDRESS);
	}
	
	public String getCuckooProcPath(){
		return getOptionValue(CUCKOO_PROC_PATH);
	}
	
	@Override
	protected void validate(){
		super.validate();
		String msg = "";
		if (!new File(getCuckooProcPath()).exists()){
			msg += "CuckooProcPath not exists!\n";
			LOGGER.error("CuckooProcPath does not exist! Path used: {}", getCuckooProcPath());
		}
		if (!"".equals(msg)){
			throw new IllegalStateException(msg);
		}
	}
}

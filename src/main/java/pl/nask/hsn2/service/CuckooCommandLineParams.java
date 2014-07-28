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

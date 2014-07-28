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

	@Test(enabled=false)
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

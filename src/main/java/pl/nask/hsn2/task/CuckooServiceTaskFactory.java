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

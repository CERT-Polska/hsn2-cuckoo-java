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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Rating {

	private static Map<String,Double> rates = new HashMap<>();
	private static final Logger LOGGER = LoggerFactory.getLogger(Rating.class);

	private Rating() {
		// this class cannot be instantiated, it's utility class
	}

	static{
		LOGGER.info("Load ratings from file: "+ new File("src/main/resources/ratings.conf").getAbsolutePath());
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("src/main/resources/ratings.conf")))){
			for(String line = reader.readLine(); line != null; line = reader.readLine()){
				String[] tokens = line.split("=");
				rates.put(tokens[0], Double.valueOf(tokens[1]));
			}
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public static double getValue(String name) {
		Double value = rates.get(name);
		if (value != null){
			return value.doubleValue();
		} else{
			throw new NoSuchElementException("No rate for: "+ name);
		}

	}
}

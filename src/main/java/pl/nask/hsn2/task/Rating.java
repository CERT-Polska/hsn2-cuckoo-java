package pl.nask.hsn2.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Rating {
	private static Map<String,Double> rates = new HashMap<>();
	static{
		System.out.println(new File("src/main/resources/ratings.conf").getAbsolutePath());
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("src/main/resources/ratings.conf")))){
			for(String line = reader.readLine(); line != null; line = reader.readLine()){
				String[] tokens = line.split("=");
				rates.put(tokens[0], Double.valueOf(tokens[1]));
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public static double getValue(String name) throws NoSuchElementException{
		Double value = rates.get(name);
		if (value != null){
			return value.doubleValue();
		}
		else{
			throw new NoSuchElementException("No rate for: "+ name);
		}
		
	}
}

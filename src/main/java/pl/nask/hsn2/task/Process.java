package pl.nask.hsn2.task;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Process implements Comparable<Process> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Process.class);
	private Long id;
	private Set<String> signatureNames = new HashSet<>();
	private Set<String> skipped = new HashSet<>();
	private double score = 0.0;
	
	public Process(Long id, String signatureName) {
		this.id = id;
		signatureNames.add(signatureName);
		score = Rating.getValue(signatureName);
	}
	
	public String getSignatureNamesAsString() {
		String out = "";
		boolean next = false;
		for(String name : signatureNames){
			if(next){
				out += ", ";
			}
			next = true;
			out += name;
		}
		return out;
	}
	
	public double getScore() {
		return score;
	}

	public void addSignature(String name){
		try{
			double d = Rating.getValue(name);
			if(signatureNames.add(name)){
				score =+ d;
			}
		}
		catch (NoSuchElementException e) {
			skipped.add(name);
			LOGGER.warn(e.getMessage() +" added to skipped list.");
		}
	}

	public Long getId(){
		return id;
	}
	
	@Override
	public int compareTo(Process o) {
		if(score < o.score){
			return -1;
		}
		else if (score > o.score){
			return 1;
		}
		else{
			return 0;
		}
	}
}

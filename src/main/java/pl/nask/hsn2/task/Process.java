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

	public final String getSignatureNamesAsString() {
		StringBuffer out = new StringBuffer("");
		for(String name : signatureNames){
			if(out.length() > 0){
				out.append(", ");
			}
			out.append(name);
		}
		return out.toString();
	}

	public final double getScore() {
		return score;
	}

	public final void addSignature(String name){
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

	public final Long getId(){
		return id;
	}

	@Override
	public final int compareTo(Process o) {
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

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof Process) {
			Process p = (Process) obj;
			return score == p.score;
		}
		return false;
	}

	@Override
	public final int hashCode() {
		long bits = Double.doubleToLongBits(score);
		return 31 * (int) (bits / 17);
	}
}

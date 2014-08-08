package pl.nask.hsn2.task;

import java.util.List;

public class Signature<T>{// extends SignatureData> {
	private List<String> families;
	private String description;
	private int severity;
	private List<String> references;
	private boolean alert;
	private List<T> data; 
	private String name;
	
	public List<String> getFamilies() {
		return families;
	}
	public void setFamilies(List<String> families) {
		this.families = families;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getSeverity() {
		return severity;
	}
	public void setSeverity(int severity) {
		this.severity = severity;
	}
	public List<String> getReferences() {
		return references;
	}
	public void setReferences(List<String> references) {
		this.references = references;
	}
	public boolean isAlert() {
		return alert;
	}
	public void setAlert(boolean alert) {
		this.alert = alert;
	}
	public List<T> getData() {
		return data;
	}
	public void setData(List<T> data) {
		this.data = data;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}

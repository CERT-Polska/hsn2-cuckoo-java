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

import java.util.List;

public class Signature<T>{// extends SignatureData> {
	private List<String> families;
	private String description;
	private int severity;
	private List<String> references;
	private boolean alert;
	private List<T> data; 
	private String name;
	
	public final List<String> getFamilies() {
		return families;
	}
	public final void setFamilies(List<String> families) {
		this.families = families;
	}
	public final String getDescription() {
		return description;
	}
	public final void setDescription(String description) {
		this.description = description;
	}
	public final int getSeverity() {
		return severity;
	}
	public final void setSeverity(int severity) {
		this.severity = severity;
	}
	public final List<String> getReferences() {
		return references;
	}
	public final void setReferences(List<String> references) {
		this.references = references;
	}
	public final boolean isAlert() {
		return alert;
	}
	public final void setAlert(boolean alert) {
		this.alert = alert;
	}
	public final List<T> getData() {
		return data;
	}
	public final void setData(List<T> data) {
		this.data = data;
	}
	public final String getName() {
		return name;
	}
	public final void setName(String name) {
		this.name = name;
	}
}

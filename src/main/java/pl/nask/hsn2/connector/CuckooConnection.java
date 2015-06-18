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
package pl.nask.hsn2.connector;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;

public class CuckooConnection implements Closeable{
	
	private String url;
	private GetMethod getMethod;
	private InputStream in;
	
	public CuckooConnection(String url) {
		this.url = url;
	}
	
	public InputStream getBodyAsInputStream(){
		return in;
	}
	
	@Override
	public void close() throws IOException {
		IOUtils.closeQuietly(in);
		getMethod.releaseConnection();
	}

	public void connect() throws CuckooException{
		try {
			getMethod = new GetMethod(url);
			HttpClient client = new HttpClient();
			int status = client.executeMethod(getMethod);
			if (status != HttpStatus.SC_OK) {
				throw new CuckooException("Unexpected response status: "+ status);
			}
			in = getMethod.getResponseBodyAsStream();
		} catch (IOException e) {
			throw new CuckooException(e.getMessage(), e);
		}
	}

	public String getBodyAsString() throws CuckooException {
		try {
			return getMethod.getResponseBodyAsString();
		} catch (IOException e) {
			throw new CuckooException(e.getMessage(), e);
		}
	}
	
	public int getResultStatusCode() {
		return getMethod.getStatusCode();
	}
}

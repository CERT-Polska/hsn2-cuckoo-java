package pl.nask.hsn2.connector;


public class CuckooException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5807571723171152171L;
	
	public CuckooException(String msg) {
		super(msg);
	}

	public CuckooException(String message, Exception e) {
		super(message, e);
	}

}

package com.rl.obf;

public class RGSException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 */
	public RGSException() {
	}

	/**
	 * Constructor
	 * 
	 * @param message
	 */
	public RGSException(final String message) {
		super(message);
	}

	public RGSException(final Throwable cause) {
		super(cause);
	}

	public RGSException(final String message, final Throwable cause) {
		super(message, cause);
	}
}

package com.rl.obf.classfile;

public class ClassFileException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 */
	public ClassFileException() {
	}

	/**
	 * Constructor
	 * 
	 * @param message
	 */
	public ClassFileException(final String message) {
		super(message);
	}

	public ClassFileException(final Throwable cause) {
		super(cause);
	}

	public ClassFileException(final String message, final Throwable cause) {
		super(message, cause);
	}
}

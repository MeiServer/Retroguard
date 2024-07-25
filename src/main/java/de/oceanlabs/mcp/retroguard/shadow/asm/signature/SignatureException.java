package de.oceanlabs.mcp.retroguard.shadow.asm.signature;

public class SignatureException extends Exception {
	private static final long serialVersionUID = 1L;

	public SignatureException() {
	}

	public SignatureException(final String message) {
		super(message);
	}

	public SignatureException(final Throwable cause) {
		super(cause);
	}

	public SignatureException(final String message, final Throwable cause) {
		super(message, cause);
	}
}

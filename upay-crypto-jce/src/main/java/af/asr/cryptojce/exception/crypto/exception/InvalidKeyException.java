/*
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
package af.asr.cryptojce.exception.crypto.exception;


import af.asr.cryptojce.exception.common.BaseUncheckedException;

/**
 * {@link Exception} to be thrown when key is invalid
 */
public class InvalidKeyException extends BaseUncheckedException {

	/**
	 * Unique id for serialization
	 */
	private static final long serialVersionUID = -3556229489431119187L;

	/**
	 * Constructor with errorCode and errorMessage
	 * 
	 * @param errorCode    The error code for this exception
	 * @param errorMessage The error message for this exception
	 */
	public InvalidKeyException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Constructor with errorCode, errorMessage, and rootCause
	 * 
	 * @param errorCode    The error code for this exception
	 * @param errorMessage The error message for this exception
	 * @param rootCause    Cause of this exception
	 */
	public InvalidKeyException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}
}
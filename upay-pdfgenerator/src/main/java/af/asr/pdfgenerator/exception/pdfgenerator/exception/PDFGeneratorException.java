package af.asr.pdfgenerator.exception.pdfgenerator.exception;

import af.asr.pdfgenerator.exception.common.BaseUncheckedException;

public class PDFGeneratorException extends BaseUncheckedException {

	private static final long serialVersionUID = -6138841548758442351L;

	/**
	 * Constructor for PDFGeneratorGenericException
	 * 
	 * @param errorCode    The errorcode
	 * @param errorMessage The errormessage
	 * @param cause        The cause
	 */
	public PDFGeneratorException(String errorCode, String errorMessage, Throwable cause) {
		super(errorCode, errorMessage, cause);
	}

	/**
	 * Constructor for PDFGeneratorGenericException
	 * 
	 * @param errorCode    The errorcode
	 * @param errorMessage The errormessage
	 */
	public PDFGeneratorException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}
}

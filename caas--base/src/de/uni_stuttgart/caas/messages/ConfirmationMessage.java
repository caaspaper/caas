package de.uni_stuttgart.caas.messages;

/**
 * Encapsulates a confirmation message that contains a status code, which
 * indicates whether another operation was successful or not.
 */
public class ConfirmationMessage implements IMessage {

	/**
	 * Status code with semantics that 0 means success and non-0 indicates
	 * failure (the exact interpretation of which is up to the user)
	 */
	public final int STATUS_CODE;

	/**
	 * Optional message that further describes the status. This value may be
	 * null.
	 */
	public final String MESSAGE;

	
	public ConfirmationMessage(int statusCode, String message) {
		STATUS_CODE = statusCode;
		MESSAGE = message;
	}
	

	@Override
	public MessageType getMessageType() {
		return MessageType.CONFIRM;
	}
}




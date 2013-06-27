package de.uni_stuttgart.caas.messages;

/**
 * @author Alexander C. Gessler, Ashley M. Smith, Simon Hanna
 *
 */
public class ActivateNodeMessage implements IMessage {

	/**
	 * @see de.uni_stuttgart.caas.admin.messages.IMessage#GetMessage()
	 */
	@Override
	public MessageType GetMessage() {
		
		return MessageType.ACTIVATE;
	}
}

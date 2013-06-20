package de.uni_stuttgart.caas.admin.messages;

public class JoinMessage implements IMessage {

	/**
	 * @see de.uni_stuttgart.caas.admin.messages.IMessage#GetMessage()
	 */
	@Override
	public MessageType GetMessage() {
		return MessageType.JOIN;
	}

}

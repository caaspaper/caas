package de.uni_stuttgart.caas.messages;

public class JoinMessage implements IMessage {

	/**
	 * @see de.uni_stuttgart.caas.admin.messages.IMessage#getMessageType()
	 */
	@Override
	public MessageType getMessageType() {
		return MessageType.JOIN;
	}

}

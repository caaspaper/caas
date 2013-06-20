package de.uni_stuttgart.caas.admin.messages;

/**
 * 
 */
public class JoinMessage implements IMessage {

	public JoinMessage() {
	}

	@Override
	public MessageType GetMessage() {
		return MessageType.JOIN;
	}

}

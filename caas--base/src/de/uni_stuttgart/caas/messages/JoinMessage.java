package de.uni_stuttgart.caas.messages;

import java.net.InetSocketAddress;

public class JoinMessage implements IMessage {

	/**
	 * @see de.uni_stuttgart.caas.admin.messages.IMessage#getMessageType()
	 */
	
	/**
	 * 
	 */
	public final InetSocketAddress ADDRESS_FOR_CACHENODE_NODECONNECTOR;
	
	
	public JoinMessage(InetSocketAddress addr) {
		ADDRESS_FOR_CACHENODE_NODECONNECTOR = addr;
	}
	
	
	@Override
	public MessageType getMessageType() {
		return MessageType.JOIN;
	}

}

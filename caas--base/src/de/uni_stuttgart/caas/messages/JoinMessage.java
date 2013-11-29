package de.uni_stuttgart.caas.messages;

import java.net.InetSocketAddress;

public class JoinMessage implements IMessage {

	/**
	 * @see de.uni_stuttgart.caas.admin.messages.IMessage#getMessageType()
	 */
	
	/**
	 * 
	 */
	public final InetSocketAddress ADDRESS_FOR_CACHENODE_NEIGHBORCONNECTOR;
	
	public final InetSocketAddress ADDRESS_FOR_CACHENODE_QUERYLISTENER;
	
	
	public JoinMessage(InetSocketAddress addrNeighborConnector, InetSocketAddress addrQueryListener) {
		ADDRESS_FOR_CACHENODE_NEIGHBORCONNECTOR = addrNeighborConnector;
		ADDRESS_FOR_CACHENODE_QUERYLISTENER = addrQueryListener;
	}
	
	
	@Override
	public MessageType getMessageType() {
		return MessageType.JOIN;
	}

}

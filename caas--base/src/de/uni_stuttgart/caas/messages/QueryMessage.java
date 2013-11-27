package de.uni_stuttgart.caas.messages;

import java.net.SocketAddress;

import de.uni_stuttgart.caas.base.LocationOfNode;

/**
 * Query messages carry the actual query that was sent to the cache service.
 * The entry node creates it, so that the entire system can rely on this class.
 * 
 */
public class QueryMessage implements IMessage {

	/**
	 * The coordinates for the query
	 */
	public final LocationOfNode QUERY_LOCATION;
	
	/**
	 * IP address of the client
	 */
	public final String CLIENT_IP;
	
	/**
	 * Port the client is listening on
	 */
	public final int CLIENT_PORT;
	
	/**
	 * If the load of the node that is supposed to process the message is to high
	 * it forwards it to any other neighbor forcing it to process it
	 */
	private boolean allowPropagationThroughNetwork = true;
	
	
	/**
	 * For debugging
	 */
	private String debuggingInfo = "";
	
	
	public QueryMessage(LocationOfNode l, String ip, int port) {
		QUERY_LOCATION = l;
		CLIENT_IP = ip;
		CLIENT_PORT = port;
	}
	
	@Override
	public MessageType getMessageType() {
		return MessageType.QUERY_MESSAGE;
	}
	
	public void stopPropagationOfTheMessage() {
		allowPropagationThroughNetwork = false;
	}
	
	public boolean isPropagtionThroughNetworkAllowed() {
		return allowPropagationThroughNetwork;
	}
	
	public void appendToDebuggingInfo(String s) {
		debuggingInfo += s + "\n";
	}
	
	public String getDebuggingInfo() {
		return debuggingInfo;
	}

}

package de.uni_stuttgart.caas.messages;

import java.net.InetSocketAddress;

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
	 * The address of the node that initially processed the query
	 * this node will have to send the response back
	 */
	public final InetSocketAddress ENTRY_NODE;
	
	/**
	 * The id is for matching a query to a response, 
	 * this id has to be unique for a single cacheNode
	 */
	public final int ID;
	
	/**
	 * If the load of the node that is supposed to process the message is to high
	 * it forwards it to any other neighbor forcing it to process it
	 */
	private boolean allowPropagationThroughNetwork = true;
	
	
	public QueryMessage(LocationOfNode l, InetSocketAddress node, int id) {
		QUERY_LOCATION = l;
		ENTRY_NODE = node;
		ID = id;
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

}

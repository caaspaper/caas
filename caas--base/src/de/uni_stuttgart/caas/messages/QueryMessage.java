package de.uni_stuttgart.caas.messages;

/**
 * Query messages carry the actual query that was sent to the cache service.
 * The entry node creates it, so that the entire system can rely on this class.
 * 
 */
public class QueryMessage implements IMessage {

	@Override
	public MessageType getMessageType() {
		// TODO Auto-generated method stub
		return MessageType.QUERY_MESSAGE;
	}

}

package de.uni_stuttgart.caas.messages;

public class QueryResult implements IMessage {

	
	
	private String debuggingInfo;
	
	public final long ID;
	
	
	public QueryResult(String debuggingInfo, long id) {
		this.debuggingInfo = debuggingInfo;
		ID = id;
	}
	
	@Override
	public MessageType getMessageType() {
		return MessageType.QUERY_RESULT;
	}
	
	public void appendToDebbugingInfo(String s) {
		debuggingInfo += s;
	}
	
	public String getDebuggingInfo() {
		return debuggingInfo;
	}

}

package de.uni_stuttgart.caas.messages;

public class QueryResult implements IMessage {

	
	
	private String debuggingInfo;
	
	
	public QueryResult(String debuggingInfo) {
		this.debuggingInfo = debuggingInfo;
	}
	
	@Override
	public MessageType getMessageType() {
		return MessageType.QUERY_RESULT;
	}
	
	public void appendToDebbugingInfo(String s) {
		debuggingInfo += s + "\n";
	}
	
	public String getDebuggingInfo() {
		return debuggingInfo;
	}

}

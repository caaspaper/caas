package de.uni_stuttgart.caas.messages;

public class QueryResult implements IMessage {

	
	public final int ID;
	
	private String debuggingInfo;
	
	
	public QueryResult(int ID, String debuggingInfo) {
		this.ID = ID;
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

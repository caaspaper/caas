package de.uni_stuttgart.caas.messages;

public class LoadMessage implements IMessage{

	
	public LoadMessage(double load) {
		LOAD = load;
	}
	
	@Override
	public MessageType getMessageType() {
		return MessageType.LOAD_MESSAGE;
	}
	
	public final double LOAD;

}

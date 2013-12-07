package de.uni_stuttgart.caas.messages;


public class SubdivisionConfirmMessage implements IMessage {

	public final long V0, V1, V2;

	public SubdivisionConfirmMessage(long v0, long v1, long v2) {
		V0 = v0;
		V1 = v1;
		V2 = v2;
	}


	@Override
	public MessageType getMessageType() {
		return MessageType.SUBDIV_CONFIRM;
	}
}

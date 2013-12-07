package de.uni_stuttgart.caas.messages;

public class SubdivisionRequestMessage implements IMessage {

	public final long ORIGIN_VERTEX;
	public final long FIRST_VERTEX;

	public double accumLoad;
	public int ttl;

	public SubdivisionRequestMessage(long origin, long firstVertex) {
		ttl = 3;
		accumLoad = 0.0;
		ORIGIN_VERTEX = origin;
		FIRST_VERTEX = firstVertex;
	}

	public boolean AddTriangleVertexLoad(double d) {
		--ttl;
		accumLoad += d;
		return ttl == 0;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.SUBDIV_REQUEST;
	}
}

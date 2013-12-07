package de.uni_stuttgart.caas.messages;

import de.uni_stuttgart.caas.base.NodeInfo;


public class SubdivisionCommitMessage implements IMessage {
	
	public final long NEW_NODE_ID;
	public final NodeInfo NEW_NODE_INFO;
	
	public SubdivisionCommitMessage(long newNodeId, NodeInfo newNodeInfo) {
		NEW_NODE_ID = newNodeId;
		NEW_NODE_INFO = newNodeInfo;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.SUBDIV_COMMIT;
	}
}

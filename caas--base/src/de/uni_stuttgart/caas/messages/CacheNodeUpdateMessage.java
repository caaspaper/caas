package de.uni_stuttgart.caas.messages;

import de.uni_stuttgart.caas.base.LocationOfNode;
import de.uni_stuttgart.caas.base.NodeInfo;

public class CacheNodeUpdateMessage implements IMessage{

	
	public CacheNodeUpdateMessage(NodeInfo newInfo, NodeInfo oldInfo) {
		this.newInfo = newInfo;
		this.oldInfo = oldInfo;
	}
	
	@Override
	public MessageType getMessageType() {
		// TODO Auto-generated method stub
		return MessageType.CACHENODE_UPDATE_MESSAGE;
	}
	
	
	public final NodeInfo newInfo;
	public final NodeInfo oldInfo;

}

package de.uni_stuttgart.caas.messages;

/** Sent by nodes to their neighbors to identify themselves */
public class PublishIdMessage implements IMessage {

	/**
	 * Unique (lifetime of cache node) ID of the node within the network
	 */
	public final long ID;

	public PublishIdMessage(long id) {
		this.ID = id;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.PUBLISH_ID;
	}

}

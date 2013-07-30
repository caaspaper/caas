///////////////////////////////////////////////////////////////////////////////////
// Cache as a Service (projekt-inf) (v0.1)
// [JoinMessage.java]
// (c) 2013 Ashley Marie Smith, Simon Hanna, Alexander Gessler
//
// All rights reserved.
//
// This code may not be published, distributed or otherwise made available to
// third parties without the prior written consent of the copyright owners.
//
///////////////////////////////////////////////////////////////////////////////////

package de.uni_stuttgart.caas.messages;

public class JoinMessage implements IMessage {

	/**
	 * @see de.uni_stuttgart.caas.admin.messages.IMessage#getMessageType()
	 */
	@Override
	public MessageType getMessageType() {
		return MessageType.JOIN;
	}

}

/* vi: set shiftwidth=4 tabstop=4: */ 
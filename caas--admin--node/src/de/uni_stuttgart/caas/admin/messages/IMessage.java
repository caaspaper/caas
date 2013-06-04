package de.uni_stuttgart.caas.admin.messages;

public interface IMessage {

	enum MessageType {
		JOIN, ADD_TO_GRID, ACTIVATE, CONFIRM
	}

	/** */
	MessageType GetMessage();

}

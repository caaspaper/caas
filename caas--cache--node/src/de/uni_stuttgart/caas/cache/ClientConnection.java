package de.uni_stuttgart.caas.cache;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import de.uni_stuttgart.caas.messages.QueryMessage;
import de.uni_stuttgart.caas.messages.QueryResult;

/**
 * Represents a currently opened client connection used by cache nodes to send
 * back query responses
 */
class ClientConnection {

	public ClientConnection(String host, int port, final boolean singleUse) throws IOException {
		client = new Socket(host, port);
		clientOut = new ObjectOutputStream(client.getOutputStream());

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// TODO: handle closing and error scenarios
					do {
						final QueryMessage message = messages.take();
						clientOut.writeObject(new QueryResult(message.getDebuggingInfo(), message.ID));
						clientOut.flush();
					} while (!singleUse);
				} catch (Exception e) {
					e.printStackTrace();
				}

				try {
					clientOut.close();
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void enqueueResponse(final QueryMessage message) {
		messages.add(message);
	}

	private Socket client;
	private ObjectOutputStream clientOut;
	private LinkedBlockingQueue<QueryMessage> messages = new LinkedBlockingQueue<>();
};

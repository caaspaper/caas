package de.uni_stuttgart.caas.base;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import de.uni_stuttgart.caas.messages.IMessage;

/**
 * Full-duplex Message-Passing-Interface (MPI) implementation based on IMessage.
 * 
 * An instance of the class represents one end point of the connection between
 * two network nodes (i.e. the other party also uses a FullDuplexMPI instance).
 * 
 * The idea is that both parties concurrently send and receive messages. Message
 * processing is inherently asynchronous, the sender of a message gets the
 * response to their message at some unspecified later point in time. Messages
 * are always responded to, even if the response is just a dummy. This enables
 * both parties to know whether the other party has processed their message.
 * 
 * FullDuplexMPI uses TCP internally. The strong guarantees made by TCP with
 * regard to data consistency are kept, therefore, loss of data is practically
 * guaranteed not to happen. The only error scenario therefore is loss of
 * connection, which currently is undefined behaviour.
 * 
 * The implementation is based on a reader and a writer thread, yet it is
 * lockless, and should therefore scale well.
 * 
 */
public abstract class FullDuplexMPI {

	// ---------------------------------
	// Public API
	// ---------------------------------

	/**
	 * A callback closure who receives response to asynchronously send messages.
	 * 
	 * See sendMessageAsync()
	 * 
	 * An IResponseHandler instance may be only used once.
	 */
	public interface IResponseHandler {

		/**
		 * Called upon reception of the response to the message sent.
		 * 
		 * @note The implementation needs to be threadsafe with respect to the
		 *       rest of the program (i.e. it must expect to be called from
		 *       different threads), but it need not be reentrant (i.e. it need
		 *       not expect to be called from two threads at the same time).
		 * 
		 * @note The implementation may not interrupt() the calling thread.
		 * 
		 * @param response
		 *            non-null response to the message
		 */
		void onResponseReceived(IMessage response);
	}

	/**
	 * Establish a full-duplex MPI connection given a socket connecting the
	 * endpoints.
	 * 
	 * @param socket
	 *            non-null Socket instance representing a connected socket.
	 * @throws IOException
	 *             upon failure to setup the full duplex connection.
	 */
	public FullDuplexMPI(Socket socket) throws IOException {
		assert socket != null;
		clientSocket = socket;

		writeQueue = new LinkedBlockingQueue<OutgoingMessage>();
		pendingSentMessages = new ConcurrentHashMap<Integer, OutgoingMessage>();

		// Start both reader and writer threads, forward any exceptions to the
		// caller. The writer thread needs to be started first: creating object
		// streams (which is done in the reader/writer thread constructors)
		// already involves network communication. Therefore, creating readers
		// first would cause deadlocking.

		try {
			(writer = new Thread(new WriterThread())).start();
		} catch (IOException e) {
			throw new IOException("FullDuplexMPI: failed to start writer thread", e);
		}

		try {
			(reader = new Thread(new ReaderThread())).start();
		} catch (IOException e) {
			throw new IOException("FullDuplexMPI: failed to start reader thread", e);
		}
	}

	/**
	 * Same as sendMessageAsync(IMessage message, IResponseHandler
	 * futureResponse), except that it discards the response to the message.
	 * 
	 * Messages _always_ are responded to, yet the sender may opt not to look at
	 * the response. This is not recommended as errors may silently slip
	 * through.
	 * 
	 * @note The method is threadsafe.
	 * 
	 * @param message
	 */
	public void sendMessageAsync(IMessage message) {
		sendMessageAsync(message, null);
	}

	/**
	 * Asynchronously send message, passing a closure to receive the future
	 * response.
	 * 
	 * Usage:
	 * 
	 * <pre>
	 * {@code
	 * 
	 * sendMessageAsync(myMessage, new IResponseHandler() {
	 *    @Override
	 *    void onResponseReceived(IMessage myResponse) {
	 *    		// handle response
	 *    }
	 * });
	 * 
	 * }
	 * </pre>
	 * 
	 * TODO: support timeouts?
	 * 
	 * @note The method is threadsafe.
	 * 
	 * @param message
	 *            non-null message object to be passed
	 * @param futureResponse
	 *            Closure that receives a callback upon receiving the message
	 *            response. May be null, in which case the response is thrown
	 *            away (but the MPI interface nevertheless expects a response).
	 */
	public void sendMessageAsync(IMessage message, IResponseHandler futureResponse) {
		assert message != null;

		try {
			writeQueue.put(new OutgoingMessage(message, futureResponse));
		} catch (InterruptedException e) {
			// unreachable - as the thread is encapsulated, there is nobody
			// who could call interrupt() on it.
			e.printStackTrace();
			assert false;
		}
	}

	/**
	 * Called upon reception of a new incoming message that is not a response to
	 * a previously sent message.
	 * 
	 * The implementation is supposed to adequately handle the message, and
	 * generate a response message to be send back to the other party.
	 * 
	 * @note The implementation needs to be threadsafe with respect to the rest
	 *       of the program (i.e. it must expect to be called from different
	 *       threads), but it need not be reentrant (i.e. it need not expect to
	 *       be called from two threads at the same time).
	 * 
	 * @note The implementation may not interrupt() the calling thread.
	 * 
	 * @param message
	 *            Non-null message object representing the incoming message
	 * @return A non-null IMessage object representing the response to the
	 *         message. Messages with no response are not allowed.
	 */
	public abstract IMessage processIncomingMessage(IMessage message);

	// ---------------------------------
	// Implementation
	// ---------------------------------

	private static class OutgoingMessage {
		private static final AtomicInteger uidCounter = new AtomicInteger(0);

		public OutgoingMessage(IMessage _message, IResponseHandler _handler) {
			this(_message, _handler, uidCounter.incrementAndGet(), true);
		}

		public OutgoingMessage(IMessage _message, IResponseHandler _handler, int _uid, boolean _expectResponse) {
			assert _message != null;
			assert _handler != null;
			assert _expectResponse || _handler == null;

			message = _message;
			handler = _handler;
			uid = _uid;
			expectResponse = _expectResponse;
		}

		public final IMessage message;
		public final int uid;

		public final boolean expectResponse;
		public final IResponseHandler handler;
	}

	private static class MessageEnvelope implements Serializable {

		private static final long serialVersionUID = 5916422368802538995L;

		public MessageEnvelope(IMessage _message, int _uid) {
			assert _message != null;

			message = _message;
			uid = _uid;
		}

		public final IMessage message;
		public final int uid;

	}

	private final Thread reader, writer;

	private final BlockingQueue<OutgoingMessage> writeQueue;
	private final ConcurrentHashMap<Integer, OutgoingMessage> pendingSentMessages;
	private final Socket clientSocket;

	private class ReaderThread implements Runnable {

		private final ObjectInputStream in;

		public ReaderThread() throws IOException {
			in = new ObjectInputStream(clientSocket.getInputStream());
		}

		@Override
		public void run() {
			assert in != null;

			while (true) {
				final MessageEnvelope envelope = readMessageEnvelope();
				final OutgoingMessage message = pendingSentMessages.get((Integer) envelope.uid);
				if (message == null) {
					// there is no entry for this message, so it is not a
					// response to a previous message
					final IMessage response = processIncomingMessage(envelope.message);
					assert response != null;

					writeResponseAsync(response, envelope.uid);
					continue;
				}

				// there exists an entry
				assert message.expectResponse;
				if (message.handler != null) {
					message.handler.onResponseReceived(envelope.message);
				}

				pendingSentMessages.remove((Integer) envelope.uid);
			}
		}

		private void writeResponseAsync(IMessage response, int uid) {
			assert response != null;
			try {
				writeQueue.put(new OutgoingMessage(response, null, uid, false));
			} catch (InterruptedException e) {
				// unreachable - as the thread is encapsulated, there is nobody
				// who could call interrupt() on it.
				e.printStackTrace();
				assert false;
			}
		}

		private MessageEnvelope readMessageEnvelope() {
			try { // message, i.e. object, passed over connection, wrapped in
					// our envelope
				return (MessageEnvelope) in.readObject(); //

			} catch (ClassNotFoundException e) {
				System.out.println("Object class not found");
				e.printStackTrace();
			} catch (ClassCastException e) {
				System.out.println("Cast of obj to type IMessage failed");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Read failed");
				e.printStackTrace();
			}
			// TODO: error handling
			return null;
		}
	}

	private class WriterThread implements Runnable {

		private final ObjectOutputStream out;

		public WriterThread() throws IOException {
			out = new ObjectOutputStream(clientSocket.getOutputStream());
		}

		@Override
		public void run() {
			assert out != null;

			while (true) {
				try {
					final OutgoingMessage msg = writeQueue.take();
					assert msg != null;

					// for proper lockless operation, it is crucial that we
					// first add the entry to the map, then send the message
					// to the other party. otherwise we could receive the
					// response before the entry is in the map, causing the
					// response to be lost.
					if (msg.expectResponse) {
						pendingSentMessages.put(msg.uid, msg);
					}

					writeMessageEnvelope(new MessageEnvelope(msg.message, msg.uid));

				} catch (InterruptedException e) {
					// unreachable - as the thread is encapsulated, there is
					// nobody who could call interrupt() on it.
					e.printStackTrace();
					assert false;
				}
			}
		}

		void writeMessageEnvelope(MessageEnvelope envelope) {
			assert out != null;
			assert envelope != null;
			try {
				out.writeObject(envelope);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

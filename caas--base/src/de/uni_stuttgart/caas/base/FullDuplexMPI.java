package de.uni_stuttgart.caas.base;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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
public abstract class FullDuplexMPI /* implements AutoCloseable */{

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
		 * Called once upon reception of the response to the message sent.
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

		/**
		 * Called if the connection is aborted and therefore no further response
		 * is to be expected.
		 * 
		 * This happens iff either close() is called on the connection, or a
		 * network error occurs (i.e. the other party closes the connection), in
		 * which case the current connection end point remains in an error state
		 * (see isErrorState).
		 * */
		void onConnectionAborted();
	}

	/**
	 * Establish a full-duplex MPI connection given a socket connecting the
	 * endpoints.
	 * 
	 * @param socket
	 *            non-null Socket instance representing a connected socket. The
	 *            created object takes ownership of the socket.
	 * @param errorOutStream
	 *            non-null PrintStream instance that receives any error messages
	 *            or exception stack traces that occur during operation of the
	 *            FullDuplexMPI instance.
	 * @param autoStart
	 *            Specifies whether the connection starts processing commands
	 *            immediately, which requires that the implementation needs to
	 *            prepared to receive processIncomingMessage() calls before or
	 *            while their own constructor code executes. If this parameter
	 *            is set to false, the implementation must call start() to begin
	 *            processing messages.
	 * @throws IOException
	 *             upon failure to setup the full duplex connection.
	 */
	protected FullDuplexMPI(Socket socket, PrintStream errorOutStream, boolean autoStart) throws IOException {
		assert socket != null;
		assert errorOutStream != null;

		clientSocket = socket;
		outStream = errorOutStream;

		writeQueue = new LinkedBlockingQueue<OutgoingMessage>();
		pendingSentMessages = new ConcurrentHashMap<Integer, OutgoingMessage>();

		if (autoStart) {
			start();
		}
	}

	/**
	 * Starts processing messages.
	 * 
	 * No callbacks on processIncomingMessage() are received before this is
	 * called.
	 * 
	 * The method may only be called once, and it must be called before any
	 * other method on the FullDuplexMPI object is invoked.
	 * 
	 * This is invoked automatically by the constructor unless the `autoStart`
	 * parameter is set to false.
	 * 
	 * @throws IOException
	 *             upon failure to setup the full duplex connection.
	 */
	protected void start() throws IOException {
		assert writer == null && reader == null;

		// Start both reader and writer threads, forward any exceptions to the
		// caller. The writer thread needs to be started first: creating object
		// streams (which is done in the reader/writer thread constructors)
		// already involves network communication. Therefore, creating readers
		// first would cause deadlocking.

		try {
			(writer = new Thread(new WriterThread())).start();
		} catch (IOException e) {
			throw new IOException("(FullDuplexMPI) failed to start writer thread", e);
		}

		try {
			(reader = new Thread(new ReaderThread())).start();
		} catch (IOException e) {
			throw new IOException("(FullDuplexMPI) failed to start reader thread", e);
		}
	}

	/**
	 * Check if the connection is in an irrecoverable error state.
	 * 
	 * This occurs if a network error occurs, or if the other party abandons the
	 * connection.
	 * 
	 * @return
	 */
	public boolean isErrorState() {
		return errorState.get();
	}

	/**
	 * Invoked when an error state is reached. Does not necessarily need to be
	 * overridden by implementors, but doing so is highly recommended.
	 * 
	 * onReachErrorState() is called at most once in the lifetime of a
	 * FullDuplexMPI instance.
	 */
	public void onReachErrorState() {
		assert isErrorState();
	}

	@Override
	public String toString() {
		return "{FullDuplexMPI, socket=" + clientSocket.toString() + "}";
	}

	/**
	 * Close the connection and relinquish all resources to the operating
	 * system. This includes closing the socket.
	 * 
	 * All pending response handlers receive onConnectionAborted().
	 * 
	 * Calling close() on an already-closed instance is a no-op.
	 */
	public void close() {
		if (isShuttingDown) {
			return;
		}

		isShuttingDown = true;
		reader.interrupt();
		writer.interrupt();

		try {
			clientSocket.close();
		} catch (IOException e) {
			outStream.println("(FullDuplexMPI) Exception during socket shutdown, ignoring");
			e.printStackTrace(outStream);
		}

		// free up references on the thread to make the object streams contained
		// therein accessible to garbage collection.
		reader = writer = null;
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
	 * @note If you expect to be interrupted, call isInterrupted() on the thread
	 *       afterwards as interrupts are swallowed by this method.
	 */
	public void sendMessageAsync(IMessage message, IResponseHandler futureResponse) {
		assert message != null;
		if (errorState.get()) {
			if (futureResponse != null) {
				futureResponse.onConnectionAborted();
			}
			return;
		}
		while (true) {
			try {
				writeQueue.put(new OutgoingMessage(message, futureResponse));
				break;
			} catch (InterruptedException e) {
				// put() internally does not wait for long, so nobody will
				// intentionally interrupt() on the calling thread during
				// _this_ time. Therefore, any code expecting to receive
				// interrupts in general will not rely on us bubbling the
				// exception up, so we can swallow it.

				// A special case is, however, if sendMessageAsync() is called
				// from IResponseHandler or from within
				// processIncomingMessage() implementation.
				//
				// In such a case, an interrupt() received here could be an
				// interrupt() send to our reader and writer threads as a
				// signal to shutdown. We do not want to lose this interrupt.

				// Therefore, calls to user code have to check afterwards if
				// threads were interrupted to rule out this case!
			}
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

	public InetAddress getLocalAddress() {
		return clientSocket.getLocalAddress();
	}

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
			// assert _handler != null;
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

	private Thread reader, writer;

	private final BlockingQueue<OutgoingMessage> writeQueue;
	private final ConcurrentHashMap<Integer, OutgoingMessage> pendingSentMessages;

	private final Socket clientSocket;

	/**
	 * Injected PrintStream to receive any error messages that would otherwise
	 * go to System.(out|err).println
	 */
	private final PrintStream outStream;

	// note: we need volatile here as the value is written from one thread, but
	// concurrently read from others. Without volatile, the thread receiving
	// the signal to shutdown may not see updates made to this variable by
	// the thread who triggered the shutdown.
	private volatile boolean isShuttingDown = false;
	private final AtomicBoolean errorState = new AtomicBoolean(false);

	private void enterErrorState() {
		if (errorState.compareAndSet(false, true)) {
			onReachErrorState();
		}

		close();
	}

	private class ReaderThread implements Runnable {

		private final ObjectInputStream in;

		public ReaderThread() throws IOException {
			in = new ObjectInputStream(clientSocket.getInputStream());
		}

		@Override
		public void run() {
			assert in != null;

			while (!isShuttingDown) {
				try {
					MessageEnvelope envelope;

					try {
						envelope = readMessageEnvelope();
					} catch (IOException e) {
						// shutdown with error flag set
						enterErrorState();
						break;
					}

					final OutgoingMessage message = pendingSentMessages.get((Integer) envelope.uid);
					if (message == null) {
						// there is no entry for this message, so it is not a
						// response to a previous message
						final IMessage response = processIncomingMessage(envelope.message);
						if (Thread.currentThread().isInterrupted()) {
							// see note in sendMessageAsync()
							throw new InterruptedException();
						}
						assert response != null;

						writeResponseAsync(response, envelope.uid);
						continue;
					}

					// must remove the entry first
					pendingSentMessages.remove((Integer) envelope.uid);

					// there exists an entry
					assert message.expectResponse;
					if (message.handler != null && !isShuttingDown) {
						message.handler.onResponseReceived(envelope.message);
						if (Thread.currentThread().isInterrupted()) {
							// see note in sendMessageAsync()
							throw new InterruptedException();
						}
					}

				} catch (InterruptedException e) {
					// interrupt() should only happen during shutdown
					assert isShuttingDown;
				}
			}

			// abort all pending messages
			for (Map.Entry<Integer, OutgoingMessage> entry : pendingSentMessages.entrySet()) {
				if (entry.getValue().handler != null) {
					entry.getValue().handler.onConnectionAborted();
				}
			}
			pendingSentMessages.clear();
		}

		private void writeResponseAsync(IMessage response, int uid) throws InterruptedException {
			assert response != null;
			writeQueue.put(new OutgoingMessage(response, null, uid, false));
		}

		private MessageEnvelope readMessageEnvelope() throws InterruptedException, IOException {
			try { // message, i.e. object, passed over connection, wrapped in
					// our envelope
				return (MessageEnvelope) in.readObject(); //
			}

			catch (InterruptedIOException e) {
				// unlike IOException, InterruptedIOException is expected during
				// shutdown of the connection.
				assert isShuttingDown;
				throw new InterruptedException();

				// these two happen if the other party is not a FullDuplexMPI
				// instance of the same version. This is a case that we do not
				// expect to happen in normal use.
			} catch (ClassNotFoundException e) {
				outStream.println("(FullDuplexMPI) Protocol: class not known to VM, expected MessageEnvelope");
				outStream.println(e.getMessage());
				e.printStackTrace(outStream);

			} catch (ClassCastException e) {
				outStream.println("(FullDuplexMPI) Protocol: wrong class type, expected MessageEnvelope");
				outStream.println(e.getMessage());
				e.printStackTrace(outStream);

			} catch (IOException e) {
				// connection loss or otherwise fatal failure. This happens
				// whenever either party aborts the connection.
				outStream.println("(FullDuplexMPI) Failure reading object, connection lost or other network failure");
				outStream.println(e.getMessage());
				e.printStackTrace(outStream);
				throw e;
			}
			assert false;
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

			while (!isShuttingDown) {
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
					// interrupt() should only happen during shutdown
					assert isShuttingDown;
				}
			}

			// abort all not yet sent messages
			for (OutgoingMessage entry : writeQueue) {
				if (entry.handler != null) {
					entry.handler.onConnectionAborted();
				}
			}
			writeQueue.clear();
		}

		void writeMessageEnvelope(MessageEnvelope envelope) {
			assert out != null;
			assert envelope != null;
			try {
				out.writeObject(envelope);

			} catch (IOException e) {
				// connection loss or otherwise fatal failure, as per se we
				// do not handle this further
				outStream.println("(FullDuplexMPI) Failure writing object, connection lost or other network failure");
				outStream.println(e.getMessage());
				e.printStackTrace(outStream);

				// shutdown with error flag set
				enterErrorState();
			}
		}
	}
}

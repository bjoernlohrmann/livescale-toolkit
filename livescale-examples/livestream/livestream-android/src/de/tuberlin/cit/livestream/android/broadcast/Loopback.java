package de.tuberlin.cit.livestream.android.broadcast;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

/**
 * Creates a server socket on the local loopback device.
 * 
 * @author bjoern
 */
public class Loopback {
	private static final String TAG = Loopback.class.getSimpleName();

	// Local data loopback
	private LocalSocket receiverSocket, senderSocket;

	private LocalServerSocket serverSocket;

	private final String localAddress;

	/**
	 * Affects the sizes of the receiver socket's send buffer and the sender
	 * socket's receive buffer. Usually you will want to keep these quite low as
	 * these buffer will not be used.
	 */
	private final int receiverToSenderBufferSize;

	/**
	 * Affects the sizes of the sender socket's send buffer and the receiver
	 * socket's receive buffer. The size of these buffers depends on your
	 * application.
	 */
	private final int senderToReceiverBufferSize;

	public Loopback(String localAddress, int bufferSize) {
		this.localAddress = localAddress;
		this.senderToReceiverBufferSize = bufferSize;
		this.receiverToSenderBufferSize = bufferSize;
	}

	public Loopback(String localAddress, int senderToReceiverBufferSize,
			int receiverToSenderBufferSize) {
		this.localAddress = localAddress;
		this.senderToReceiverBufferSize = senderToReceiverBufferSize;
		this.receiverToSenderBufferSize = receiverToSenderBufferSize;
	}

	public boolean initLoopback() {
		releaseLoopback();

		try {
			serverSocket = new LocalServerSocket(localAddress);

			receiverSocket = new LocalSocket();
			receiverSocket.connect(new LocalSocketAddress(localAddress));
			receiverSocket.setReceiveBufferSize(senderToReceiverBufferSize);
			receiverSocket.setSendBufferSize(receiverToSenderBufferSize);

			senderSocket = serverSocket.accept(); // blocks until a new
													// connection arrives!
			senderSocket.setReceiveBufferSize(receiverToSenderBufferSize);
			senderSocket.setSendBufferSize(senderToReceiverBufferSize);
			return true;
		} catch (IOException e) {
			Log.d(TAG, "Couldn't create local interthread connection", e);
			return false;
		}
	}

	public void releaseLoopback() {
		try {
			if (serverSocket != null) {
				serverSocket.close();
			}
			if (receiverSocket != null) {
				receiverSocket.close();
			}
			if (senderSocket != null) {
				senderSocket.close();
			}
		} catch (IOException e) {
			Log.e(TAG, "Error closing sockets", e);
		}

		serverSocket = null;
		senderSocket = null;
		receiverSocket = null;
	}

	public FileDescriptor getSenderFileDescriptor() {
		if (senderSocket == null) {
			throw new IllegalStateException(
					"Cannot return file descriptor as sender is unitialized (Call initLoopback first)");
		}
		return senderSocket.getFileDescriptor();
	}

	public FileDescriptor getReceiverFileDescriptor() {
		if (receiverSocket == null) {
			throw new IllegalStateException(
					"Cannot return file descriptor as receiver is unitialized (Call initLoopback first)");
		}
		return receiverSocket.getFileDescriptor();

	}

	public InputStream getReceiverInputStream() throws IOException {
		return receiverSocket.getInputStream();
	}

	public OutputStream getSenderOutputStream() throws IOException {
		return senderSocket.getOutputStream();
	}
}

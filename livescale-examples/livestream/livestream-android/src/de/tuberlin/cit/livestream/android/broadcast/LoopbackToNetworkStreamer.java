package de.tuberlin.cit.livestream.android.broadcast;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Socket;

import android.util.Log;
import de.tuberlin.cit.livestream.android.broadcast.flv.FLVPackagingThread;
import de.tuberlin.cit.livestream.android.broadcast.mp4.MP4UnpackagingThread;

/**
 * Class that reads an MP4 container from an input stream and converts it to FLV
 * which is then written to an output stream. This repackaging process is done
 * using two concurrent threads that communicate via a VideoPackageRing.
 * 
 * @author Bjoern Lohrmann
 */
public class LoopbackToNetworkStreamer {

	private static final String TAG = LoopbackToNetworkStreamer.class
			.getSimpleName();

	private Loopback loopback;

	private String targetHost;

	private int targetPort;

	private Socket socket;

	private MP4UnpackagingThread mp4UnpackagerThread;

	private FLVPackagingThread flvPackagingThread;

	private VideoPackageRing videoPackageRing;

	private BroadcastStatusListener failureListener;

	public LoopbackToNetworkStreamer(String targetHost, int targetPort,
			BroadcastStatusListener failureListener) {

		this.targetHost = targetHost;
		this.targetPort = targetPort;
		this.failureListener = failureListener;
	}

	public void prepareStreaming() throws IOException {
		Log.i(TAG, String.format("Connecting to %s:%d", targetHost, targetPort));

		this.socket = new Socket(BCConstants.STREAM_SERVER_SEND_HOST,
				BCConstants.STREAM_SERVER_SEND_PORT);
		this.socket.setTcpNoDelay(true);

		this.videoPackageRing = new VideoPackageRing(16, 96 * 1024);

		this.loopback = new Loopback("foobar", 4096);
		this.loopback.initLoopback();

		this.mp4UnpackagerThread = new MP4UnpackagingThread(videoPackageRing,
				this.loopback.getReceiverInputStream(), failureListener);
		this.flvPackagingThread = new FLVPackagingThread(videoPackageRing,
				this.socket.getOutputStream(), failureListener,
				MediaSettings.getInstance());
	}

	public void startStreaming() {
		this.mp4UnpackagerThread.start();
		this.flvPackagingThread.start();
	}

	public void stopStreaming() {
		this.mp4UnpackagerThread.interrupt();
		this.flvPackagingThread.interrupt();
	}

	public void releaseResources() {
		if (loopback != null) {
			loopback.releaseLoopback();
			loopback = null;
		}

		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}

		videoPackageRing = null;
		mp4UnpackagerThread = null;
		flvPackagingThread = null;
	}

	public FileDescriptor getLoopbackTargetFD() {
		return loopback.getSenderFileDescriptor();
	}

	/**
	 * (De)activates error reporting of streaming threads via the
	 * {@link BroadcastStatusListener} provided in the constructor.
	 * 
	 * @param doErrorReporting
	 *            Whether to do error reporting or not
	 */
	public void setErrorReporting(boolean doErrorReporting) {
		this.mp4UnpackagerThread.setErrorReporting(doErrorReporting);
		this.flvPackagingThread.setErrorReporting(doErrorReporting);
	}

	public void setPackageForwarding(boolean doPackageForwarding) {
		this.mp4UnpackagerThread.setPackageForwarding(doPackageForwarding);
	}
}

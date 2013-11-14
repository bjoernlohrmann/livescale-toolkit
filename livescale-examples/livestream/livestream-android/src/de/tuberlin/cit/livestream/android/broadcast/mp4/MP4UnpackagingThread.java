package de.tuberlin.cit.livestream.android.broadcast.mp4;

import java.io.IOException;
import java.io.InputStream;

import android.os.SystemClock;
import android.util.Log;
import de.tuberlin.cit.livestream.android.broadcast.BroadcastStatusListener;
import de.tuberlin.cit.livestream.android.broadcast.BroadcastStatusListener.ErrorReason;
import de.tuberlin.cit.livestream.android.broadcast.MediaSettings;
import de.tuberlin.cit.livestream.android.broadcast.VideoPackage;
import de.tuberlin.cit.livestream.android.broadcast.VideoPackageRing;

public class MP4UnpackagingThread extends Thread {

	private static final String TAG = MP4UnpackagingThread.class
			.getSimpleName();

	public static final int MAX_FRAME_SIZE = 65536;

	public static final int TYPICAL_FRAME_SIZE = 4096;

	private TimestampEstimator frameTimeStamp;

	private VideoPackageRing videoPackageRing;

	private InputStream inputStream;

	private long startTime;

	private long bytesRead;

	private BroadcastStatusListener errorListener;

	private volatile boolean doErrorReporting;

	private volatile boolean doPackageForwarding;

	public MP4UnpackagingThread(VideoPackageRing videoPackageRing,
			InputStream inputStream, BroadcastStatusListener errorListener) {
		this.frameTimeStamp = new TimestampEstimator(
				MediaSettings.getInstance());
		this.videoPackageRing = videoPackageRing;
		this.inputStream = inputStream;
		this.errorListener = errorListener;
		this.doErrorReporting = true;
		this.doPackageForwarding = true;
	}

	public void run() {

		try {
			Log.d(TAG, "Waiting to skip MP4 header offset");
			Log.d(TAG,
					"Found after reading "
							+ MP4Utils.checkMP4_MDAT(inputStream) + " bytes");

			startTime = SystemClock.elapsedRealtime();
			frameTimeStamp.setFirstFrameTiming();
			while (!interrupted()) {
				boolean eofReached;

				if (doPackageForwarding) {
					eofReached = forwardNextPackage();
				} else {
					eofReached = dumpDataUntilEOF();
				}

				if (eofReached) {
					break;
				}
			}
		} catch (IOException e) {
			if (doErrorReporting) {
				Log.e(TAG,
						"Encountered IOException while reading from loopback.",
						e);
				errorListener.reportError(ErrorReason.SOURCE_FD_FAILURE);
			}
		} catch (InterruptedException e) {
			// ignore
		}
	}

	private boolean dumpDataUntilEOF() throws IOException {
		int read;
		do {
			read = inputStream.read();
		} while (read != -1);

		return true;
	}

	private boolean forwardNextPackage() throws IOException,
			InterruptedException {
		boolean success;
		// To improve frame timings, the only method allowed to block is
		// reading from the input stream (connection to camera). For this
		// purpose we allow interframe dropping (non-keyframes) in order to
		// ensure
		// we can get empty video packages from the package ring.
		VideoPackage packageToFill = videoPackageRing
				.takeEmptyVideoPackage(true);
		success = packageToFill.fillFromMP4InputStream(inputStream);

		if (success) {
			// packageToFill.setTimestamp(SystemClock.elapsedRealtime() -
			// startTime);
			packageToFill.setTimestamp(frameTimeStamp.getSequenceTimeStamp());
			frameTimeStamp.update();
			videoPackageRing.putFullVideoPackage(packageToFill);
		}

		return !success;
	}

	public int getBytesPerSecond() {
		long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000L;
		return (int) (bytesRead / (elapsedSeconds + 1));
	}

	public void setErrorReporting(boolean doErrorReporting) {
		this.doErrorReporting = doErrorReporting;
	}

	public void setPackageForwarding(boolean doPackageForwarding) {
		this.doPackageForwarding = doPackageForwarding;
	}
}

package de.tuberlin.cit.livestream.android.broadcast.flv;

import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;
import de.tuberlin.cit.livestream.android.broadcast.BroadcastStatusListener;
import de.tuberlin.cit.livestream.android.broadcast.BroadcastStatusListener.ErrorReason;
import de.tuberlin.cit.livestream.android.broadcast.MediaSettings;
import de.tuberlin.cit.livestream.android.broadcast.VideoPackageRing;

public class FLVPackagingThread extends Thread {

	private final static String TAG = FLVPackagingThread.class.getSimpleName();

	private final FlvPackager flvPackager;

	private BroadcastStatusListener statusListener;

	private volatile boolean doErrorReporting;

	public FLVPackagingThread(VideoPackageRing videoPackageRing,
			OutputStream outputStream, BroadcastStatusListener statusListener,
			MediaSettings mediaSettings) {

		this.flvPackager = new FlvPackager(videoPackageRing, outputStream,
				mediaSettings, statusListener);
		this.statusListener = statusListener;
		this.doErrorReporting = true;
	}

	public void run() {
		try {
			this.flvPackager.packageVideoIntoFlv();
		} catch (IOException e) {
			if (doErrorReporting) {
				Log.e(TAG, "Error during flv packaging", e);
				this.statusListener
						.reportError(ErrorReason.SERVER_CONNECTION_FAILURE);
			}
		} catch (InterruptedException e) {
		}
	}

	public void setErrorReporting(boolean doErrorReporting) {
		this.doErrorReporting = doErrorReporting;
	}
}

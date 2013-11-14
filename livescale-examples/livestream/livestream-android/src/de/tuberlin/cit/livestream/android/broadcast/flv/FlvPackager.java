package de.tuberlin.cit.livestream.android.broadcast.flv;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.os.SystemClock;
import de.tuberlin.cit.livestream.android.broadcast.BCConstants;
import de.tuberlin.cit.livestream.android.broadcast.BroadcastStatusListener;
import de.tuberlin.cit.livestream.android.broadcast.MediaSettings;
import de.tuberlin.cit.livestream.android.broadcast.VideoPackage;
import de.tuberlin.cit.livestream.android.broadcast.VideoPackageRing;

public class FlvPackager {
	// private static final String TAG = FlvPackager.class.getSimpleName();

	private MediaSettings mediaSettings;

	private VideoPackageRing videoPackageRing;

	private BufferedOutputStream outputStream;

	private BroadcastStatusListener statusListener;

	private long startTime;

	private int framesSentSinceLastStatusRefresh;

	private int bytesWrittenSinceLastStatusRefresh;

	private long timeOfLastStatusRefresh;

	public FlvPackager(VideoPackageRing videoPackageRing,
			OutputStream outputStream, MediaSettings mediaSettings,
			BroadcastStatusListener statusListener) {
		this.videoPackageRing = videoPackageRing;
		this.outputStream = new BufferedOutputStream(outputStream, 1400);
		this.mediaSettings = mediaSettings;
		this.statusListener = statusListener;
	}

	public void packageVideoIntoFlv() throws IOException, InterruptedException {
		startTime = SystemClock.elapsedRealtime();
		timeOfLastStatusRefresh = startTime;
		bytesWrittenSinceLastStatusRefresh = 0;
		framesSentSinceLastStatusRefresh = 0;

		byte[] sps = mediaSettings.getAvcConfig().getSequenceParameterSets()
				.get(0);
		byte[] pps = mediaSettings.getAvcConfig().getPictureParameterSets()
				.get(0);
		byte[] uint32Buffer = { 0, 0, 0, 0 };

		// write send token size
		byte[] streamServerToken = BCConstants.STREAM_SERVER_SEND_TOKEN
				.getBytes("US-ASCII");
		Serializer
				.writeUnsignedInt32(uint32Buffer, 0, streamServerToken.length);
		outputStream.write(uint32Buffer, 0, 4);

		// write send token
		outputStream.write(streamServerToken, 0, streamServerToken.length);

		bytesWrittenSinceLastStatusRefresh += streamServerToken.length
				+ uint32Buffer.length;

		// write first package size
		int packageSize = FlvUtils.getFlvHeaderSize()
				+ FlvUtils.getPreviousTagLengthSize()
				+ FlvUtils.getVideoMetaInformationSize(mediaSettings)
				+ FlvUtils.getAVCConfigSize(sps, pps);
		Serializer.writeUnsignedInt32(uint32Buffer, 0, packageSize);
		outputStream.write(uint32Buffer, 0, 4);

		// write first package
		FlvUtils.writeFlvHeader(outputStream, true, false);
		FlvUtils.writePreviousTagLength(0, outputStream);
		FlvUtils.writeVideoMetaInformation(mediaSettings, outputStream);
		FlvUtils.writeAVCConfig(outputStream, sps, pps);

		bytesWrittenSinceLastStatusRefresh += packageSize + uint32Buffer.length;

		while (!Thread.interrupted()) {
			VideoPackage videoPackage = videoPackageRing.takeFullVideoPackage();

			// write flv package size
			packageSize = FlvUtils.getVideoPackageSize(videoPackage);
			Serializer.writeUnsignedInt32(uint32Buffer, 0, packageSize);
			outputStream.write(uint32Buffer, 0, 4);

			// write flv package data
			FlvUtils.writeVideoPackage(outputStream, videoPackage);
			videoPackageRing.putEmptyVideoPackage(videoPackage);

			updateStatus(packageSize + uint32Buffer.length);
		}
	}

	private void updateStatus(int bytesSent) {
		framesSentSinceLastStatusRefresh++;
		bytesWrittenSinceLastStatusRefresh += bytesSent;

		long now = SystemClock.elapsedRealtime();
		long timePassed = now - timeOfLastStatusRefresh;
		if (timePassed >= 1000) {
			double framesPerSecond = framesSentSinceLastStatusRefresh
					/ (timePassed / 1000.0);
			double bytesPerSecond = bytesWrittenSinceLastStatusRefresh
					/ (timePassed / 1000.0);
			statusListener.reportStatus(framesPerSecond, bytesPerSecond);
			framesSentSinceLastStatusRefresh = 0;
			bytesWrittenSinceLastStatusRefresh = 0;
			timeOfLastStatusRefresh = now;
		}
	}
}

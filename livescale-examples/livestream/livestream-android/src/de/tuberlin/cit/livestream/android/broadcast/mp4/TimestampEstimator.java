package de.tuberlin.cit.livestream.android.broadcast.mp4;

import android.os.SystemClock;
import de.tuberlin.cit.livestream.android.broadcast.MediaSettings;

public class TimestampEstimator {

	private final int durationHistoryLength = 512;

	private int durationHistory[];

	private int durationHistoryIndex = 0;

	private int durationHistorySum = 0;

	private long lastFrameTiming = 0;

	private long sequenceDuration = 0;

	private int frameDuration;

	public TimestampEstimator(MediaSettings mediaSettings) {
		this.frameDuration = 1000 / mediaSettings.getVideoFrameRate();
		reset();
	}

	public void update() {
		long currentFrameTiming = SystemClock.elapsedRealtime();
		int newDuration = (int) (currentFrameTiming - lastFrameTiming);
		lastFrameTiming = currentFrameTiming;

		durationHistorySum -= durationHistory[durationHistoryIndex];
		durationHistorySum += newDuration;
		durationHistory[durationHistoryIndex] = newDuration;
		durationHistoryIndex = (durationHistoryIndex + 1)
				% durationHistoryLength;

		sequenceDuration += durationHistorySum / durationHistoryLength;
	}

	public void setFirstFrameTiming() {
		lastFrameTiming = SystemClock.elapsedRealtime() - durationHistorySum
				/ durationHistoryLength;
		sequenceDuration = 0;
	}

	public long getSequenceTimeStamp() {
		return sequenceDuration;
	}

	public void reset() {
		durationHistory = new int[durationHistoryLength];
		for (int i = 0; i < durationHistoryLength; i++) {
			durationHistory[i] = frameDuration;
		}
		durationHistorySum = frameDuration * durationHistoryLength;
		lastFrameTiming = 0;
		sequenceDuration = 0;
		durationHistoryIndex = 0;
	}
}

package de.tuberlin.cit.livestream.android.broadcast;

public interface BroadcastStatusListener {

	public enum ErrorReason {
		SOURCE_FD_FAILURE, SERVER_CONNECTION_FAILURE, INTERNAL_ERROR
	};

	public void reportError(ErrorReason reason);

	public void reportStatus(double framePerSecond, double bytesPerSecond);
}

package de.tuberlin.cit.livestream.android.view;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.AsyncTask;
import android.util.Log;
import de.tuberlin.cit.livestream.android.activity.Broadcast;
import de.tuberlin.cit.livestream.android.broadcast.BCConstants;
import de.tuberlin.cit.livestream.android.broadcast.BroadcastStatusListener;
import de.tuberlin.cit.livestream.android.broadcast.BroadcastStatusListener.ErrorReason;
import de.tuberlin.cit.livestream.android.broadcast.LoopbackToNetworkStreamer;
import de.tuberlin.cit.livestream.android.broadcast.MediaSource.State;
import de.tuberlin.cit.livestream.android.messaging.Messaging;

public class SetupStreamingAsyncTask extends
		AsyncTask<Void, Void, LoopbackToNetworkStreamer> {

	private static final String TAG = SetupStreamingAsyncTask.class
			.getSimpleName();

	private Broadcast broadcastActivity;

	private String username;

	private String password;

	private static AtomicBoolean hasReceivedStreamServerInformation = new AtomicBoolean(
			false);

	public SetupStreamingAsyncTask(Broadcast broadcastActivity,
			String username, String password) {
		this.broadcastActivity = broadcastActivity;
		this.username = username;
		this.password = password;
	}

	@Override
	protected LoopbackToNetworkStreamer doInBackground(Void... params) {
		LoopbackToNetworkStreamer streamer = null;
		try {
			if (broadcastActivity.getMediaSource().getState() == State.PREVIEWING) {
				waitUntilHasStreamServerInformation();

				streamer = new LoopbackToNetworkStreamer(
						BCConstants.STREAM_SERVER_SEND_HOST,
						BCConstants.STREAM_SERVER_SEND_PORT,
						new BroadcastStatusListener() {
							@Override
							public void reportError(ErrorReason reason) {
								broadcastActivity
										.handleStreamingFailure(reason);
							}

							@Override
							public void reportStatus(double framePerSecond,
									double bytesPerSecond) {
								broadcastActivity.postUpdateStatusOverlay(
										framePerSecond, bytesPerSecond);
							}
						});

				streamer.prepareStreaming();
			}
		} catch (Exception e) {
			Log.e(TAG, "Error when trying to prepare streaming", e);
			if (streamer != null) {
				streamer.releaseResources();
				streamer = null;
			}
		}

		return streamer;
	}

	private void waitUntilHasStreamServerInformation()
			throws InterruptedException, IOException {
		synchronized (hasReceivedStreamServerInformation) {
			hasReceivedStreamServerInformation.set(false);
			Messaging.getInstance().sendDispatcherRequestStreamSend(username, password);
			if (!hasReceivedStreamServerInformation.get()) {
				hasReceivedStreamServerInformation.wait(6000);
			}

			if (!hasReceivedStreamServerInformation.get()) {
				throw new IOException("Did not get stream server information");
			}
		}
	}

	public static void notifyHasStreamServerInformation() {
		synchronized (hasReceivedStreamServerInformation) {
			hasReceivedStreamServerInformation.set(true);
			hasReceivedStreamServerInformation.notify();
		}
	}

	protected void onPostExecute(LoopbackToNetworkStreamer streamer) {
		if (streamer != null) {
			broadcastActivity.startStreaming(streamer);
		} else {
			broadcastActivity
					.handleStreamingFailure(ErrorReason.SERVER_CONNECTION_FAILURE);
		}
	}

}

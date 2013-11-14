package de.tuberlin.cit.livestream.android.broadcast;

import java.io.FileDescriptor;
import java.io.IOException;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Provides media recording (currently only video) services. Video is dumped to
 * a provided file descriptor and can be displayed on a surface. When using this
 * class, first call initializePreview(), initializeCapture(), then
 * startCapture() and then stopCapture(). After stopping reinitialization is
 * possible by calling initialize() again.
 * 
 * @author Bjoern Lohrmann
 */
public class MediaSource {

	private final static String TAG = MediaSource.class.getSimpleName();

	private Camera camera;

	private MediaRecorder mediaRecorder;

	private State currentState;

	public enum State {
		UNINITIALIZED, PREVIEWING, CAPTURE_PREPARED, CAPTURING
	}

	public MediaSource() {
		this.currentState = State.UNINITIALIZED;
	}

	public synchronized State getState() {
		return currentState;
	}

	public synchronized void initializePreview(SurfaceHolder surfaceHolder)
			throws IOException {
		if (currentState != State.UNINITIALIZED) {
			throw new IllegalStateException(
					"Cannot initialize preview from state " + currentState);
		}

		this.camera = Camera
				.open(MediaSettings.getInstance().getCameraDevice());
		this.camera.setPreviewDisplay(surfaceHolder);
		this.camera.startPreview();
		this.currentState = State.PREVIEWING;
	}

	public synchronized void prepareCapture(FileDescriptor targetFile,
			SurfaceHolder previewSurface) throws IOException {
		if (currentState != State.PREVIEWING) {
			throw new IllegalStateException(
					"Cannot initialize preview from state " + currentState);
		}

		MediaSettings mediaSettings = MediaSettings.getInstance();

		this.camera.unlock();
		this.mediaRecorder = new MediaRecorder();
		this.mediaRecorder
				.setOnErrorListener(new MediaRecorder.OnErrorListener() {
					@Override
					public void onError(MediaRecorder mr, int what, int extra) {
						Log.e(TAG, String
								.format("Camera error: what %d / extra %d",
										what, extra));
					}
				});

		mediaRecorder.setCamera(this.camera);
		// mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mediaRecorder.setOutputFormat(mediaSettings.getOutputContainerFormat());
		mediaRecorder.setVideoEncodingBitRate(mediaSettings.getVideoBitRate());
		mediaRecorder.setVideoEncoder(mediaSettings.getVideoEncoder());
		mediaRecorder.setVideoSize(mediaSettings.getVideoWidth(),
				mediaSettings.getVideoHeight());
		mediaRecorder.setVideoFrameRate(mediaSettings.getVideoFrameRate());
		// mediaRecorder.setMaxDuration(0);
		// mediaRecorder.setMaxFileSize(0);
		mediaRecorder.setOutputFile(targetFile);
		mediaRecorder.setPreviewDisplay(previewSurface.getSurface());
		mediaRecorder.prepare();

		currentState = State.CAPTURE_PREPARED;
		Log.d(TAG, "MediaRecorder prepared! ");
	}

	public synchronized void startCapture() {
		if (currentState != State.CAPTURE_PREPARED) {
			throw new RuntimeException("Cannot start capture, not initialized!");
		}

		mediaRecorder.start();
		currentState = State.CAPTURING;
	}

	public synchronized void stopCapture() {
		if (currentState != State.CAPTURING) {
			throw new RuntimeException(
					"Cannot stop capture as capture has not been started!");
		}

		try {
			mediaRecorder.stop();
		} catch (RuntimeException e) {
			// ignored, because thrown if media recorder is stopped right after
			// being started so that no data
			// has been written
		}
		mediaRecorder.reset();
		mediaRecorder.release();
		mediaRecorder = null;
		this.camera.lock();
		this.currentState = State.PREVIEWING;
	}

	public synchronized void stopPreview() {
		if (currentState != State.PREVIEWING) {
			throw new RuntimeException(
					"Cannot stop capture as capture has not been started!");
		}
		this.camera.stopPreview();
		this.camera.release();
		this.camera = null;
		this.currentState = State.UNINITIALIZED;
	}
}

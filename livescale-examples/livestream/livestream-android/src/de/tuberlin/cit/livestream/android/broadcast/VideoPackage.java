package de.tuberlin.cit.livestream.android.broadcast;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;
import de.tuberlin.cit.livestream.android.broadcast.mp4.MP4UnpackagingThread;

public class VideoPackage {

	private static final String TAG = MP4UnpackagingThread.class
			.getSimpleName();

	private byte[] data;

	private int size;

	private boolean isFull;

	private boolean isKeyframe;

	private long timestamp;

	private boolean isFrame;

	public VideoPackage(int pkgSize) {
		this.data = new byte[pkgSize];
		reset();
	}

	public void reset() {
		this.size = 0;
		this.isFull = false;
		this.isKeyframe = false;
		this.isFrame = false;
		this.timestamp = -1;
	}

	/**
	 * Reads the next video package from the given stream.
	 * 
	 * @param in
	 *            The stream to read from.
	 * @return true if successful, false if end of stream has been reached-
	 * @throws IOException
	 *             thrown when reading from stream
	 */
	public boolean fillFromMP4InputStream(InputStream in) throws IOException {
		int bytesRead = fillBuffer(data, 0, 4, in);
		if (bytesRead != 4) {
			Log.d(TAG,
					"Encountered end of stream when trying to read package size.");
			return false;
		}

		int packageSize = (data[1] & 0xFF) * 65536 + (data[2] & 0xFF) * 256
				+ (data[3] & 0xFF);

		if (packageSize < 0) {
			throw new RuntimeException("Encountered negative package size.");
		}

		bytesRead = fillBuffer(data, 4, packageSize, in);
		if (bytesRead != packageSize) {
			Log.d(TAG,
					"Encountered end of stream when trying to read package data.");
			return false;
		}

		this.size = 4 + packageSize;
		this.isFull = true;

		// H.264 NAL unit type header (1byte) has form 0xXY where Y corresponds
		// to NAL unit type
		// type 5 are Intra-frames (keyframes), type 1 are
		// Predicted/Bidirectionally-predicted frames
		// (non-keyframes)
		int nalUnitType = (data[4] & 0x0F);
		this.isKeyframe = (nalUnitType == 5);
		this.isFrame = (nalUnitType == 1) || (nalUnitType == 5);
		return true;
	}

	public boolean fillFromInputStream(InputStream in) throws IOException {
		int read = in.read(data);

		if (read != -1) {
			this.isFull = true;
			this.size = read;
			return true;
		} else {
			return false;
		}
	}

	public boolean fillFromBuffer(byte[] buffer, int offset, int size) {
		int toCopy = Math.min(size, data.length);
		System.arraycopy(buffer, offset, data, 0, toCopy);
		this.isFull = true;
		this.size = toCopy;
		if (toCopy == size) {
			return true;
		} else {
			return false;
		}
	}

	private int fillBuffer(byte[] buf, int offset, int size, InputStream fis)
			throws IOException {
		if (offset + size > buf.length) {
			throw new IOException("Cannot read offset " + offset + " size "
					+ size + " in a buffer of length " + buf.length);
		}

		int totalRead = 0;
		while (totalRead < size) {
			int read = fis.read(buf, offset + totalRead, size - totalRead);

			if (read == -1) {
				break;
			} else {
				totalRead += read;
			}
		}

		return totalRead;
	}

	public boolean isKeyframe() {
		return isKeyframe;
	}

	public void setKeyframe(boolean vflag) {
		this.isKeyframe = vflag;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long ts) {
		this.timestamp = ts;
	}

	public byte[] getData() {
		return data;
	}

	public int getSize() {
		return size;
	}

	public boolean isFull() {
		return isFull;
	}

	public boolean isFrame() {
		return isFrame;
	}

}

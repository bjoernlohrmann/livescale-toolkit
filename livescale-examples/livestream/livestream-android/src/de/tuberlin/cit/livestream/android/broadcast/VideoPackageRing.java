package de.tuberlin.cit.livestream.android.broadcast;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

public class VideoPackageRing {

	private static final String TAG = VideoPackageRing.class.getSimpleName();

	private VideoPackage[] videoPackages;

	private LinkedBlockingQueue<VideoPackage> emptyPackages;

	private LinkedBlockingDeque<VideoPackage> fullPackages;

	private LinkedList<VideoPackage> toReenqueue;

	public VideoPackageRing(int bufferNum, int pkgSize) {
		emptyPackages = new LinkedBlockingQueue<VideoPackage>();
		fullPackages = new LinkedBlockingDeque<VideoPackage>();
		videoPackages = new VideoPackage[bufferNum];

		for (int i = 0; i < bufferNum; i++) {
			videoPackages[i] = new VideoPackage(pkgSize);
		}
		reset();

		toReenqueue = new LinkedList<VideoPackage>();
	}

	public VideoPackage takeEmptyVideoPackage(boolean dropInterframeIfNecessary)
			throws InterruptedException {
		VideoPackage toReturn;

		if (dropInterframeIfNecessary) {
			toReturn = emptyPackages.poll();
			if (toReturn == null) {
				toReturn = dropInterframeIfPossible();
			}
		} else {
			toReturn = emptyPackages.take();
		}

		return toReturn;
	}

	private VideoPackage dropInterframeIfPossible() {
		VideoPackage droppedFrame = null;

		while (droppedFrame == null) {
			VideoPackage dropCandidate = fullPackages.pollLast();
			if (dropCandidate == null) {
				// may happen due to scheduling effects
				droppedFrame = emptyPackages.poll();
			} else if (!dropCandidate.isFrame() || dropCandidate.isKeyframe()) {
				// don't drop non-frame-NAL-units and keyframes
				toReenqueue.addFirst(dropCandidate);
			} else {
				Log.v(TAG, "dropping interframe");
				droppedFrame = dropCandidate;
			}
		}

		droppedFrame.reset();
		fullPackages.addAll(toReenqueue);
		toReenqueue.clear();
		return droppedFrame;
	}

	public void putEmptyVideoPackage(VideoPackage emptyPackage)
			throws InterruptedException {
		emptyPackages.put(emptyPackage);
	}

	public VideoPackage takeFullVideoPackage() throws InterruptedException {
		return fullPackages.take();
	}

	public void putFullVideoPackage(VideoPackage fullPackage)
			throws InterruptedException {
		fullPackages.put(fullPackage);
	}

	public void reset() {
		for (VideoPackage videoPackage : videoPackages) {
			videoPackage.reset();
		}

		fullPackages.clear();
		emptyPackages.clear();
		emptyPackages.addAll(Arrays.asList(videoPackages));
	}

	public int countFullPackages() {
		return fullPackages.size();
	}

	public int getNoOfPackages() {
		return videoPackages.length;
	}
}

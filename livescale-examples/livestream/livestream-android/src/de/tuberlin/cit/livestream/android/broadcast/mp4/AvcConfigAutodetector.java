package de.tuberlin.cit.livestream.android.broadcast.mp4;

import java.io.File;

import com.coremedia.iso.boxes.h264.AvcConfigurationBox;

public class AvcConfigAutodetector {

	public static final String AUTODETECTION_FILE = "/sdcard/detect.mp4";

	public static boolean canDetect() {
		if (!getDetectionFile().exists()) {
			return false;
		} else {
			return detectAvcConfig() != null;
		}
	}

	public static AvcConfigurationBox detectAvcConfig() {
		try {
			return MP4Utils.detectAvcConfigurationBox(AUTODETECTION_FILE);
		} catch (Exception e) {
			getDetectionFile().delete();
			return null;
		}
	}

	public static File getDetectionFile() {
		return new File(AUTODETECTION_FILE);
	}
}

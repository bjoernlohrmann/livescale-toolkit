package de.tuberlin.cit.livestream.android.broadcast.flv;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import de.tuberlin.cit.livestream.android.broadcast.MediaSettings;
import de.tuberlin.cit.livestream.android.broadcast.VideoPackage;
import de.tuberlin.cit.livestream.android.broadcast.flv.amf.AMF0Serializer;

public class FlvUtils {
	// private final static String TAG = FlvUtils.class.getSimpleName();

	private static final byte VIDEO_TAG_TYPE = (byte) 0x09;

	// private static final byte AUDIO_TAG_TYPE = (byte) 0x08;

	private static final byte META_TAG_TYPE = (byte) 0x12;

	private static final byte[] FLV_HEADER_TEMPLATE = {
			// FLV header (FLV)
			(byte) 0x46, (byte) 0x4c, (byte) 0x56,
			// file version 1
			(byte) 0x01,
			// flags (video and audio tag presence)
			(byte) 0x00,
			// flv header length (including these four bytes)
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09, };

	public static int writeFlvHeader(OutputStream out, boolean containsVideo,
			boolean containsAudio) throws IOException {

		byte[] flvHeader = new byte[FLV_HEADER_TEMPLATE.length];
		System.arraycopy(FLV_HEADER_TEMPLATE, 0, flvHeader, 0,
				FLV_HEADER_TEMPLATE.length);

		byte typeFlags = 0x00;
		if (containsVideo) {
			typeFlags = (byte) (typeFlags | 0x01);
		}

		if (containsAudio) {
			typeFlags = (byte) (typeFlags | 0x04);
		}
		flvHeader[4] = typeFlags;
		out.write(flvHeader);

		return flvHeader.length;
	}

	public static int getFlvHeaderSize() {
		return FLV_HEADER_TEMPLATE.length;
	}

	public static int writeVideoMetaInformation(MediaSettings mediaSettings,
			OutputStream out) throws IOException {
		byte[] amf0MetaInfo = serializeMetaInfo(mediaSettings);
		int tagHeaderLength = writeTagHeader(META_TAG_TYPE,
				amf0MetaInfo.length, 0, 0, out);
		out.write(amf0MetaInfo);
		writePreviousTagLength(tagHeaderLength + amf0MetaInfo.length, out);
		return tagHeaderLength + amf0MetaInfo.length
				+ getPreviousTagLengthSize();
	}

	private static byte[] serializeMetaInfo(MediaSettings mediaSettings)
			throws IOException {
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		DataOutputStream serializationOutput = new DataOutputStream(byteOutput);
		AMF0Serializer serializer = new AMF0Serializer(serializationOutput);
		serializer.serialize("onMetaData");
		Map<String, Object> metaData = new HashMap<String, Object>();
		metaData.put("filesize", 0);
		metaData.put("duration", 0);
		metaData.put("width", mediaSettings.getVideoWidth());
		metaData.put("height", mediaSettings.getVideoHeight());
		metaData.put("videodatarate", mediaSettings.getVideoBitRate());
		metaData.put("videocodecid", 7);
		metaData.put("encoder", "Lavf52.87.1");
		metaData.put("framerate", mediaSettings.getVideoFrameRate());
		serializer.serialize(metaData);
		serializationOutput.close();
		return byteOutput.toByteArray();
	}

	public static int getVideoMetaInformationSize(MediaSettings mediaSettings)
			throws IOException {
		return getTagHeaderSize() + serializeMetaInfo(mediaSettings).length
				+ getPreviousTagLengthSize();
	}

	public static int writeAVCConfig(OutputStream out, byte[] sps, byte[] pps)
			throws IOException {

		int bodyLength = 16 + sps.length + pps.length;
		int tagHeaderLength = writeTagHeader(VIDEO_TAG_TYPE, bodyLength, 0, 0,
				out);

		// Frametype and CodecID
		out.write((byte) 0x17);
		out.write(0x00);

		// Composition time
		out.write(0x00);
		out.write(0x00);
		out.write(0x00);

		// VideoData->Data
		// Version
		out.write(0x01);

		// profile&level
		out.write(sps[1]);
		out.write(sps[2]);
		out.write(sps[3]);

		// reserved
		out.write((byte) 0xff);
		out.write((byte) 0xe1);

		// sps_size&data
		out.write((byte) (sps.length >> 8));
		out.write((byte) (sps.length & 0xFF));
		for (int i = 0; i < sps.length; i++)
			out.write(sps[i]);

		// pps_size&data
		out.write(0x01);
		out.write((byte) (pps.length >> 8));
		out.write((byte) (pps.length & 0xFF));
		for (int i = 0; i < pps.length; i++)
			out.write(pps[i]);

		writePreviousTagLength(bodyLength + tagHeaderLength, out);

		return tagHeaderLength + bodyLength + getPreviousTagLengthSize();
	}

	static public int getAVCConfigSize(byte[] sps, byte[] pps)
			throws IOException {
		int result = 0;

		result += getTagHeaderSize();

		// Frametype and CodecID
		result += 2;

		// Composition time
		result += 3;

		// VideoData->Data
		// Version
		result++;

		// profile&level
		result += 3;

		// reserved
		result += 2;

		// sps_size&data
		result += 2 + sps.length;

		// pps_size&data
		result += 3 + pps.length;

		result += getPreviousTagLengthSize();

		return result;
	}

	public static int writeVideoPackage(OutputStream out,
			VideoPackage videoPackage) throws IOException {
		int tagBodyLength = videoPackage.getSize() + 5;
		int tagHeaderLength = writeTagHeader(VIDEO_TAG_TYPE, tagBodyLength,
				videoPackage.getTimestamp(), 0, out);

		if (videoPackage.isKeyframe()) {
			// Log.v(TAG, "Adding AVC keyframe");
			out.write((byte) 0x17);
		} else {
			// Log.v(TAG, "Adding AVC interframe");
			out.write((byte) 0x27);
		}

		// unknown stuff
		out.write(1);
		out.write(0);
		out.write(0);
		out.write(0);

		out.write(videoPackage.getData(), 0, videoPackage.getSize());

		writePreviousTagLength(tagHeaderLength + tagBodyLength, out);

		return tagHeaderLength + tagBodyLength + getPreviousTagLengthSize();
	}

	public static int getVideoPackageSize(VideoPackage videoPackage)
			throws IOException {
		int result = 0;

		result += getTagHeaderSize();

		// unknown stuff
		result += 5;

		result += videoPackage.getSize();

		result += getPreviousTagLengthSize();

		return result;
	}

	private static byte[] tagHeaderBuffer = new byte[11];

	private static int writeTagHeader(byte type, int bodyLength,
			long timestamp, int streamId, OutputStream out) throws IOException {
		// frame type
		tagHeaderBuffer[0] = type;

		// frame body length
		Serializer.writeUnsignedInt24BigEndian(tagHeaderBuffer, 1, bodyLength);

		// frame time stamp
		tagHeaderBuffer[4] = (byte) ((timestamp >> 16) & 0xff);
		tagHeaderBuffer[5] = (byte) ((timestamp >> 8) & 0xff);
		tagHeaderBuffer[6] = (byte) (timestamp & 0xff);

		// frame time stamp extend
		tagHeaderBuffer[7] = (byte) ((timestamp >> 24) & 0xff);

		// stream id
		Serializer.writeUnsignedInt24BigEndian(tagHeaderBuffer, 8, streamId);

		out.write(tagHeaderBuffer);
		return tagHeaderBuffer.length;
	}

	private static int getTagHeaderSize() {
		return tagHeaderBuffer.length;
	}

	private static byte[] uint32Buffer = { 0, 0, 0, 0 };

	public static int writePreviousTagLength(int previousTagLength,
			OutputStream out) throws IOException {
		Serializer.writeUnsignedInt32(uint32Buffer, 0, previousTagLength);
		out.write(uint32Buffer);
		return uint32Buffer.length;
	}

	public static int getPreviousTagLengthSize() {
		return uint32Buffer.length;
	}
}

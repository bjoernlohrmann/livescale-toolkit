package de.tuberlin.cit.livestream.android.broadcast.flv;

/**
 * @author citadmin
 */
public class Serializer {
	public static final int writeUnsignedInt32(byte[] buffer, int offset,
			int value) {
		buffer[offset] = (byte) (value >>> 24);
		buffer[offset + 1] = (byte) (value >>> 16);
		buffer[offset + 2] = (byte) (value >>> 8);
		buffer[offset + 3] = (byte) value;
		return offset + 4;
	}

	public static final int readUnsignedInt32(byte[] buffer, int offset) {
		return (buffer[offset] << 24) + ((buffer[offset + 1] & 0xFF) << 16)
				+ ((buffer[offset + 2] & 0xFF) << 8)
				+ (buffer[offset + 3] & 0xFF);
	}

	public static final int writeInt32(byte[] buffer, int offset, int value) {
		buffer[offset++] = (byte) ((value >> 24) & 0xff);
		buffer[offset++] = (byte) ((value >> 16) & 0xff);
		buffer[offset++] = (byte) ((value >> 8) & 0xff);
		buffer[offset++] = (byte) ((value) & 0xff);
		return offset;
	}

	public static final int readInt32(byte[] buffer, int offset) {
		return (int) ((0xff & buffer[offset]) << 24
				| (0xff & buffer[offset + 1]) << 16
				| (0xff & buffer[offset + 2]) << 8 | (0xff & buffer[offset + 3]));
	}

	public static final int writeUnsignedInt24BigEndian(byte[] buffer,
			int offset, int value) {
		buffer[offset] = (byte) (value >>> 16);
		buffer[offset + 1] = (byte) (value >>> 8);
		buffer[offset + 2] = (byte) value;

		return offset + 3;
	}

	public static final int writeUnsignedInt16(byte[] buffer, int offset,
			int value) {
		buffer[offset] = (byte) (value >>> 8);
		buffer[offset + 1] = (byte) value;
		return offset + 2;
	}

	public static final int readUnsignedInt16(byte[] buffer, int offset) {
		return (buffer[offset] << 8) + (buffer[offset + 1] & 0xFF);
	}

	public static final int writeUnsignedLong(byte[] buffer, int offset,
			long value) {
		long v = value;
		for (int i = 0; i < 8; i++) {
			buffer[offset + 8 - 1 - i] = (byte) (v & 0xff);
			v = v >> 8;
		}
		return offset + 8;
	}

	public static final long readUnsignedLong(byte[] buffer, int offset) {
		long result = 0;
		for (int i = 0; i < 8; i++) {
			int b = buffer[offset + i];
			if (b < 0)
				b += 256;
			result = (result << 8) + b;
		}
		return result;
	}

	public static final int writeLong(byte[] buffer, int offset, long val) {
		buffer[offset++] = (byte) ((val >> 56) & 0xff);
		buffer[offset++] = (byte) ((val >> 48) & 0xff);
		buffer[offset++] = (byte) ((val >> 40) & 0xff);
		buffer[offset++] = (byte) ((val >> 32) & 0xff);
		buffer[offset++] = (byte) ((val >> 24) & 0xff);
		buffer[offset++] = (byte) ((val >> 16) & 0xff);
		buffer[offset++] = (byte) ((val >> 8) & 0xff);
		buffer[offset++] = (byte) ((val >> 0) & 0xff);
		return offset;
	}

	public static final long readLong(byte[] buffer, int offset) {
		return (long) (
		// (Below) convert to longs before shift because digits
		// are lost with ints beyond the 32-bit limit
		(long) (0xff & buffer[offset++]) << 56
				| (long) (0xff & buffer[offset++]) << 48
				| (long) (0xff & buffer[offset++]) << 40
				| (long) (0xff & buffer[offset++]) << 32
				| (long) (0xff & buffer[offset++]) << 24
				| (long) (0xff & buffer[offset++]) << 16
				| (long) (0xff & buffer[offset++]) << 8 | (long) (0xff & buffer[offset++]) << 0);
	}

	public static final int writeDouble64(byte[] buffer, int offset, double val) {
		return writeLong(buffer, offset, Double.doubleToRawLongBits(val));
	}

	public static final double readDouble64(byte[] buffer, int offset) {
		return Double.longBitsToDouble(readLong(buffer, offset));
	}

	public static final int writeFloat32(byte[] buffer, int offset, float val) {
		return writeUnsignedInt32(buffer, offset, Float.floatToRawIntBits(val));
	}

	public static final float readFloat32(byte[] buffer, int offset) {
		return Float.intBitsToFloat(readUnsignedInt32(buffer, offset));
	}

	public static final int writeIPv4(byte[] buffer, int offset, int encoded) {
		buffer[offset] = (byte) (encoded >>> 24);
		buffer[offset + 1] = (byte) (encoded >>> 16);
		buffer[offset + 2] = (byte) (encoded >>> 8);
		buffer[offset + 3] = (byte) encoded;
		return offset + 4;
	}

	public static final int readIPv4(byte[] buffer, int offset) {
		return (buffer[offset] << 24) + ((buffer[offset + 1] & 0xFF) << 16)
				+ ((buffer[offset + 2] & 0xFF) << 8)
				+ (buffer[offset + 3] & 0xFF);
	}

	public static final byte[] readArray(byte[] source, int offset, int len) {
		byte[] result = new byte[len];
		System.arraycopy(source, offset, result, 0, len);
		return result;
	}

	public static final int writeArray(byte[] dest, int offset, byte[] source) {
		System.arraycopy(source, 0, dest, offset, source.length);
		return offset + source.length;
	}

	public static final int writeArray(byte[] dest, int offset, byte[] source,
			int sourceOffset, int len) {
		System.arraycopy(source, sourceOffset, dest, offset, len);
		return offset + len;
	}

	public static final byte[] readArray16Len(byte[] source, int offset) {
		int len = Serializer.readUnsignedInt16(source, offset);
		offset += 2;
		byte[] result = new byte[len];
		System.arraycopy(source, offset, result, 0, len);
		return result;
	}

	public static final int writeArray16Len(byte[] dest, int offset,
			byte[] source) {
		assert (source.length < 65536);
		offset = Serializer.writeUnsignedInt16(dest, offset, source.length);
		System.arraycopy(source, 0, dest, offset, source.length);
		return offset + source.length;
	}
}
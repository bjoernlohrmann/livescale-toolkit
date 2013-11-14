package de.tuberlin.cit.livestream.android.broadcast.flv.amf;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Provides rudimentary deserialization of AMF0 objects from a DataInputStream
 * (just "rudimentary enough" functionality to read AMF0 objects in the META Tag
 * of a FLV file). This code is mainly taken (copy-pasted) from the
 * LGPL-licensed http://code.google.com/p/amf-serializer/ version 1.0.0 (2008),
 * class com.exadel.flamingo.flex.messaging.amf.io.AMF0Deserializer.
 * 
 * @author Bjoern Lohrmann
 */
public class AMF0Deserializer {

	private DataInputStream inputStream;

	public AMF0Deserializer(DataInputStream inputStream) {
		this.inputStream = inputStream;
	}

	public List<Object> deserialize() throws IOException {
		List<Object> deserializedObjects = new ArrayList<Object>();

		while (inputStream.available() > 0) {
			byte type = inputStream.readByte();
			Object object = readData(type);
			deserializedObjects.add(object);
		}

		return deserializedObjects;
	}

	protected Map<String, Object> readObject() throws IOException {
		return readObject(new HashMap<String, Object>());
	}

	protected Map<String, Object> readObject(Map<String, Object> aso)
			throws IOException {
		// Grab the key
		String key = inputStream.readUTF();
		for (byte type = inputStream.readByte(); type != 9; type = inputStream
				.readByte()) {
			// Grab the value
			Object value = readData(type);
			// Save the name/value pair in the map
			if (value != null) {
				System.out.println("read " + key + "=" + value);
				aso.put(key, value);
			}
			// Get the next name
			key = inputStream.readUTF();
		}
		return aso;
	}

	/**
	 * Reads array
	 * 
	 * @return
	 * @throws IOException
	 */
	protected List<?> readArray() throws IOException {
		// Init the array
		List<Object> array = new ArrayList<Object>();

		long length = inputStream.readInt();

		// Loop over all the elements in the data
		for (long i = 0; i < length; i++) {
			// Grab the type for each element
			byte type = inputStream.readByte();
			// Grab the element
			Object data = readData(type);
			array.add(data);
		}
		// Return the data
		return array;
	}

	protected Object readData(byte type) throws IOException {

		Object toReturn;

		switch (type) {
		case AMF0Types.DATA_TYPE_NUMBER: // 0
			toReturn = new Double(inputStream.readDouble());
			break;
		case AMF0Types.DATA_TYPE_BOOLEAN: // 1
			toReturn = Boolean.valueOf(inputStream.readBoolean());
			break;
		case AMF0Types.DATA_TYPE_STRING: // 2
			toReturn = inputStream.readUTF();
			break;
		case AMF0Types.DATA_TYPE_OBJECT: // 3
			toReturn = readObject();
			break;
		case AMF0Types.DATA_TYPE_NULL: // 5
		case AMF0Types.DATA_TYPE_UNDEFINED: // 6
			toReturn = null;
			break;
		case AMF0Types.DATA_TYPE_MIXED_ARRAY: // 8
			/* long length = */
			inputStream.readInt();
			// don't do anything with the length
			toReturn = readObject();
			break;
		case AMF0Types.DATA_TYPE_OBJECT_END: // 9
			toReturn = null;
			break;
		case AMF0Types.DATA_TYPE_ARRAY: // 10
			toReturn = readArray();
			break;
		case AMF0Types.DATA_TYPE_DATE: // 11
			toReturn = readDate();
			break;
		case AMF0Types.DATA_TYPE_LONG_STRING: // 12
			toReturn = readLongUTF(inputStream);
			break;
		default:
			throw new IOException("Unknown/unsupported object type " + type);
		}
		return toReturn;
	}

	/**
	 * Reads date
	 * 
	 * @return
	 * @throws IOException
	 */
	protected Date readDate() throws IOException {
		long ms = (long) inputStream.readDouble(); // Date in millis from
													// 01/01/1970

		// here we have to read in the raw
		// timezone offset (which comes in minutes, but incorrectly signed),
		// make it millis, and fix the sign.
		int timeoffset = inputStream.readShort() * 60000 * -1; // now we have
																// millis

		TimeZone serverTimeZone = TimeZone.getDefault();

		// now we subtract the current timezone offset and add the one that was
		// passed
		// in (which is of the Flash client), which gives us the appropriate ms
		// (i think)
		// -alon
		Calendar sent = new GregorianCalendar();
		sent.setTime((new Date(ms - serverTimeZone.getRawOffset() + timeoffset)));

		TimeZone sentTimeZone = sent.getTimeZone();

		// we have to handle daylight savings ms as well
		if (sentTimeZone.inDaylightTime(sent.getTime())) {
			//
			// Implementation note: we are trying to maintain compatibility
			// with J2SE 1.3.1
			//
			// As such, we can't use java.util.Calendar.getDSTSavings() here
			//
			sent.setTime(new java.util.Date(sent.getTime().getTime() - 3600000));
		}

		return sent.getTime();
	}

	/**
	 * This is a hacked verison of Java's DataInputStream.readUTF(), which only
	 * supports Strings <= 65535 UTF-8-encoded characters
	 */
	private Object readLongUTF(DataInputStream in) throws IOException {
		int utflen = in.readInt();
		StringBuffer str = new StringBuffer(utflen);
		byte bytearr[] = new byte[utflen];
		int c, char2, char3;
		int count = 0;

		in.readFully(bytearr, 0, utflen);

		while (count < utflen) {
			c = bytearr[count] & 0xff;
			switch (c >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				/* 0xxxxxxx */
				count++;
				str.append((char) c);
				break;
			case 12:
			case 13:
				/* 110x xxxx 10xx xxxx */
				count += 2;
				if (count > utflen)
					throw new UTFDataFormatException();
				char2 = bytearr[count - 1];
				if ((char2 & 0xC0) != 0x80)
					throw new UTFDataFormatException();
				str.append((char) (((c & 0x1F) << 6) | (char2 & 0x3F)));
				break;
			case 14:
				/* 1110 xxxx 10xx xxxx 10xx xxxx */
				count += 3;
				if (count > utflen)
					throw new UTFDataFormatException();
				char2 = bytearr[count - 2];
				char3 = bytearr[count - 1];
				if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
					throw new UTFDataFormatException();
				str.append((char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0)));
				break;
			default:
				/* 10xx xxxx, 1111 xxxx */
				throw new UTFDataFormatException();
			}
		}

		// The number of chars produced may be less than utflen
		return new String(str);
	}
}

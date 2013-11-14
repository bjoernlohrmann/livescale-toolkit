package de.tuberlin.cit.livestream.android.broadcast.flv.amf;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Provides rudimentary AMF0 serialization of objects into a DataInputStream
 * (just "rudimentary enough" functionality to write the AMF0 objects in the
 * META Tag of a FLV file). This code is mainly taken (copy-pasted) from the
 * LGPL-licensed http://code.google.com/p/amf-serializer/ version 1.0.0 (2008),
 * class com.exadel.flamingo.flex.messaging.amf.io.AMF0Serializer.
 * 
 * @author Bjoern Lohrmann
 */
public class AMF0Serializer {

	private static final int MILLIS_PER_HOUR = 60000;

	private DataOutputStream outputStream;

	public AMF0Serializer(DataOutputStream outputStream) {
		this.outputStream = outputStream;
	}

	/**
	 * Writes Data
	 * 
	 * @param value
	 * @throws java.io.IOException
	 */
	public void serialize(Object value) throws IOException {
		if (value == null) {
			// write null object
			outputStream.writeByte(AMF0Types.DATA_TYPE_NULL);
		} else if (isPrimitiveArray(value)) {
			writePrimitiveArray(value);
		} else if (value instanceof Number) {
			// write number object
			outputStream.writeByte(AMF0Types.DATA_TYPE_NUMBER);
			outputStream.writeDouble(((Number) value).doubleValue());
		} else if (value instanceof String) {
			writeString((String) value);
		} else if (value instanceof Character) {
			// write String object
			outputStream.writeByte(AMF0Types.DATA_TYPE_STRING);
			outputStream.writeUTF(value.toString());
		} else if (value instanceof Boolean) {
			// write boolean object
			outputStream.writeByte(AMF0Types.DATA_TYPE_BOOLEAN);
			outputStream.writeBoolean(((Boolean) value).booleanValue());
		} else if (value instanceof Date) {
			// write Date object
			outputStream.writeByte(AMF0Types.DATA_TYPE_DATE);
			outputStream.writeDouble(((Date) value).getTime());
			int offset = TimeZone.getDefault().getRawOffset();
			outputStream.writeShort(offset / MILLIS_PER_HOUR);
		} else if (value instanceof Object[]) {
			// write Object Array
			writeArray((Object[]) value);
		} else if (value instanceof Iterator) {
			write((Iterator<?>) value);
		} else if (value instanceof Collection) {
			write((Collection<?>) value);
		} else if (value instanceof Map) {
			writeMap((Map<?, ?>) value);
		}
	}

	protected boolean isPrimitiveArray(Object obj) {
		if (obj == null)
			return false;
		return obj.getClass().isArray()
				&& obj.getClass().getComponentType().isPrimitive();
	}

	protected void writePrimitiveArray(Object array) throws IOException {
		writeArray(convertPrimitiveArrayToObjectArray(array));
	}

	protected Object[] convertPrimitiveArrayToObjectArray(Object array) {
		Class<?> componentType = array.getClass().getComponentType();

		Object[] result = null;

		if (componentType == null) {
			throw new NullPointerException("componentType is null");
		} else if (componentType == Character.TYPE) {
			char[] carray = (char[]) array;
			result = new Object[carray.length];
			for (int i = 0; i < carray.length; i++) {
				result[i] = new Character(carray[i]);
			}
		} else if (componentType == Byte.TYPE) {
			byte[] barray = (byte[]) array;
			result = new Object[barray.length];
			for (int i = 0; i < barray.length; i++) {
				result[i] = new Byte(barray[i]);
			}
		} else if (componentType == Short.TYPE) {
			short[] sarray = (short[]) array;
			result = new Object[sarray.length];
			for (int i = 0; i < sarray.length; i++) {
				result[i] = new Short(sarray[i]);
			}
		} else if (componentType == Integer.TYPE) {
			int[] iarray = (int[]) array;
			result = new Object[iarray.length];
			for (int i = 0; i < iarray.length; i++) {
				result[i] = Integer.valueOf(iarray[i]);
			}
		} else if (componentType == Long.TYPE) {
			long[] larray = (long[]) array;
			result = new Object[larray.length];
			for (int i = 0; i < larray.length; i++) {
				result[i] = new Long(larray[i]);
			}
		} else if (componentType == Double.TYPE) {
			double[] darray = (double[]) array;
			result = new Object[darray.length];
			for (int i = 0; i < darray.length; i++) {
				result[i] = new Double(darray[i]);
			}
		} else if (componentType == Float.TYPE) {
			float[] farray = (float[]) array;
			result = new Object[farray.length];
			for (int i = 0; i < farray.length; i++) {
				result[i] = new Float(farray[i]);
			}
		} else if (componentType == Boolean.TYPE) {
			boolean[] barray = (boolean[]) array;
			result = new Object[barray.length];
			for (int i = 0; i < barray.length; i++) {
				result[i] = new Boolean(barray[i]);
			}
		} else {
			throw new IllegalArgumentException("unexpected component type: "
					+ componentType.getClass().getName());
		}

		return result;
	}

	protected int writeString(String str) throws IOException {
		int strlen = str.length();
		int utflen = 0;
		char[] charr = new char[strlen];
		int c, count = 0;

		str.getChars(0, strlen, charr, 0);

		// check the length of the UTF-encoded string
		for (int i = 0; i < strlen; i++) {
			c = charr[i];
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}

		/**
		 * if utf-encoded String is < 64K, use the "String" data type, with a
		 * two-byte prefix specifying string length; otherwise use the
		 * "Long String" data type, withBUG#298 a four-byte prefix
		 */
		byte[] bytearr;
		if (utflen <= 65535) {
			outputStream.writeByte(AMF0Types.DATA_TYPE_STRING);
			bytearr = new byte[utflen + 2];
		} else {
			outputStream.writeByte(AMF0Types.DATA_TYPE_LONG_STRING);
			bytearr = new byte[utflen + 4];
			bytearr[count++] = (byte) ((utflen >>> 24) & 0xFF);
			bytearr[count++] = (byte) ((utflen >>> 16) & 0xFF);
		}

		bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
		bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);
		for (int i = 0; i < strlen; i++) {
			c = charr[i];
			if ((c >= 0x0001) && (c <= 0x007F)) {
				bytearr[count++] = (byte) c;
			} else if (c > 0x07FF) {
				bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			} else {
				bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			}
		}

		outputStream.write(bytearr);
		return utflen + 2;
	}

	/**
	 * Writes Array Object - call <code>writeData</code> foreach element
	 * 
	 * @param array
	 * @throws java.io.IOException
	 */
	protected void writeArray(Object[] array) throws IOException {
		outputStream.writeByte(AMF0Types.DATA_TYPE_ARRAY);
		outputStream.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			serialize(array[i]);
		}
	}

	/**
	 * Writes collection
	 * 
	 * @param collection
	 *            Collection
	 * @throws java.io.IOException
	 */
	protected void write(Collection<?> collection) throws IOException {
		outputStream.writeByte(AMF0Types.DATA_TYPE_ARRAY);
		outputStream.writeInt(collection.size());
		for (Iterator<?> objects = collection.iterator(); objects.hasNext();) {
			Object object = objects.next();
			serialize(object);
		}
	}

	/**
	 * Writes Iterator - convert to List and call <code>writeCollection</code>
	 * 
	 * @param iterator
	 *            Iterator
	 * @throws java.io.IOException
	 */
	protected void write(Iterator<?> iterator) throws IOException {
		List<Object> list = new ArrayList<Object>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		write(list);
	}

	/**
	 * Writes Object Map
	 * 
	 * @param map
	 * @throws java.io.IOException
	 */
	protected void writeMap(Map<?, ?> map) throws IOException {
		outputStream.writeByte(AMF0Types.DATA_TYPE_MIXED_ARRAY);
		outputStream.writeInt(0);
		for (Iterator<?> entrys = map.entrySet().iterator(); entrys.hasNext();) {
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entrys.next();
			outputStream.writeUTF(entry.getKey().toString());
			serialize(entry.getValue());
		}
		outputStream.writeShort(0);
		outputStream.writeByte(AMF0Types.DATA_TYPE_OBJECT_END);
	}
}

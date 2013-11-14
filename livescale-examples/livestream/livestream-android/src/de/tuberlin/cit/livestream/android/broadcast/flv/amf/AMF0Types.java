package de.tuberlin.cit.livestream.android.broadcast.flv.amf;

/**
 * Defines AMF0 object types. This code is mainly taken (copy-pasted) from the
 * LGPL-licensed http://code.google.com/p/amf-serializer/ version 1.0.0 (2008),
 * class com.exadel.flamingo.flex.amf.AMF0Body
 * 
 * @author Bjoern Lohrmann
 */
public class AMF0Types {
	/**
	 * Unknow object type
	 */
	public static final byte DATA_TYPE_UNKNOWN = -1;

	/**
	 * Number object type
	 */
	public static final byte DATA_TYPE_NUMBER = 0;

	/**
	 * Boolean object type
	 */
	public static final byte DATA_TYPE_BOOLEAN = 1;

	/**
	 * String object type
	 */
	public static final byte DATA_TYPE_STRING = 2;

	/**
	 * Object object type
	 */
	public static final byte DATA_TYPE_OBJECT = 3;

	/**
	 * Movie clip object type
	 */
	public static final byte DATA_TYPE_MOVIE_CLIP = 4;

	/**
	 * NULL object type
	 */
	public static final byte DATA_TYPE_NULL = 5;

	/**
	 * Undefined object type
	 */
	public static final byte DATA_TYPE_UNDEFINED = 6;

	/**
	 * Reference object type
	 */
	public static final byte DATA_TYPE_REFERENCE_OBJECT = 7;

	/**
	 * Mixed Array Object type
	 */
	public static final byte DATA_TYPE_MIXED_ARRAY = 8;

	/**
	 * Object end type
	 */
	public static final byte DATA_TYPE_OBJECT_END = 9;

	/**
	 * Array Object type
	 */
	public static final byte DATA_TYPE_ARRAY = 10;

	/**
	 * Date object type
	 */
	public static final byte DATA_TYPE_DATE = 11;

	/**
	 * Long String object type
	 */
	public static final byte DATA_TYPE_LONG_STRING = 12;

	/**
	 * General Object type
	 */
	public static final byte DATA_TYPE_AS_OBJECT = 13;

	/**
	 * RecordSet object type
	 */
	public static final byte DATA_TYPE_RECORDSET = 14;

	/**
	 * XML Document object type
	 */
	public static final byte DATA_TYPE_XML = 15;

	/**
	 * Custom class object type
	 */
	public static final byte DATA_TYPE_CUSTOM_CLASS = 16;

	/**
	 * AMF3 data
	 */
	public static final byte DATA_TYPE_AMF3_OBJECT = 17;
}

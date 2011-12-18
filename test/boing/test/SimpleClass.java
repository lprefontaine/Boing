package boing.test;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

public class SimpleClass extends SimpleClassParent{
	private static String COLON = ":";

	@SuppressWarnings("unused")
	private byte byteVal;
	@SuppressWarnings("unused")
	private short shortVal;
	@SuppressWarnings("unused")
	private int intVal;
	@SuppressWarnings("unused")
	private long longVal;
	@SuppressWarnings("unused")
	private float floatVal;
	@SuppressWarnings("unused")
	private double doubleVal;
	@SuppressWarnings("unused")
	private char charVal = 0;
	@SuppressWarnings("unused")
	private boolean boolVal;
	@SuppressWarnings("unused")
	private String stringVal;
	@SuppressWarnings("unused")
	private Object objectVal;
	@SuppressWarnings("unused")
	private List listVal;
	@SuppressWarnings("unused")
	private Map mapVal;
	@SuppressWarnings("unused")
	private Properties props;
	@SuppressWarnings("unused")
	private Vector vector;

	public void setVector(Vector vector) {
		this.vector = vector;
	}

	public byte getByteVal() {
		return byteVal;
	}

	public long getLongVal() {
		return longVal;
	}

	public void setProps(Properties props) {
		this.props = props;
	}

	public void setListVal(List listVal) {
		this.listVal = listVal;
	}

	public void setMapVal(Map mapVal) {
		this.mapVal = mapVal;
	}

	public void init() {
		boolVal = true;
		System.out.println("In init() of class SimpleClass");
	}

	public void destroy() {
		System.out.println("In destroy() of class SimpleClass");
	}

	public void setByteVal(byte byteVal) {
		this.byteVal = byteVal;
	}

	public void setShortVal(short shortVal) {
		this.shortVal = shortVal;
	}

	public void setFloatVal(float floatVal) {
		this.floatVal = floatVal;
	}

	public void setDoubleVal(double doubleVal) {
		this.doubleVal = doubleVal;
	}

	public void setCharVal(char charVal) {
		this.charVal = charVal;
	}

	public void setBoolVal(boolean boolVal) {
		this.boolVal = boolVal;
	}

	public void setIntVal(int intVal) {
		this.intVal = intVal;
	}

	public void setLongVal(long longVal) {
		this.longVal = longVal;
	}

	public void setStringVal(String stringVal) {
		this.stringVal = stringVal;
	}

	public void setObjectVal(Object objectVal) {
		this.objectVal = objectVal;
	}

	public SimpleClass() {}

	public SimpleClass(byte byteVal) {
		super();
		this.byteVal = byteVal;
	}

	public SimpleClass(byte byteVal, short shortVal) {
		super();
		this.byteVal = byteVal;
		this.shortVal = shortVal;
	}

	public SimpleClass(byte byteVal, short shortVal, int intVal, long longVal) {
		super();
		this.byteVal = byteVal;
		this.shortVal = shortVal;
		this.intVal = intVal;
		this.longVal = longVal;
	}

	public SimpleClass(byte byteVal, short shortVal, int intVal, long longVal, String stringVal) {
		super();
		this.byteVal = byteVal;
		this.shortVal = shortVal;
		this.intVal = intVal;
		this.longVal = longVal;
		this.stringVal = stringVal;
	}

	public SimpleClass(byte byteVal, short shortVal, int intVal, long longVal, String stringVal,
			float floatVal) {
		super();
		this.byteVal = byteVal;
		this.shortVal = shortVal;
		this.intVal = intVal;
		this.longVal = longVal;
		this.stringVal = stringVal;
		this.floatVal = floatVal;
	}

	public SimpleClass(byte byteVal, short shortVal, int intVal, long longVal, String stringVal,
			float floatVal, double doubleval) {
		super();
		this.byteVal = byteVal;
		this.shortVal = shortVal;
		this.intVal = intVal;
		this.longVal = longVal;
		this.stringVal = stringVal;
		this.floatVal = floatVal;
		this.doubleVal = doubleval;
	}

	public SimpleClass(byte byteVal, short shortVal, int intVal, long longVal, String stringVal,
			float floatVal, double doubleval, char charVal) {
		super();
		this.byteVal = byteVal;
		this.shortVal = shortVal;
		this.intVal = intVal;
		this.longVal = longVal;
		this.stringVal = stringVal;
		this.floatVal = floatVal;
		this.doubleVal = doubleval;
		this.charVal = charVal;
	}

	public SimpleClass(byte byteVal, short shortVal, int intVal, long longVal, String stringVal,
			float floatVal, double doubleval, char charVal, boolean boolVal) {
		super();
		this.byteVal = byteVal;
		this.shortVal = shortVal;
		this.intVal = intVal;
		this.longVal = longVal;
		this.stringVal = stringVal;
		this.floatVal = floatVal;
		this.doubleVal = doubleval;
		this.charVal = charVal;
		this.boolVal = boolVal;
	}


	public SimpleClass(List listVal, Map mapVal) {
		super();
		this.listVal = listVal;
		this.mapVal = mapVal;
	}

	public SimpleClass(Properties props) {
		super();
		this.props = props;
	}

	public SimpleClass(Vector vector) {
		super();
		this.vector = vector;
	}

	public SimpleClass(Object objectVal) {
		super();
		this.objectVal = objectVal;
	}

	public String toString() {
		String value = ""+byteVal + COLON + shortVal + COLON + intVal+COLON+longVal+COLON+stringVal
		+ COLON + floatVal + COLON + doubleVal + COLON + StringEscapeUtils.escapeJava(String.valueOf(charVal)) + COLON + boolVal;
		if (listVal != null)
			value += COLON + listVal;
		if (mapVal != null)
			value += COLON + mapVal;
		if (props != null)
			value += COLON + props;
		if (vector != null)
			value += COLON + vector;
		return value;
	}

}

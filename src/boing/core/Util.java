package boing.core;

import java.util.HashMap;
import java.util.Map;

public class Util {
	private static Map<Class<?>,Class<?>> primitives =
		new HashMap<Class<?>,Class<?>>();
	static  {
		primitives.put(Byte.class,byte.class);
		primitives.put(Short.class,short.class);
		primitives.put(Integer.class,int.class);
		primitives.put(Long.class,long.class);
		primitives.put(Float.class,float.class);
		primitives.put(Double.class,double.class);
		primitives.put(Character.class,char.class);
		primitives.put(Boolean.class,boolean.class);
	};

	static public Class<?> getPrimitiveClass(Class<Object> cl) {
		Class<?> clp = primitives.get(cl);
		if (clp != null) return clp;
		return cl;
	}
}

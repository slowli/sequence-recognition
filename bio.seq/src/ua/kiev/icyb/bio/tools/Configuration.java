package ua.kiev.icyb.bio.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * Конфигурация задания, представляющая набор пар "ключ — значение".
 * Конфигурация может использоваться для присвоения свойств объектов.
 */
public class Configuration extends HashMap<String, String> {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Создает конфигурацию на основе аргументов командной строки.
	 * Каждый аргумент должен иметь вид {@code key=value}.
	 * 
	 * @param args
	 *    последовательность аргументов
	 */
	public Configuration(String... args) {
		super(args.length);
		
		for (int i = 0; i < args.length; i++) {
			String[] parts = args[i].split("\\s*=\\s*", 2);
			if (parts.length != 2) {
				throw new IllegalArgumentException("Illegal argument: " + args[i]);
			}
			
			this.put(parts[0], parts[1]);
		}
	}
	
	private static double[] parseDoubleArray(String str) {
		String[] parts = str.split("\\s*,\\s*");
		double[] parsed = new double[parts.length];
		for (int i = 0; i < parts.length; i++) {
			parsed[i] = Double.parseDouble(parts[i]);
		}
		
		return parsed;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object parseEnum(String str, Class<? extends Enum> enumType) {
		return Enum.valueOf(enumType, str.toUpperCase());
	}
	
	/**
	 * Присваивает значения свойств конфигурации объекту.
	 * Метод устанавливает значения публичных свойств объекта, имена которых совпадают
	 * с названиями свойств конфигурации. При этом производится преобразование из строкового типа
	 * к типу свойства.
	 * 
	 * @param obj
	 *    объект, свойства которого надо изменить
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setFields(Object obj) {
		Field[] fields = obj.getClass().getFields();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
				continue;
			}
			field.setAccessible(true);
			
			final Class<?> fieldType = field.getType();
			final String propName = field.getName();
			final String fieldStr = this.get(propName); 
			if (fieldStr == null) continue;
			
			try {
				if (fieldType.equals(int.class)) {
					field.setInt(obj, Integer.parseInt(fieldStr));
				} else if (fieldType.equals(boolean.class)) {
					field.setBoolean(obj, Boolean.parseBoolean(fieldStr));
				} else if (fieldType.equals(double.class)) {
					field.setDouble(obj, Double.parseDouble(fieldStr));
				} else if (fieldType.equals(String.class)) {
					field.set(obj, fieldStr);
				} else if (fieldType.equals(double[].class)) {
					field.set(obj, parseDoubleArray(fieldStr));
				} else if (fieldType.isEnum()) {
					field.set(obj, parseEnum(fieldStr, (Class<? extends Enum>) fieldType));
				} else {
				}
			} catch (IllegalAccessException e) {
				// Не должно происходить
			}
		}
	}
}

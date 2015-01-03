package ua.kiev.icyb.bio;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ua.kiev.icyb.bio.res.Messages;

/**
 * Класс, позволяющий читать и изменять значения всех полей объекта 
 * (включая частные и определенные в суперклассах).
 * Реализован в виде обертки вокруг объекта.
 */
public class FieldAccessor implements Representable {
	
	/** Объект, вокруг которого создана обертка. */
	private final Object obj;
	
	/** 
	 * Таблица полей объекта с полными именами (полное имя класса, в котором
	 * задекларировано поле + '.' + имя поля). 
	 */
	private final Map<String, Field> objFields = new HashMap<String, Field>();
	
	/**
	 * Создает обертку вокруг объекта, которая позволяет читать и изменять
	 * значения его полей.
	 *  
	 * @param obj
	 *    объект, для которого создается обертка
	 */
	public FieldAccessor(Object obj) {
		this.obj = obj;
		fillFields(obj);
	}
	
	/**
	 * Проверяет, разумно ли предоставлять к полю доступ. Статические и несохраняющиеся ({@code transient})
	 * поля менять не имеет смысла.
	 * 
	 * @param field
	 *    поле
	 * @return
	 * 	  {@code true}, если поле доступно для изменения
	 */
	private boolean isModifiable(Field field) {
		int modifiers = field.getModifiers();
		return !field.isSynthetic() 
				&& !Modifier.isStatic(modifiers)
				&& !Modifier.isTransient(modifiers);
	};
	
	/**
	 * Создает таблицу полей для конкретного объекта. 
	 * 
	 * @param obj
	 *    объект, для которого требуется создать таблицу полей
	 */
	private void fillFields(Object obj) {
		List<Field> fields = new ArrayList<Field>();
		Class<?> cls = obj.getClass();
		
		while (cls != null) {
			for (Field f : cls.getDeclaredFields()) {
				if (isModifiable(f)) {
					if (!f.isAccessible()) f.setAccessible(true);
					fields.add(f);
				}
			}
			cls = cls.getSuperclass();
		}
		
		for (Field f: fields) {
			String fieldName = f.getDeclaringClass().getCanonicalName() + "." + f.getName();
			objFields.put(fieldName, f);
		}
	}
	
	/**
	 * Возвращает допустимые поля объекта.
	 * 
	 * @return
	 *    коллекция, состоящая из полей объекта, значения которых можно изменять
	 */
	public Collection<Field> getFields() {
		return objFields.values();
	}
	
	/**
	 * Конвертирует строковое представление поля в соответствующий полю объект. 
	 * 
	 * @param fieldToSet
	 *    поле, для которого необходимо установить значение
	 * @param value
	 * 	  строковое представление значения
	 * @return
	 * 	  объект, который соответствует значению поля, или {@code null}, если
	 * 	  тип поля не позволяет создать объект
	 */
	private Object convertField(Class<?> type, String value) {
		Object objValue = null;
		
		// Целочисленные типы данных
		if (type.equals(int.class)) {
			value = value.trim();
			objValue = Integer.parseInt(value);
		} else if (type.equals(short.class)) {
			value = value.trim();
			objValue = Short.parseShort(value);
		} else if (type.equals(byte.class)) {
			value = value.trim();
			objValue = Byte.parseByte(value);
		} else if (type.equals(long.class)) {
			value = value.trim();
			objValue = Long.parseLong(value);
		}
		
		// Символьный тип
		if (type.equals(char.class)) {
			objValue = value.charAt(0);
		}
		
		// Типы данных с плавающей запятой
		if (type.equals(double.class)) {
			value = value.trim();
			objValue = Double.parseDouble(value);
		} else if (type.equals(float.class)) {
			value = value.trim();
			objValue = Float.parseFloat(value);
		} 
		
		// Булев тип
		if (type.equals(boolean.class)) {
			value = value.trim();
			objValue = Boolean.parseBoolean(value);
		}
		
		// Строки
		if (type.equals(String.class)) {
			objValue = value;
		}
		
		return objValue;
	}
	
	/**
	 * Устанавливает значение определенного поля объекта. 
	 * 
	 * @param name
	 * 	  название поля. Может включать в себя название класса, в котором задекларировано
	 * 	  поле (для избежания неоднозначностей).
	 * @param value
	 * 	  строковое представление нового значения поля
	 * 
	 * @throws IllegalArgumentException
	 * 	  если имя поля не позволяет установить его однозначно
	 * @throws UnsupportedOperationException
	 * 	  если тип поля не позволяет получить значение поля из строкового представления
	 */
	public void setField(String name, String value) {
		Field fieldToSet = null;
		
		for (Map.Entry<String, Field> entry : objFields.entrySet()) {
			String fieldName = entry.getKey();
			if (fieldName.equals(name) || fieldName.endsWith("." + name)) {
				if (fieldToSet != null) {
					throw new IllegalArgumentException(Messages.format("attr.ambiguous", fieldName));
				}
				fieldToSet = entry.getValue();
			}
		}
		
		if (fieldToSet != null) {
			Object objValue = convertField(fieldToSet.getType(), value);
			
			if (objValue == null) {
				throw new UnsupportedOperationException(Messages.format("attr.not_supported",
						fieldToSet.getType()));
			}
			try {
				fieldToSet.set(this.obj, objValue);
			} catch (IllegalAccessException e) {
				// Не должно вызываться
			}
		}
	}

	@Override
	public String repr() {
		String result = "";
		try {
			for (Field field : this.getFields()) {
				result += String.format("%s = %s\n", 
						field.toString(), field.get(this.obj));
			}
		} catch (IllegalAccessException e) {
			// Не должно вызываться
		}
		return result;
	}
}

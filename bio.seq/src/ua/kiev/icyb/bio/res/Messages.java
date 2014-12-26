package ua.kiev.icyb.bio.res;

import java.util.ResourceBundle;

/**
 * Класс, содержащий методы для работы с локализованными сообщениями.
 */
public class Messages {
	
	/**
	 * Имя ресурса, содержащего сообщения.
	 */
	public static final String BUNDLE_NAME = "ua.kiev.icyb.bio.res.messages";
	private Messages() {
	}
	
	/**
	 * Возвращает источник ресурсов, содержащего сообщения.
	 * 
	 * @return
	 *    активный источник ресурсов
	 */
	private static ResourceBundle getBundle() {
		return ResourceBundleUtils.getBundle(BUNDLE_NAME);
	}

	/**
	 * Проверяет, содержится ли строка с заданным идентификатором в ресурсах.
	 * 
	 * @param key
	 *    идентификатор строки
	 * @return
	 *    <code>true</code>, если строка есть в ресурсах
	 */
	public static boolean contains(String key) {
		return getBundle().containsKey(key);
	}
	
	/**
	 * Возвращает строку, содержащуются в ресурсах.
	 * Если строка отсутствует в ресурсах, возвращается строка {@code "!" + key + "!"}.
	 * 
	 * @param key
	 *    идентификатор строки в файле ресурсов
	 * @return
	 *    локализованная строка
	 */
	public static String getString(String key) {
		return ResourceBundleUtils.getString(getBundle(), key);
	}
	
	/**
	 * Возвращает строку, содержащуются в ресурсах с подстановкой аргументов.
	 * Если строка отсутствует в ресурсах, возвращается строка {@code "!" + key + "!"}.
	 * 
	 * @param key
	 *    идентификатор строки в файле ресурсов
	 * @param args
	 *    аргументы для подстановки
	 * @return
	 *    локализованная отформатированная строка
	 */
	public static String format(String key, Object... args) {
		return ResourceBundleUtils.format(getBundle(), key, args);
	}
}

package ua.kiev.icyb.bio.io.res;

import java.util.ResourceBundle;

import ua.kiev.icyb.bio.res.ResourceBundleUtils;

/**
 * Класс, содержащий методы для работы с локализованными сообщениями.
 */
public class Messages {
	
	/**
	 * Имя ресурса, содержащего сообщения.
	 */
	public static final String BUNDLE_NAME = "ua.kiev.icyb.bio.io.res.messages"; //$NON-NLS-1$
	
	private static ResourceBundle getBundle() {
		return ResourceBundleUtils.getBundle(BUNDLE_NAME);
	}

	private Messages() {
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

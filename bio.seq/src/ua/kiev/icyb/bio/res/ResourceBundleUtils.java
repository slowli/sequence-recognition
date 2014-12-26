package ua.kiev.icyb.bio.res;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Класс, содержащий утилиты для работы с файлами ресурсов.
 */
public class ResourceBundleUtils {
	/**
	 * Кодировка файлов локализации.
	 */
	private static final String CHARSET_NAME = "UTF-8"; //$NON-NLS-1$
	
	private static final ResourceBundle.Control CONTROL = new ResourceBundle.Control() {
		
		@Override
		public ResourceBundle newBundle(String baseName, 
				Locale locale,
                String format,
                ClassLoader loader,
                boolean reload) throws IOException, InstantiationException, IllegalAccessException {

			if (format.equals("java.properties")) {
				String bundleName = toBundleName(baseName, locale);
				String resourceName = toResourceName(bundleName, "properties");
				InputStream inp = loader.getResourceAsStream(resourceName);
				return new PropertyResourceBundle(new InputStreamReader(inp, CHARSET_NAME));
			} else {
				return super.newBundle(baseName, locale, format, loader, reload);
			}
		}
	};
	
	/**
	 * Возвращает источник ресурсов с заданным местоположением. В отличие от метода
	 * {@link ResourceBundle#getBundle(String)}, при работе с файлами ресурсов
	 * полагается, что информация в них содержится в кодировке UTF-8.
	 * 
	 * @param name
	 *    идентификатор источника ресурсов
	 * @return
	 *    источник ресурсов, или {@code null}, если источник с заданным идентификатором
	 *    не найден
	 */
	public static ResourceBundle getBundle(String name) {
		if (name == null) return null;
		try {
			return ResourceBundle.getBundle(name, CONTROL);
		} catch (MissingResourceException e) {
			return null;
		}
	}
	
	/**
	 * Возвращает строку, содержащуются в ресурсах.
	 * Если строка отсутствует в ресурсах, возвращается строка {@code "!" + key + "!"}.
	 * 
	 * @param bundle
	 *    источник ресурсов
	 * @param key
	 *    идентификатор строки в файле ресурсов
	 * @return
	 *    локализованная строка
	 */
	public static String getString(ResourceBundle bundle, String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
	/**
	 * Возвращает строку, содержащуются в ресурсах с подстановкой аргументов.
	 * Если строка отсутствует в ресурсах, возвращается строка {@code "!" + key + "!"}.
	 * 
	 * @param bundle
	 *    источник ресурсов
	 * @param key
	 *    идентификатор строки в файле ресурсов
	 * @param args
	 *    аргументы для подстановки
	 * @return
	 *    локализованная отформатированная строка
	 */
	public static String format(ResourceBundle bundle, String key, Object... args) {
		try {
			String message = bundle.getString(key);
			return MessageFormat.format(message, args);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}

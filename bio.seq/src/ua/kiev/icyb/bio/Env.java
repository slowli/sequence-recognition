package ua.kiev.icyb.bio;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ua.kiev.icyb.bio.res.Messages;

/**
 * Набор инструментов для работы с окруженим.
 */
public final class Env {
	
	private static final String DEFAULT_CONF = "ua/kiev/icyb/bio/res/default.conf";
	
	/**
	 * Файл конфигурации, в котором содержатся значения переменных окружения.
	 * Для чтения файла используется класс {@link Properties}, т.е. переменные
	 * задаются в формате
	 * <pre>
	 * &lt;имя переменной окружения&gt;=&lt;значение переменной&gt;
	 * </pre>
	 */
	public static final String CONF_FILENAME = "main.conf";
	
	/**
	 * Имя переменной конфигурации, содержащей уровень отладки, влияющий на детальность
	 * выводимой информации.
	 */
	public static final String DEBUG_PROPERTY = "debug";
	/**
	 * Имя переменной конфигурации, задающей количество вычислительных потоков при параллельных
	 * вычислениях.
	 */
	public static final String THREADS_PROPERTY = "threads";
	/**
	 * Имя переменной конфигурации, задающей используемую локаль.
	 */
	public static final String LOCALE_PROPERTY = "locale";
	/**
	 * Имя переменной конфигурации, задающей используемую кодировку в стандартном выводе
	 * и потоке ошибок.
	 */
	public static final String ENCODING_PROPERTY = "encoding";
	
	private static final Properties defaultProps;
	private static final Properties props;
	
	private static int debugLevel;
	private static int nThreads;
	private static ExecutorService executor;
	
	static {
		InputStream inStream = Env.class.getClassLoader().getResourceAsStream(DEFAULT_CONF);
		defaultProps = new Properties();
		try {
			defaultProps.load(new InputStreamReader(inStream, "UTF-8"));
			inStream.close();
		} catch (IOException e) {
			// Should not happen
		}
		
		props = new Properties(defaultProps);
		try {
			props.load(new FileReader(CONF_FILENAME));
		} catch (IOException e) {
			Env.error(0, Messages.format("env.e_load_conf", e));
		}
		
		debugLevel = intProperty(DEBUG_PROPERTY);
		nThreads = intProperty(THREADS_PROPERTY);
		if (nThreads <= 0) {
			nThreads = Runtime.getRuntime().availableProcessors();
		}
		
		String locale = property(LOCALE_PROPERTY);
		if (locale != null) {
			Locale.setDefault(new Locale(locale));
		}
		String encoding = property(ENCODING_PROPERTY);
		if (encoding != null) {
			try {
				System.setOut(new PrintStream(System.out, true, encoding));
				System.setErr(new PrintStream(System.err, true, encoding));
			} catch (UnsupportedEncodingException e) {
				Env.error(0, Messages.format("env.e_encoding", e));
			}
		}

		Env.debug(2, Messages.format("env.load_conf", CONF_FILENAME));
		if (locale != null) {
			Env.debug(2, Messages.format("env.locale", locale));
		}
		if (encoding != null) {
			Env.debug(2, Messages.format("env.encoding", encoding));
		}
		Env.debug(2, Messages.format("env.debug", debugLevel));
		Env.debug(2, Messages.format("env.threads", nThreads));
		Env.debug(2, "");
	} 
	
	/**
	 * Инициализирует переменные окружения, считывая их из соответствующего
	 * файла конфигурации.
	 */
	public static void initialize() {
		// Иницализация происходит в <clinit>
	}
	
	/**
	 * Возвращает уровень отладки, влияющий на детальность выводимой информации.
	 * Значение <code>0</code> соответстует выводу только наиболее важных сведений;
	 * значения меньше нуля подавляют весь вывод; значения больше нуля увеличивают
	 * объем информации для вывода.
	 * 
	 * @return
	 *    уровень отладки
	 */
	public static int debugLevel() {
		return debugLevel;
	}
	
	public static void setDebugLevel(int level) {
		debugLevel = level;
	}
	
	/**
	 * Возвращает значение переменной окружения с заданным именем.
	 * 
	 * @param name
	 *    имя переменной окружения
	 * @return
	 *    значение переменной окружения
	 */
	public static String property(String name) {
		return props.getProperty(name);
	}
	
	/**
	 * Возвращает целочисленное значение переменной окружения с заданным именем.
	 * 
	 * @param name
	 *    имя переменной окружения
	 * @return
	 *    значение переменной окружения
	 */
	public static int intProperty(String name) {
		try {
			return Integer.parseInt(property(name));
		} catch (NumberFormatException e) {
			return Integer.parseInt(defaultProps.getProperty(name));
		}
	}
	
	/**
	 * Возвращает булево значение переменной окружения с заданным именем.
	 * Преобразование к логическому типу осуществляется с помощью метода
	 * {@link Boolean#parseBoolean(String)}.
	 * 
	 * @param name
	 *    имя переменной окружения
	 * @return
	 *    значение переменной окружения
	 */
	public static boolean booleanProperty(String name) {
		return Boolean.parseBoolean(property(name));
	}
	
	/**
	 * Возвращает действительное значение переменной окружения с заданным именем.
	 * 
	 * @param name
	 *    имя переменной окружения
	 * @return
	 *    значение переменной окружения
	 */
	public static double doubleProperty(String name) {
		try {
			return Double.parseDouble(property(name));
		} catch (NumberFormatException e) {
			return Double.parseDouble(defaultProps.getProperty(name));
		}
	}
	
	/**
	 * Возвращает значение переменной окружения с заданным именем в виде массива действительных
	 * чисел. Текстовое значение переменной должно состоять из записи чисел с плавающей запятой,
	 * которую мог бы распознать метод {@link Double#parseDouble(String)}, разделенных
	 * пробелами, например, <code>"0.25 0.5 -1.3e-4"</code>.
	 * 
	 * @param name
	 *    имя переменной окружения
	 * @return
	 *    значение переменной окружения
	 */
	public static double[] doubleArrayProperty(String name) {
		String[] parts = property(name).split("\\s+");
		double[] parsed = new double[parts.length];
		
		try {
			for (int i = 0; i < parts.length; i++) {
				parsed[i] = Double.parseDouble(parts[i]);
			}
		} catch (NumberFormatException e) {
			parts = defaultProps.getProperty(name).split("\\s+");
			parsed = new double[parts.length];
			for (int i = 0; i < parts.length; i++) {
				parsed[i] = Double.parseDouble(parts[i]);
			}
		}
		return parsed;
	}
	
	/**
	 * Возвращает значение переменной окружения, заданной как значение перечисления.
	 * Текстовое значение переменной должно в точности совпадать с одной из констант
	 * перечисления.
	 * 
	 * @param name
	 *    имя переменной окружения
	 * @param enumClass
	 *    класс перечисления
	 * @return
	 *    значение переменной окружения
	 */
	public static <T extends Enum<T>> T enumProperty(String name, Class<T> enumClass) {
		try {
			return Enum.valueOf(enumClass, property(name));
		} catch (IllegalArgumentException e) {
			return Enum.valueOf(enumClass, defaultProps.getProperty(name));
		}
	}
	
	/**
	 * Устанавливает значения публичных полей объекта в соответствии с переменными, заданными
	 * в {@linkplain #CONF_FILENAME файле конфигурации}.
	 * 
	 * <p><b>Пример.</b> Пусть есть объект {@code obj} с полями {@code int a} и {@code boolean b}.
	 * Тогда при вызове метода
	 * <pre>
	 * Env.setFields(obj, "obj.");
	 * </pre>
	 * значения для полей будут браться из переменных конфигурации {@code "obj.a"} и {@code "obj.b"}.
	 * 
	 * @param obj
	 *    объект, для которого необходимо установить значения полей
	 * @param prefix
	 *    префикс для названий переменных в файле конфигурации
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void setFields(Object obj, String prefix) {
		Set<String> propNames = props.stringPropertyNames();
		
		Field[] fields = obj.getClass().getFields();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
				continue;
			}
			final Class<?> fieldType = field.getType();
			final String propName = prefix + field.getName();
			
			if (!propNames.contains(propName)) {
				continue;
			}
			
			try {
				if (fieldType.equals(int.class)) {
					field.setInt(obj, intProperty(propName));
				} else if (fieldType.equals(boolean.class)) {
					field.setBoolean(obj, booleanProperty(propName));
				} else if (fieldType.equals(double.class)) {
					field.setDouble(obj, doubleProperty(propName));
				} else if (fieldType.equals(double[].class)) {
					field.set(obj, doubleArrayProperty(propName));
				} else if (fieldType.isEnum()) {
					field.set(obj, enumProperty(propName, (Class<? extends Enum>)fieldType));
				} else {
				}
			} catch (IllegalAccessException e) {
			}
		}
	}
	
	/**
	 * Печатает отладочное сообщение в стандартный вывод {@link System#out}.
	 * 
	 * @param level
	 *    минимальный уровень отладки, необходимый чтобы напечатать сообщение
	 * @param message
	 *    печатаемое сообщение
	 */
	public static void debug(int level, String message) {
		if (debugLevel() >= level) {
			System.out.println(message);
		}
	}
	
	/**
	 * Печатает отладочное сообщение в стандартный поток ошибок {@link System#err}.
	 * 
	 * @param level
	 *    минимальный уровень отладки, необходимый чтобы напечатать сообщение
	 * @param message
	 *    печатаемое сообщение
	 */
	public static void error(int level, String message) {
		if (debugLevel() >= level) {
			System.err.println(message);
		}
	}
	
	/**
	 * Печатает короткое отладочное сообщение в стандартный вывод {@link System#out}.
	 * В отличие от метода {@link #debug(int, String)}, после вывода сообщения
	 * не ставится символ переноса строки <code>'\n'</code>.
	 * 
	 * @param level
	 *    минимальный уровень отладки, необходимый чтобы напечатать сообщение
	 * @param message
	 *    печатаемое сообщение
	 */
	public static void debugInline(int level, String message) {
		if (debugLevel() >= level) {
			System.out.print(message);
		}
	}
	
	/**
	 * Возвращает количество вычислительных потоков при параллельных
	 * вычислениях, задаваемое соответствующей переменной окружения.
	 * 
	 * @see #THREADS_PROPERTY
	 * 
	 * @return
	 *    количество вычислительных потоков
	 */
	public static int threadCount() {
		return nThreads;
	}
	
	/**
	 * Пул вычислительных потоков, который может использоваться для параллельных вычислений.
	 * Число потоков в пуле определяется методом {@link #threadCount()}.
	 * 
	 * @return
	 *    пул потоков для параллельных вычислений
	 */
	public static synchronized ExecutorService executor() {
		if (executor == null) {
			executor = Executors.newFixedThreadPool(threadCount());
		}
		return executor;
	}
}

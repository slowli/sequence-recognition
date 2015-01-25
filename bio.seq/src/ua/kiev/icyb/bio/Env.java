package ua.kiev.icyb.bio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import ua.kiev.icyb.bio.res.Messages;

/**
 * Набор инструментов для работы с окруженим.
 */
public class Env implements Representable {
	
	/**
	 * Поток для загрузки данных, предоставляющий доступ к окружению,
	 * в пределах которого выполняется загрузка.
	 */
	public static class ObjInputStream extends ObjectInputStream {

		/**
		 * Окружение, в котором выполняется загрузка.
		 */
		public final Env env;
		
		/**
		 * Создает поток для загрузки объектов.
		 * 
		 * @param in
		 *    исходный поток для чтения данных
		 * @param env
		 *    окружение
		 *    
		 * @throws IOException
		 *    при ошибке ввода/вывода
		 */
		public ObjInputStream(InputStream in, Env env) throws IOException {
			super(in);
			this.env = env;
		}
	}
	
	/**
	 * Создает автомат для чтения из текстового файла с буфером. Если имя файла заканчивается
	 * на «.gz», полагается, что файл сжат с помощью алгоритма GZIP.
	 * 
	 * @param filename
	 *    имя файла
	 * @return
	 *    автомат для считывания файла
	 * @throws IOException
	 *    если во время создания автомата произошла ошибка (например, файла не существует)
	 */
	public static BufferedReader getReader(String filename) throws IOException {
		if (filename.endsWith(".gz")) {
			InputStream inStream = new GZIPInputStream(new FileInputStream(filename));
			return new BufferedReader(new InputStreamReader(inStream));
		} else {
			return new BufferedReader(new FileReader(filename));
		}
	}

	/**
	 * Создает автомат для записи в текстовый файл с буфером. Если имя файла заканчивается
	 * на «.gz», полагается, что файл следует сжимать с помощью алгоритма GZIP.
	 * 
	 * @param filename
	 *    имя файла
	 * @return 
	 *    автомат для записи в файл
	 * @throws IOException
	 *    если во время создания автомата произошла ошибка
	 */
	public static BufferedWriter getWriter(String filename) throws IOException {
		if (filename.endsWith(".gz")) {
			OutputStream outStream = new GZIPOutputStream(new FileOutputStream(filename));
			return new BufferedWriter(new OutputStreamWriter(outStream));
		} else {
			return new BufferedWriter(new FileWriter(filename));
		}
	}

	/** Уровень отладки. */
	private int debugLevel = 0;
	
	/** Число потоков выполнения. */
	private int nThreads = -1;
	
	/** Пул потоков выполнения. */
	private ExecutorService executor;
	
	/** Именованные выборки. */
	private final Map<String, String> namedSets = new HashMap<String, String>(); 
	
	/** Текущее выполняемое задание. */
	private Launchable currentTask;
	
	/** Файл, в который сохраняются данные текущего задания. */
	private String taskSaveFile;
	
	private boolean interruptedByUser;
	
	private boolean interruptedByError;
	
	private final File workingDir;
	
	/** Кэш загруженных выборок данных. */
	private final Map<String, WeakReference<SequenceSet>> loadedSets = 
			new HashMap<String, WeakReference<SequenceSet>>();

	/**
	 * Создает окружение с настройками по умолчанию.
	 */
	public Env() {
		workingDir = new File(".");
	}
	
	/**
	 * Создает окружение с настройками, которые читаются из файла конфигурации.
	 * 
	 * @param configFile
	 *    имя файла конфигурации
	 * 
	 * @throws IOException
	 *    при ошибке чтения из файла
	 */
	public Env(String configFile) throws IOException {
		Properties props = new Properties();
		props.load(new FileReader(configFile));
		for (Map.Entry<Object, Object> entry : props.entrySet()) {
			addDataset((String) entry.getKey(), (String) entry.getValue());
		}
		
		File dir = new File(configFile).getParentFile();
		workingDir = (dir == null) ? new File(".") : dir;
	}
	
	/**
	 * Было ли выполнение текущего задания прервано пользователем (напр., 
	 * с помощью нажатия {@code ^C})?
	 * 
	 * @return
	 *    {@code true}, если выполнение прервано пользователем
	 */
	public boolean interruptedByUser() {
		return interruptedByUser;
	}
	
	/**
	 * Было ли выполнение текущего задания прервано из-за ошибки?
	 * 
	 * @return
	 *    {@code true}, если выполнение прервано из-за ошибки
	 */
	public boolean interruptedByError() {
		return interruptedByError;
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
	public int debugLevel() {
		return debugLevel;
	}
	
	/**
	 * Устанавливает уровень отладки. Чем выше уровень отладки, тем больше сообщений выводится во время
	 * выполнения алгоритмов.
	 * 
	 * @param level
	 *    новый уровень отладки
	 */
	public void setDebugLevel(int level) {
		debugLevel = level;
		this.debug(1, Messages.format("env.debug", debugLevel));
	}

	/**
	 * Устанавливает локаль.
	 * 
	 * @param locale
	 *    текстовое представление локали, например, 'en_US'
	 */
	public void setLocale(String locale) {
		Locale.setDefault(new Locale(locale));
		this.debug(1, Messages.format("env.locale", locale));
	}
	
	/**
	 * Устанавливает кодировку выходных потоков.
	 * 
	 * @param encoding
	 *    текстовое представление кодировки, например 'UTF-8'
	 */
	public void setEncoding(String encoding) {
		try {
			System.setOut(new PrintStream(System.out, true, encoding));
			System.setErr(new PrintStream(System.err, true, encoding));
		} catch (UnsupportedEncodingException e) {
			this.error(0, Messages.format("env.e_encoding", e));
		}
	}
	
	/**
	 * Устанавливает количество потоков выполнения.
	 * 
	 * @param threadCount
	 *    количество потоков или отрицательное число для автоматического выбора 
	 */
	public void setThreadCount(int threadCount) {
		if (threadCount <= 0) {
			threadCount = Runtime.getRuntime().availableProcessors();
		}
		
		nThreads = threadCount;
		this.debug(1, Messages.format("env.threads", nThreads));
	}
	
	/**
	 * Добавляет соответствие между именем выборки и файлом.
	 * 
	 * @param name
	 *    имя выборки
	 * @param filename
	 *    имя файла, содержащего данные выборки
	 */
	public void addDataset(String name, String filename) {
		namedSets.put(name, filename);
	}
	
	/**
	 * Уведомляет о возникновении исключительной ситуации.
	 * 
	 * @param e
	 *    объект исключения
	 * @throws RuntimeException
	 *    вызывается гарантированно
	 */
	public void exception(Exception e) throws RuntimeException {
		if (executor != null) executor.shutdownNow();
		
		if (e.getCause() instanceof RuntimeException) {
			throw (RuntimeException) e.getCause();
		} else {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Возвращает автомат для чтения указанной именованной выборки.
	 * Если указанное имя не соответствует выборке, оно трактуется как имя файла.
	 * 
	 * @param name
	 *    имя выборки
	 * @return
	 *    автомат для чтения выборки
	 *    
	 * @throws IOException 
	 *    если при создании автомата возникла ошибка ввода/вывода
	 */
	public BufferedReader resolveDataset(String name) throws IOException {
		String filename = namedSets.get(name);
		if (filename == null) filename = name;
		return getReader(workingDir + "/" + filename);
	}
	
	/**
	 * Печатает отладочное сообщение в стандартный вывод {@link System#out}.
	 * 
	 * @param level
	 *    минимальный уровень отладки, необходимый чтобы напечатать сообщение
	 * @param message
	 *    печатаемое сообщение
	 */
	public void debug(int level, String message) {
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
	public void error(int level, String message) {
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
	public void debugInline(int level, String message) {
		if (debugLevel() >= level) {
			System.out.print(message);
		}
	}
	
	/**
	 * Возвращает количество вычислительных потоков при параллельных
	 * вычислениях, задаваемое соответствующей переменной окружения.
	 * 
	 * @return
	 *    количество вычислительных потоков
	 */
	public int threadCount() {
		if (nThreads <= 0) setThreadCount(-1);
		return nThreads;
	}
	
	/**
	 * Пул вычислительных потоков, который может использоваться для параллельных вычислений.
	 * Число потоков в пуле определяется методом {@link #threadCount()}.
	 * 
	 * @return
	 *    пул потоков для параллельных вычислений
	 */
	public synchronized ExecutorService executor() {
		if (executor == null) {
			executor = Executors.newFixedThreadPool(threadCount());
		}
		return executor;
	}
	
	/**
	 * Запускает задание. 
	 * 
	 * @param task
	 *    задание
	 */
	public synchronized void run(Launchable task) {
		this.currentTask = task;
		
		Thread shutdownThread = new Thread(new Runnable() {

			@Override
			public void run() {
				if (!interruptedByError) {
					interruptedByUser = true;
					Env.this.saveProgress();
				}
			}
		});
		interruptedByUser = false;
		interruptedByError = false;
		Runtime.getRuntime().addShutdownHook(shutdownThread);
		
		try {
			task.run(this);
		} catch (RuntimeException e) {
			interruptedByError = true;
			throw e;
		} catch (Error e) {
			interruptedByError = true;
			throw e;
		}
		
		Runtime.getRuntime().removeShutdownHook(shutdownThread);
		
		this.currentTask = null;
	}
	
	/**
	 * Запускает задание с сохранением результатов выполнения в файл.
	 * 
	 * @param task
	 *    задание
	 * @param saveFile
	 *    файл, в который сохраняются результаты выполнения
	 */
	public synchronized void run(Launchable task, String saveFile) {
		this.taskSaveFile = saveFile;
		this.run(task);
		this.taskSaveFile = null;
	}
	
	/**
	 * Заргужает именованную выборку. Окружение поддерживает кэширование, т.е.
	 * при вызове метода с одинаковыми аргументами возвращается один и тот же объект. 
	 * 
	 * @param datasetName
	 *    имя выборки, которую нужно загрузить
	 * @return
	 *    объект выборки
	 * 
	 * @throws IOException
	 *    при ошибке ввода/вывода
	 */
	public synchronized SequenceSet loadSet(String datasetName) throws IOException {
		SequenceSet set = null;
		WeakReference<SequenceSet> ref = loadedSets.get(datasetName);
		if (ref != null) set = ref.get();
		
		if (set == null) {
			set = new NamedSequenceSet(datasetName, this);
			loadedSets.put(datasetName, new WeakReference<SequenceSet>(set));
		}
		return set;
	}
	
	/**
	 * Считывает сериализуемый объект из двоичного файла, в который тот был сохранен
	 * методом {@link #save(Serializable, String)}.
	 *  
	 * @param filename
	 *    имя файла
	 * @return
	 *    считанный объект
	 * @throws IOException
	 *    если во время чтения произошла ошибка ввода/вывода; в том числе, если не найден один
	 *    из классов сериализованных объектов
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T load(String filename) throws IOException {
		InputStream fis = new FileInputStream(filename);
		fis = new GZIPInputStream(fis);
		
	    ObjectInputStream ois = new Env.ObjInputStream(fis, this);
		Object obj;
		try {
			obj = ois.readObject();	
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		} finally {
			ois.close();
		}
		
		return (T) obj;
	}
	
	/**
	 * Записывает сериализуемый объект в двоичный файл. Файл сжимается с помощью алгоритма GZIP.
	 * 
	 * @param obj
	 *    объект, который надо записать
	 * @param filename
	 *    имя файла, в который производится запись
	 * @throws IOException
	 *    в случае ошибки ввода/вывода во время записи
	 */
	public void save(Serializable obj, String filename) throws IOException {
		OutputStream outStream = new FileOutputStream(filename);
		outStream = new GZIPOutputStream(outStream);
		
		ObjectOutputStream stream = new ObjectOutputStream(outStream); 
		stream.writeObject(obj);
		stream.close();
	}
	
	/**
	 * Сохраняет данные текущего задания в файл, заданный при вызове метода {@link #run(Launchable, String)}.
	 * 
	 * <p>Эта имплементация метода дополнительно пытается перед сохранением скопировать предыдущее
	 * сохранение в файл, имя которого получается добавлением к исходному имени 
	 * тильды <code>'~'</code> (например, {@code test.run~} для {@code test.run}).
	 */
	public String saveProgress() {
		if ((taskSaveFile != null) && (currentTask instanceof Serializable)) {
			File save = new File(taskSaveFile), backup = new File(taskSaveFile + "~");
			if (save.exists()) {
				backup.delete();
				save.renameTo(backup);
			}
			
			try {
				this.save((Serializable) currentTask, taskSaveFile);
			} catch (IOException e) {
				this.error(0, Messages.format("misc.save_error", e));
				this.exception(e);
			}

			this.debug(1, Messages.format("misc.save", taskSaveFile));
			return taskSaveFile;
		}
		
		return null;
	}
	
	@Override
	public String repr() {
		String repr = "Datasets:\n";
		for (Map.Entry<String, String> entry : namedSets.entrySet()) {
			repr += "  " + entry.getKey() + " -> " + entry.getValue() + "\n";
		}
		return repr;
	}
	
	@Override
	public void finalize() {
		if (executor != null) executor.shutdownNow();
	}
}

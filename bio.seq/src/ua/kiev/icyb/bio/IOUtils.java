package ua.kiev.icyb.bio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.io.Serializable;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * Утилиты, связанные с вводом/выводом.
 */
public class IOUtils {
	
	/**
	 * Название файла, в котором содержатся сведения о именованных выборках.
	 * Для управления сведениями используется класс {@link Properties}, т.е. соответствия
	 * между именованными выборками и файлами задаются с помощью строк вида
	 * <pre>
	 * &lt;имя выборки&gt;=&lt;имя файла&gt;
	 * </pre>
	 * например,
	 * <pre>
	 * human=dna/Human/genes.gz
	 * human.small=dna/Human/small.gz
	 * </pre>
	 */
	public static final String DATASETS_CONF = "datasets.conf";
	
	/**
	 * Загружает именованную выборку или набор выборок.
	 * Соответствие между именем выборки и файлом, в которой она хранится, осуществляется с помощью метода
	 * {@link #resolveDataset(String)}. Если выборок несколько, их имена
	 * должны быть разделены запятой (например, <code>"human,elegans"</code>); 
	 * полученная выборка будет равна конкатенации этих выборок в порядке их указания.
	 * 
	 * @param name
	 *    имя одной или более выборок, разделенных запятыми
	 * @return
	 *    созданная выборка
	 * @throws IllegalArgumentException
	 *    если по крайней мере одно из указанных имен выборок не соответствует файлу
	 * @throws IOException
	 *    если при чтении файлов выборок произошла ошибка ввода/вывода
	 */
	public static SequenceSet createSet(String name) throws IllegalArgumentException, IOException {
		return new GenericSequenceSet(name);
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
	
	/**
	 * Считывает сериализуемый объект из двоичного файла, в который тот был сохранен
	 * методом {@link #writeObject(String, Serializable)}.
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
	public static<T extends Serializable> T readObject(String filename) throws IOException {
		InputStream fis = new FileInputStream(filename);
		fis = new GZIPInputStream(fis);
		
	    ObjectInputStream ois = new ObjectInputStream(fis);
		Object obj;
		try {
			obj = ois.readObject();			
			return (T) obj;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			ois.close();
		}
	}
	
	/**
	 * Записывает сериализуемый объект в двоичный файл. Файл сжимается с помощью алгоритма GZIP.
	 * 
	 * @param filename
	 *    имя файла, в который производится запись
	 * @param obj
	 *    объект, который надо записать
	 * @throws IOException
	 *    в случае ошибки ввода/вывода во время записи
	 */
	public static void writeObject(String filename, Serializable obj) throws IOException {
		OutputStream outStream = new FileOutputStream(filename);
		outStream = new GZIPOutputStream(outStream);
		
		ObjectOutputStream stream = new ObjectOutputStream(outStream); 
		stream.writeObject(obj);
		stream.close();
	}
	
	/**
	 * Возвращает путь к файлу, содержащему указанную именованную выборку.
	 * 
	 * @param name
	 *    имя выборки
	 * @return
	 *    путь к файлу, содержащему выборку; {@code null}, если в {@linkplain #DATASETS_CONF файле конфигурации} 
	 *    нет выборки с заданным именем
	 */
	public static String resolveDataset(String name) {
		Properties datasets = new Properties();
		try {
			datasets.load(new FileReader(DATASETS_CONF));
		} catch (IOException e) {
			Env.error(0, "Exception while reading datasets configuration: " + e);
			return null;
		}
		
		String filename = datasets.getProperty(name, null);
		return filename;
	}
}

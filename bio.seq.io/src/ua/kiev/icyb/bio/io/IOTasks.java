package ua.kiev.icyb.bio.io;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.biojava.bio.BioException;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.Task;
import ua.kiev.icyb.bio.TaskRunner;
import ua.kiev.icyb.bio.io.res.Messages;

/**
 * Контейнер для заданий, связанных с преобразованием файлов, содержащих данные
 * для задач распознавания скрытых последовательностей.
 */
public class IOTasks extends TaskRunner {
	
	/**
	 * Получает имя файла, в которой сохраняется выборка, соответствующая определенному
	 * файлу Genbank.
	 * 
	 * @param dirName
	 *    имя директории, в которую сохраняется файл
	 * @param inputFilename
	 *    имя входного файла
	 * @return
	 *    имя выходного файла для хранения выборки
	 */
	private static String getOutputFile(String dirName, String inputFilename) {
		File outFolder = new File(dirName), inFile = new File(inputFilename);
		String filename = inFile.getName();
		if (!filename.endsWith(".gz")) filename += ".gz";
		return new File(outFolder, filename).toString();
	}
	
	/**
	 * Преобразует один или более файлов в формате Genbank в выборки, соответствующие
	 * интерфейсу {@link SequenceSet}. Каждый входной файл порождает отдельную выборку.
	 * 
	 * <p><b>Аргументы:</b>
	 * <ol>
	 * <li>папка для хранения полученных файлов выборок;
	 * <li>(необязательно) разделенные запятой параметры преобразования, перед которыми стоят 
	 * два дефиса, например <code>--unique,unknown</code>. Допустимые параметры:
	 * <ul>
	 * <li>{@code unique} — рассматривать не более одной кодирующей последовательности для
	 * каждого гена;
	 * <li>{@code no-unique} — рассматривать все кодирующие последовательности для
	 * каждого гена (по умолчанию);
	 * <li>{@code unknown} — не игнорировать гены, содержащие неизвестные нуклеотиды;
	 * <li>{@code no-unknown} — игнорировать гены, содержащие неизвестные нуклеотиды
	 * (по умолчанию);
	 * </ul>
	 * <li>список файлов в формате Genbank для трансформации (могут быть сжаты с помощью GZIP)
	 * </ol>
	 * 
	 * <p><b>Пример.</b>
	 * <pre>
	 * genbank out --unique genbank/elegans-i.gz genbank/elegans-ii.gz
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, приведенные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 * @throws BioException
	 *    в случае ошибки чтения файла Genbank
	 */
	@Task(id="genbank",res=Messages.BUNDLE_NAME)
	public static void transformGenbank(String[] args) throws IOException, BioException {
		final String outputDir = args[0];
		boolean unique = false;
		boolean allowUnknown = false;
		
		int startIdx = 1;
		if (args[1].startsWith("--")) {
			String[] params = args[1].substring(2).split(",");
			for (String param : params) {
				if (param.equals("unique")) {
					unique = true;
				} else if (param.equals("no-unique")) {
					unique = false;
				} else if (param.equals("unknown")) {
					allowUnknown = true;
				} else if (param.equals("no-unknown")) {
					allowUnknown = false;
				}
			}
			startIdx = 2;
		}
		final String[] files = Arrays.copyOfRange(args, startIdx, args.length); 
		
		Env.debug(1, Messages.format("misc.out_dir", outputDir));
		Env.debug(1, Messages.format("genbank.unique", unique));
		Env.debug(1, Messages.format("genbank.unknown", allowUnknown));
		Env.debug(1, Messages.format("genbank.input", files.length));
		Env.debug(1, "");
				
		for (String gbFile : files) {
			System.gc();
			Env.debug(0, "");
			Env.debug(0, Messages.format("genbank.cur_file", gbFile));
			GenbankReader r = new GenbankReader(gbFile);
			r.allowUnknownNts = allowUnknown;
			r.uniqueGenes = unique;
			SequenceSet set = r.transform();
			Env.debug(0, Messages.format("misc.out_set", set.repr()));
			
			String outFilename = getOutputFile(outputDir, gbFile);
			Env.debug(0, Messages.format("misc.save", outFilename));
			set.saveToFile(outFilename);
		}
	}
	
	/**
	 * Преобразует один или более файлов в формате DSSP в выборку, соответствующую
	 * интерфейсу {@link SequenceSet}. Все входные файлы сохраняются в одну выборку.
	 * 
	 * <p><b>Аргументы:</b>
	 * <ol>
	 * <li>файл для хранения полученной выборки;
	 * <li>(необязательно) разделенные запятой параметры преобразования, перед которыми 
	 * стоят два дефиса, например <code>--unames,no-breaks</code>. Допустимые параметры:
	 * <ul>
	 * <li>{@code unames} — формировать выборку из белков, уникальных по имени (по умолчанию);
	 * <li>{@code no-unames} — не проверять уникальность имени белков;
	 * <li>{@code prefix} — формировать выборку из белков с уникальным началом 
	 * аминокислотной последовательности;
	 * <li>{@code no-prefix} — не проверять уникальность строки аминокислот белка (по умолчанию);
	 * <li>{@code breaks} — включать разрывы в цепочке аминокислот белка;
	 * <li>{@code no-breaks} — игнорировать разрывы в цепочке аминокислот белка (по умолчанию).
	 * </ul>
	 * <li>список файлов в формате DSSP для трансформации
	 * </ol>
	 * 
	 * <p><b>Пример.</b>
	 * <pre>
	 * dssp proteins.gz --unames,no-breaks *.dssp
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, приведенные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	@Task(id="dssp",res=Messages.BUNDLE_NAME)
	public static void transformDSSP(String[] args) throws IOException {
		final String outFilename = args[0];
		boolean uniqueNames = true, uniquePrefix = false, breaks = false;
		
		int startIdx = 1;
		if (args[1].startsWith("--")) {
			String[] params = args[1].substring(2).split(",");
			for (String param : params) {
				if (param.equals("unames")) {
					uniqueNames = true;
				} else if (param.equals("no-unames")) {
					uniqueNames = false;
				} else if (param.equals("prefix")) {
					uniquePrefix = true;
				} else if (param.equals("no-prefix")) {
					uniquePrefix = false;
				} else if (param.equals("breaks")) {
					breaks = true;
				} else if (param.equals("no-breaks")) {
					breaks = false;
				}
			}
			startIdx = 2;
		}
		
		Env.debug(1, Messages.format("dssp.u_names", uniqueNames));
		Env.debug(1, Messages.format("dssp.u_prefix", uniquePrefix));
		Env.debug(1, Messages.format("dssp.breaks", breaks));
		Env.debug(1, "");
		
		DSSPReader reader = new DSSPReader();
		reader.uniqueNames = uniqueNames;
		reader.uniquePrefix = uniquePrefix;
		reader.includeBreaks = breaks;
		
		for (int i = startIdx; i < args.length; i++) {
			reader.read(args[i]);
		}
		
		SequenceSet proteins = reader.getSet();
		Env.debug(0, Messages.format("misc.out_set", proteins.repr()));
		Env.debug(0, Messages.format("misc.save", outFilename));
		proteins.saveToFile(outFilename);
	}
	
	@Override
	protected void addTasks() {
		super.addTasks();
		addTaskHolder(IOTasks.class);
	}
	
	/**
	 * Запускает задание на исполнение.
	 * 
	 * @param args
	 *    идентификатор задания, за которым следуют его аргументы; если идентификатор
	 *    не указан (то есть <code>args.length == 0</code>), выводится список
	 *    всех зарегистрированных заданий
	 * @throws IllegalArgumentException
	 *    если неверно задан идентификатор задания
	 * @throws InvocationTargetException
	 *    если метод, соответствующий заданию, не обладает корректной сигнатурой 
	 * @throws IllegalAccessException 
	 *    в случае неверной области видимости метода
	 */
	public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		execute(new IOTasks(), args);
	}
}

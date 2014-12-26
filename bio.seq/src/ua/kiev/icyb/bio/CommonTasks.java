package ua.kiev.icyb.bio;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import ua.kiev.icyb.bio.res.Messages;

/**
 * Контейнер для заданий общего плана.
 */
public class CommonTasks {
	
	/**
	 * Печатает информацию об объекте, сериализованном в файл.
	 * 
	 * @param args
	 *    имя файла с объектом, реализующим интерфейс {@link Representable}
	 * @throws IOException 
	 *    в случае ошибки чтения файла
	 */
	@Task(id="print",res=Messages.BUNDLE_NAME)
	public static void print(String[] args) throws IOException {
		final String filename = args[0];
		Object obj = IOUtils.readObject(filename);
		Env.debug(0, Messages.format("misc.file", filename));
		Env.debug(0, Messages.format("misc.class", obj.getClass().getName()));
		if (obj instanceof Representable) {
			System.out.println(((Representable) obj).repr());
		} else {
			System.out.println(obj.toString());
		}
	}
	
	/**
	 * Печатает значения всех полей сериализованного объекта (включая частные поля и поля
	 * суперклассов) и позволяет их изменять.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя файла с сериализованным объектом;
	 * <li>(опционально) названия полей объекта и их новые значения, разделенные 
	 * знаком равенства <code>=</code>. Имя поля может содержать в себе название класса,
	 * в котором поле задекларировано (для избежания неоднозначностей).
	 * </ol>
	 * 
	 * <p><b>Примеры.</b>
	 * <ul>
	 * <li> 
	 * <code>attr human.run</code>
	 * 
	 * <p>(выводит названия и значения всех полей объекта, сохраненного в файле {@code human.run}).
	 * 
	 * <li>
	 * <code>attr human.run "nIterations=20" "iteration=10"</code>
	 * 
	 * <p>изменяет значения полей {@code nIterations} и {@code iteration} в объекте из файла
	 * {@code human.run}, после чего перезаписывает этот файл.
	 * </ul>
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * 		
	 * @throws IOException
	 */
	@Task(id="attr",res=Messages.BUNDLE_NAME)
	public static void attr(String[] args) throws IOException {
		final String filename = args[0];
		Object obj = IOUtils.readObject(filename);
		Env.debug(0, Messages.format("misc.file", filename));
		Env.debug(0, Messages.format("misc.class", obj.getClass().getName()));
		
		FieldAccessor accessor = new FieldAccessor(obj);
		
		if (args.length == 1) {
			System.out.println(accessor.repr());
		} else {
			for (int i = 1; i < args.length; i++) {
				String[] parts = args[i].split("\\s*=", 2);
				accessor.setField(parts[0], parts[1]);
			}
			IOUtils.writeObject(filename, (Serializable) obj);
		}
	}
	
	/**
	 * Запускает сохраненный алгоритм.
	 * 
	 * @param args
	 *    имя файла, содержащего сохраненный объект, реализующий интерфейс 
	 *    {@link Launchable}
	 * @throws IOException
	 *    в случае ошибки чтения файла
	 */
	@Task(id="launch",res=Messages.BUNDLE_NAME)
	public static void launch(String[] args) throws IOException {
		Env.debug(0, Messages.format("misc.launchable_file", args[0]));
		Launchable launchable = IOUtils.readObject(args[0]);
		launchable.setSaveFile(args[0]);
		launch(launchable);
	}
	
	/**
	 * Запускает сохраненный алгоритм.
	 * 
	 * @param launchable
	 *    сохраненный объект
	 */
	public static void launch(Launchable launchable) {
		Env.debug(0, Messages.format("misc.class", launchable.getClass().getName()));
		launchable.run();
	}
	
	/**
	 * Объединяет две или более выборки и сохраняет результат в файл.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя файла для сохранения объединенной выборки;
	 * <li>имена файлов, в которых содержатся выборки, предназначенные для слияния.
	 * </ol>
	 * 
	 * <p><b>Пример.</b>
	 * <pre>
	 * set.merge elegans.gz elegans/I.gz elegans/II.gz elegans/III.gz 
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, приведенные выше
	 * @throws IOException
	 *    в случае ошибки чтения из файлов выборок или записи в целевой файл
	 */
	@Task(id="set.merge",res=Messages.BUNDLE_NAME)
	public static void merge(String[] args) throws IOException {
		final String outputFile = args[0];
		
		Env.debug(1, Messages.format("misc.out_file", outputFile));
		Env.debug(1, Messages.format("misc.in_files_n", args.length - 1));
		
		Env.debug(1, Messages.format("task.set.merge.load", args[1]));
		GenericSequenceSet set = new GenericSequenceSet(args[1], false);
		for (int i = 2; i < args.length; i++) {
			SequenceSet part = new GenericSequenceSet(args[i], false);
			Env.debug(1, Messages.format("task.set.merge.add", args[i]));
			set.addSet(part);
		}
		Env.debug(1, Messages.getString("task.set.merge.save"));
		set.saveToFile(outputFile);
	}
	
	/**
	 * Преобразовывает наблюдаемые и/или скрытые состояния в определенной выборке.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя файла, содержащего исходную выборку;
	 * <li>имя файла для сохранения преобразованной выборки;
	 * <li>отображение наблюдаемых состояний выборки; должно иметь формат {@code <src>:<dest>}, 
	 * где {@code <src>} и {@code <dest>} — строки одинаковой длины, содержащие исходные
	 * и соответствующие им преобразованные состояния;
	 * <li>отображение скрытых состояний выборки.
	 * </ol>
	 * 
	 * <p><b>Пример.</b>
	 * <pre>
	 * set.tr proteins.aa proteins-tr.aa : -TSGHIEB:---HHHSS
	 * </pre>
	 * (выполняет преобразование выборки белков, полученной из DSSP-файлов, для ее использования
	 * в задачах распознавания).
	 * 
	 * @see SequenceUtils#translationMap(String)
	 * @see SequenceUtils#translateStates(SequenceSet, Map, Map)
	 * 
	 * @param args
	 *    аргументы, приведенные выше
	 * @throws IOException
	 *    в случае ошибки чтения из файла выборки или записи в целевой файл
	 */
	@Task(id="set.tr",res=Messages.BUNDLE_NAME)
	public static void translate(String[] args) throws IOException {
		final String inputFile = args[0];
		final String outputFile = args[1];
		final String rawObservedTr = args[2];
		final String rawHiddenTr = (args.length > 3) ? args[3] : ":";
		
		Map<Character, Character> observedTr = SequenceUtils.translationMap(rawObservedTr);
		Map<Character, Character> hiddenTr = SequenceUtils.translationMap(rawHiddenTr);
		SequenceSet set = new GenericSequenceSet(inputFile, false);
		
		Env.debug(0, Messages.format("misc.dataset", set.repr()));
		Env.debug(0, "");
		
		SequenceSet trSet = SequenceUtils.translateStates(set, observedTr, hiddenTr);
		Env.debug(0, Messages.format("task.set.tr.out", trSet.repr()));
		Env.debug(0, Messages.getString("task.set.tr.save"));
		trSet.saveToFile(outputFile);
	}
}

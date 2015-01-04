package ua.kiev.icyb.bio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import ua.kiev.icyb.bio.res.Messages;

/**
 * Имплементация контейнера для хранения строк полных состояний, определяемая
 * интерфейсом {@link SequenceSet}.
 * 
 * <h3>Хранение данных</h3>
 * Для постоянного хранения наборов полных состояний используются текстовые файлы
 * в одном из двух форматов. Если задан алфавит полных состояний, используется
 * формат файла
 * <pre>
 * &lt;алфавит наблюдаемых состояний&gt; &lt;алфавит скрытых состояний&gt; &lt;алфавит полных состояний&gt;
 * c: &lt;первая строка полных состояний&gt;
 * c: &lt;вторая строка полных состояний&gt;
 * ...
 * c: &lt;последняя строка полных состояний&gt;
 * </pre>
 * Если алфавит полных состояний не задан, используется следующий формат:
 * <pre>
 * &lt;алфавит наблюдаемых состояний&gt; &lt;алфавит скрытых состояний&gt;
 * o: &lt;первая строка наблюдаемых состояний&gt;
 * h: &lt;первая строка скрытых состояний&gt;
 * o: &lt;вторая строка наблюдаемых состояний&gt;
 * h: &lt;вторая строка скрытых состояний&gt;
 * ...
 * </pre>
 * 
 * <p>Файл может быть сжат с помощью алгоритма GZIP; в этом случае он должен заканчиваться
 * расширением «.gz».
 */
public class GenericSequenceSet implements SequenceSet {
	
	private static final long serialVersionUID = 1L;
	
	private transient List<byte[]> hiddenSeq = new ArrayList<byte[]>();
	private transient List<byte[]> observedSeq = new ArrayList<byte[]>();
	private transient List<String> ids = new ArrayList<String>();

	/** Алфавит наблюдаемых состояний. */
	private String observedStates;
	/** Алфавит скрытых состояний. */
	private String hiddenStates;
	/** Алфавит полных состояний. */
	private String completeStates;
	
	/**
	 * Количество пар наблюдаемых и скрытых строк в выборке.
	 */
	private int length;
	
	/** Название выборки (выборок), которое использовалось для создания контейнера. */
	private String datasetName = null;
	
	/** 
	 * Базовая выборка, при фильтрации которой была получена эта выборка.
	 * Если выборка не была получена путем фильтрации, значение поля равно {@code null}. 
	 */
	private GenericSequenceSet unfilteredSet = null;
	
	/** 
	 * Селектор, которой использовался при создании выборки путем фильтрации 
	 * {@linkplain #unfilteredSet базовой выборки}.
	 * Если выборка не была получена путем фильтрации, значение поля равно {@code null}.
	 */
	private boolean[] selector = null;
	
	/**
	 * Последняя почитанная наблюдаемая строка.
	 */
	private transient byte[] lastObservedSeq;
	
	private transient String lastReadId;

	/**
	 * Создает новую пустую выборку.
	 * 
	 * @param observedStates
	 *    алфавит наблюдаемых состояний
	 * @param hiddenStates
	 *    алфавит скрытых состояний
	 * @param completeStates
	 *    алфавит полных состояний (может быть равен {@code null})
	 */
	public GenericSequenceSet(String observedStates, String hiddenStates, String completeStates) {
		this.observedStates = observedStates;
		this.hiddenStates = hiddenStates;
		this.completeStates = completeStates;
	}

	/**
	 * Загружает именованную выборку. Соответствие между именем выборки
	 * и файлом, в которой она хранится, осуществляется с помощью метода
	 * {@link IOUtils#resolveDataset(String)}. Если выборок несколько, их имена
	 * должны быть разделены запятой (например, <code>"human,elegans"</code>); 
	 * полученная выборка будет равна конкатенации этих
	 * выборок в порядке их указания.
	 * 
	 * @param datasetName
	 *        название одной или более выборок
	 * @throws IOException
	 *         если при чтении файлов выборок произошла ошибка ввода/вывода
	 * @throws IllegalArgumentException
	 *         если по крайней мере одно из указанных имен выборок не соответствует файлу 
	 */
	public GenericSequenceSet(String datasetName) throws IOException, IllegalArgumentException {
		read(datasetName);
	}
	
	/**
	 * Загружает именованную выборку или выборку из заданного файла.
	 * Если {@code resolve == true}, загружается именованная выборка; в противном
	 * случае происходит загрузка из файла.
	 * 
	 * @see #GenericSequenceSet(String)
	 * 
	 * @param name
	 *        название одной или более выборок или имя файла
	 * @param resolve
	 *        следует ли искать выборку среди именованных выборок
	 * @throws IOException
	 *         если при чтении файлов выборок произошла ошибка ввода/вывода
	 * @throws IllegalArgumentException
	 *         если по крайней мере одно из указанных имен выборок не соответствует файлу 
	 */
	public GenericSequenceSet(String name, boolean resolve) throws IOException {
		if (resolve) {
			read(name);
		} else {
			read(IOUtils.getReader(name));
		}
		this.length = hiddenSeq.size();
	}
	
	/**
	 * Выполняет чтение указанной именованной выборки/выборок.
	 * 
	 * @param datasetName
	 *        название одной или более выборок, разделенных запятыми
	 * @throws IOException
	 */
	private void read(String datasetName) throws IOException {
		this.datasetName = datasetName;
		String[] datasets = datasetName.split(",");
		String filename = IOUtils.resolveDataset(datasets[0]);
		if (filename == null) {
			throw new IOException(Messages.format("dataset.e_name", datasets[0]));
		}
		read(IOUtils.getReader(filename));
		
		for (int i = 1; i < datasets.length; i++) {
			SequenceSet part = new GenericSequenceSet(datasets[i]);
			this.addSet(part);
		}
	}

	/**
	 * Выполняет чтение строк выборки из текстового файла. 
	 * 
	 * @param reader
	 *    автомат для считывания строк выборки
	 * @throws IOException
	 */
	private void read(BufferedReader reader) throws IOException {
		// Read the header of the file
		String line = reader.readLine();
		String[] parts = line.split("\\s+");
		observedStates = parts[0];
		hiddenStates = parts[1];

		if (parts.length == 2) {
			completeStates = null;
		} else {
			completeStates = parts[2];
		}

		while ((line = reader.readLine()) != null) {
			processLine(line);
		}
		((ArrayList<byte[]>) observedSeq).trimToSize();
		((ArrayList<byte[]>) hiddenSeq).trimToSize();
		((ArrayList<String>) ids).trimToSize();
	}

	/**
	 * Обрабатывает отдельную строку текстового файла, содержащего выборку.
	 * 
	 * @param line
	 *    строка, которую надо обработать
	 * @throws IOException
	 */
	private void processLine(String line) throws IOException {
		String key = line.substring(0, line.indexOf(':')).trim();
		String value = line.substring(line.indexOf(':') + 1).trim();

		if (key.equals("o")) {
			lastObservedSeq = decode(value, observedStates);
		} else if (key.equals("i")) {
			lastReadId = value;
		} else if (key.equals("h")) {
			this.add(lastObservedSeq, decode(value, hiddenStates), lastReadId);
		} else if (key.equals("c")) {
			byte[] complete = decode(value, completeStates);
			byte[] observed = new byte[complete.length], hidden = new byte[complete.length];

			for (int pos = 0; pos < complete.length; pos++) {
				observed[pos] = (byte) (complete[pos] % observedStates.length());
				hidden[pos] = (byte) (complete[pos] / observedStates.length());
			}
			
			this.add(observed, hidden, lastReadId);
		}
	}

	/**
	 * Конвертирует строку символов из определенного алфавита в байтовый массив,
	 * каждый элемент которого равен индексу соответствующего символа строки в алфавите.
	 * 
	 * @param line
	 *    строка, которая подвергается преобразованию
	 * @param alphabet
	 *    испольуемый алфавит символов
	 * @return
	 *    байтовый массив индексов
	 * @throws IOException
	 */
	private byte[] decode(String line, String alphabet) throws IOException {
		byte[] decoded = new byte[line.length()];
		for (int pos = 0; pos < line.length(); pos++) {
			decoded[pos] = (byte) alphabet.indexOf(line.charAt(pos));
			if (decoded[pos] < 0) {
				throw new IOException(Messages.format("dataset.e_char", line.charAt(pos)));
			}
		}
		return decoded;
	}

	@Override
	public int length() {
		return this.length;
	}

	@Override
	public byte[] observed(int index) {
		return observedSeq.get(index);
	}

	@Override
	public byte[] hidden(int index) {
		return hiddenSeq.get(index);
	}
	
	@Override
	public String id(int index) {
		return ids.get(index);
	}

	@Override
	public String observedStates() {
		return observedStates;
	}

	@Override
	public String hiddenStates() {
		return hiddenStates;
	}
	
	@Override
	public String completeStates() {
		return completeStates;
	}

	public SequenceSet filter(boolean[] selector) {
		GenericSequenceSet filtered = new GenericSequenceSet(
				observedStates, hiddenStates, completeStates);
		filtered.unfilteredSet = this;
		filtered.selector = selector.clone();

		for (int i = 0; i < selector.length; i++)
			if (selector[i]) {
				filtered.add(observed(i), hidden(i), id(i));
			}
		return filtered;
	}

	public SequenceSet filter(Filter filter) {
		GenericSequenceSet filtered = new GenericSequenceSet(
				observedStates, hiddenStates, completeStates);
		filtered.unfilteredSet = this;
		filtered.selector = new boolean[length()];

		for (int i = 0; i < length(); i++)
			if (filter.pass(i, observed(i), hidden(i))) {
				filtered.add(observed(i), hidden(i), id(i));
				filtered.selector[i] = true;
			}
		return filtered;
	}

	@Override
	public void saveToFile(String fileName) throws IOException {
		saveToFile(IOUtils.getWriter(fileName));
	}

	/**
	 * Сохраняет набор последовательностей в текстовый поток.
	 * 
	 * @param writer
	 * @throws IOException
	 */
	private void saveToFile(BufferedWriter writer) throws IOException {
		writer.write(observedStates + " " + hiddenStates);
		if (completeStates != null)
			writer.write(" " + completeStates);
		writer.write("\n");

		StringBuilder builder = new StringBuilder();
		byte[] seq, hidden;

		for (int i = 0; i < length(); i++) {
			writer.write("i: " + id(i) + "\n");
			builder.setLength(0);
			if (completeStates == null) {
				seq = observed(i);
				for (int pos = 0; pos < seq.length; pos++)
					builder.append((char) observedStates.charAt(seq[pos]));
				writer.write("o: " + builder + "\n");

				builder.setLength(0);
				seq = hidden(i);
				for (int pos = 0; pos < seq.length; pos++)
					builder.append((char) hiddenStates.charAt(seq[pos]));
				writer.write("h: " + builder + "\n");
			} else {
				seq = observed(i);
				hidden = hidden(i);
				for (int pos = 0; pos < seq.length; pos++) {
					builder.append((char) completeStates.charAt(seq[pos] + observedStates.length()
							* hidden[pos]));
				}
				writer.write("c: " + builder + "\n");
			}
		}
		writer.close();
	}

	/**
	 * Добавляет в коллекцию пару из наблюдаемой и соответстующей скрытой 
	 * последовательности состояний.
	 * 
	 * @param observed
	 *        строка наблюдаемых состояний
	 * @param hidden
	 *        строка скрытых состояний
	 */
	public void add(byte[] observed, byte[] hidden, String id) {
		if (observed.length != hidden.length) {
			throw new IllegalArgumentException(Messages.getString("dataset.e_length"));
		}
		this.observedSeq.add(observed);
		this.hiddenSeq.add(hidden);
		this.ids.add(id);
		this.length = this.hiddenSeq.size();
	}

	/**
	 * Добавляет все строки из другой выборки в эту выборку. 
	 * 
	 * @param set
	 *        коллекция последовательностей, которые надо добавить в эту выборку
	 */
	public void addSet(SequenceSet set) {
		if (!set.observedStates().equals(observedStates())) {
			throw new IllegalArgumentException(Messages.getString("dataset.e_states"));
		}
		if (!set.hiddenStates().equals(hiddenStates())) {
			throw new IllegalArgumentException(Messages.getString("dataset.e_states"));
		}

		for (int i = 0; i < set.length(); i++) {
			this.add(set.observed(i), set.hidden(i), set.id(i));
		}
	}

	/**
	 * Сериализация набора последовательностей осуществляется согласно следующим правилам:
	 * <ol>
	 * <li>если набор был создан с помощью конструктора {@link #GenericSequenceSet(String)},
	 * сохраняется строка — аргумент этого конструктора;
	 * <li>в противном случае, если набор получен в результате фильтрации другого набора <code>src</code>,
	 * сохраняется исходный набор <code>src</code> и массив булевых величин, 
	 * характеризующий вхождение строк из него в отфильтрованный набор. 
	 * </ol>
	 * 
	 * @param in
	 *    поток, из которого считывается объект
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		hiddenSeq = new ArrayList<byte[]>(this.length());
		observedSeq = new ArrayList<byte[]>(this.length());
		ids = new ArrayList<String>(this.length());
		
		if (datasetName != null) {
			try {
				read(datasetName);
			} catch (IOException e) {
				if (Env.debugLevel() >= 0) {
					e.printStackTrace();
				}
				Env.debug(0, Messages.getString("dataset.default"));
				
				hiddenSeq.clear();
				observedSeq.clear();
				ids.clear();
				
				for (int i = 0; i < length(); i++) {
					hiddenSeq.add(new byte[0]);
					observedSeq.add(new byte[0]);
					ids.add("" + i);
				}
			}
		} else if (unfilteredSet != null) {
			GenericSequenceSet filtered = (GenericSequenceSet)unfilteredSet.filter(selector);
			this.hiddenSeq = filtered.hiddenSeq;
			this.observedSeq = filtered.observedSeq;
			this.ids = filtered.ids;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>В представление входит имя выборки (если есть), количество строк и алфавиты
	 * наблюдаемых и скрытых состояний.
	 */
	public String repr() {
		GenericSequenceSet set = this;
		while ((set.datasetName == null) && (set.unfilteredSet != null)) {
			set = set.unfilteredSet;
		}
		int totalLength = 0;
		for (int i = 0; i < this.length(); i++) {
			totalLength += observed(i).length;
		}
		
		String repr = "";
		if (set.datasetName != null) {
			repr += Messages.format("dataset.name", set.datasetName) + "\n";
		}
		repr += Messages.format("dataset.repr", 
				length(), observedStates(), hiddenStates()) + "\n";
		repr += Messages.format("dataset.seq_len", totalLength, 1.0 * totalLength / length());
		return repr;
	}
	
	@Override
	public String toString() {
		return Messages.format("dataset.str", length(), 
				observedStates(), hiddenStates());
	}
}

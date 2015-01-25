package ua.kiev.icyb.bio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
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
public class SimpleSequenceSet extends AbstractCollection<Sequence> implements SequenceSet {
	
	private static final long serialVersionUID = 1L;
	
	private List<byte[]> hiddenSeq = new ArrayList<byte[]>();
	private List<byte[]> observedSeq = new ArrayList<byte[]>();
	private List<String> ids = new ArrayList<String>();

	/** Алфавит наблюдаемых состояний. */
	private String observedStates;
	/** Алфавит скрытых состояний. */
	private String hiddenStates;
	/** Алфавит полных состояний. */
	private String completeStates;
	
	/**
	 * Следует ли записывать содержимое выборки при сериализации.
	 */
	protected boolean writeContent = true;

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
	public SimpleSequenceSet(String observedStates, String hiddenStates, String completeStates) {
		this.observedStates = observedStates;
		this.hiddenStates = hiddenStates;
		this.completeStates = completeStates;
	}
	
	/**
	 * Загружает выборку из текстового потока данных.
	 * 
	 * @param reader
	 *    поток данных, из которого читается информация о строках выборки
	 * @throws IOException
	 *    если при чтении файла выборки произошла ошибка ввода/вывода 
	 */
	public SimpleSequenceSet(BufferedReader reader) throws IOException {
		read(reader);
	}

	
	protected SimpleSequenceSet(SequenceSet other) {
		this(other.observedStates(), other.hiddenStates(), other.completeStates());
		this.addSet(other);
	}
	
	/**
	 * Создает отстутствующие идентификаторы для строк выборки. Идентификатор состоит из
	 * названия выборки, двоеточия и порядкового номера строки в выборке.
	 */
	protected void fillMissingIds(String prefix) {
		for (int i = 0; i < this.size(); i++) {
			if (this.ids.get(i) == null) {
				this.ids.set(i, prefix + i);
			}
		}
	}

	/**
	 * Выполняет чтение строк выборки из текстового файла. 
	 * 
	 * @param reader
	 *    автомат для считывания строк выборки
	 * @throws IOException
	 */
	protected void read(BufferedReader reader) throws IOException {
		// Заголовок файла
		String line = reader.readLine();
		String[] parts = line.split("\\s+");
		observedStates = parts[0];
		hiddenStates = parts[1];

		if (parts.length == 2) {
			completeStates = null;
		} else {
			completeStates = parts[2];
		}

		byte[] lastObservedSeq = null;
		String lastReadId = null;
		
		while ((line = reader.readLine()) != null) {
			String key = line.substring(0, line.indexOf(':')).trim();
			String value = line.substring(line.indexOf(':') + 1).trim();

			if (key.equals("o")) {
				lastObservedSeq = decode(value, observedStates);
			} else if (key.equals("i")) {
				lastReadId = value;
			} else if (key.equals("h")) {
				this.doAdd(new Sequence(
						lastReadId, lastObservedSeq, decode(value, hiddenStates)));
				lastReadId = null;
			} else if (key.equals("c")) {
				byte[] complete = decode(value, completeStates);
				byte[] observed = new byte[complete.length], hidden = new byte[complete.length];

				for (int pos = 0; pos < complete.length; pos++) {
					observed[pos] = (byte) (complete[pos] % observedStates.length());
					hidden[pos] = (byte) (complete[pos] / observedStates.length());
				}
				
				this.doAdd(new Sequence(lastReadId, observed, hidden));
				lastReadId = null;
			}
		}
		((ArrayList<byte[]>) observedSeq).trimToSize();
		((ArrayList<byte[]>) hiddenSeq).trimToSize();
		((ArrayList<String>) ids).trimToSize();
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
	public int size() {
		return this.ids.size();
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
	
	public Sequence get(int index) {
		return new Sequence(this, index, id(index), observed(index), hidden(index));
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
	
	@Override
	public SequenceSet join(SequenceSet other, SequenceSet... more) {
		SimpleSequenceSet union = new SimpleSequenceSet(
				observedStates, hiddenStates, completeStates);

		union.addSet(this);
		union.addSet(other);
		for (SequenceSet set : more) {
			union.addSet(set);
		}
		return union;
	}

	public SequenceSet filter(boolean[] selector) {
		SimpleSequenceSet filtered = new SimpleSequenceSet(
				observedStates, hiddenStates, completeStates);

		for (int i = 0; i < selector.length; i++)
			if (selector[i]) {
				filtered.doAdd(this.get(i));
			}
		return filtered;
	}

	public SequenceSet filter(Filter filter) {
		boolean[] selector = new boolean[this.size()];
		
		for (int i = 0; i < size(); i++)
			selector[i] = filter.eval( this.get(i) );
		return this.filter(selector);
	}

	@Override
	public void saveToFile(String fileName) throws IOException {
		saveToFile(Env.getWriter(fileName));
	}

	/**
	 * Сохраняет набор последовательностей в текстовый поток.
	 * 
	 * @param writer
	 *    поток для записи
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	private void saveToFile(BufferedWriter writer) throws IOException {
		writer.write(observedStates + " " + hiddenStates);
		if (completeStates != null)
			writer.write(" " + completeStates);
		writer.write("\n");

		StringBuilder builder = new StringBuilder();
		byte[] seq, hidden;

		for (int i = 0; i < size(); i++) {
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
	 * @param sequence
	 *    добавляемый объект
	 */
	protected void doAdd(Sequence sequence) {
		this.observedSeq.add(sequence.observed);
		this.hiddenSeq.add(sequence.hidden);
		this.ids.add(sequence.id);
	}

	/**
	 * Добавляет все строки из другой выборки в эту выборку. 
	 * 
	 * @param set
	 *        коллекция последовательностей, которые надо добавить в эту выборку
	 */
	protected void addSet(SequenceSet set) {
		if (!set.observedStates().equals(observedStates())) {
			throw new IllegalArgumentException(Messages.getString("dataset.e_states"));
		}
		if (!set.hiddenStates().equals(hiddenStates())) {
			throw new IllegalArgumentException(Messages.getString("dataset.e_states"));
		}

		for (int i = 0; i < set.size(); i++) {
			this.doAdd(set.get(i));
		}
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		if (!this.writeContent) {
			this.hiddenSeq = new ArrayList<byte[]>();
			this.observedSeq = new ArrayList<byte[]>();
			this.ids = new ArrayList<String>();
		}
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		List<byte[]> o = this.observedSeq, h = this.hiddenSeq;
		List<String> ids = this.ids;
		
		if (!this.writeContent) {
			this.observedSeq = null;
			this.hiddenSeq = null;
			this.ids = null;
		}
		
		out.defaultWriteObject();
		
		this.observedSeq = o;
		this.hiddenSeq = h;
		this.ids = ids;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>В представление входит имя выборки (если есть), количество строк и алфавиты
	 * наблюдаемых и скрытых состояний.
	 */
	public String repr() {
		int totalLength = 0;
		for (int i = 0; i < this.size(); i++) {
			totalLength += observed(i).length;
		}
		
		String repr = "";
		repr += Messages.format("dataset.repr", 
				size(), observedStates(), hiddenStates()) + "\n";
		repr += Messages.format("dataset.seq_len", totalLength, 1.0 * totalLength / size());
		return repr;
	}
	
	@Override
	public String toString() {
		return Messages.format("dataset.str", size(), 
				observedStates(), hiddenStates());
	}

	private static class SetIterator implements Iterator<Sequence> {

		private final SimpleSequenceSet set;
		
		private int index;
		
		public SetIterator(SimpleSequenceSet set) {
			this.set = set;
			this.index = 0;
		}
		
		@Override
		public boolean hasNext() {
			return (index < set.size());
		}

		@Override
		public Sequence next() {
			return this.set.get(this.index++);
		}

		@Override
		public void remove() {
			this.set.remove(this.index - 1);
		}
		
	}
	
	@Override
	public Iterator<Sequence> iterator() {
		return new SetIterator(this);
	}
	
	@Override
	public void clear() {
		this.observedSeq.clear();
		this.hiddenSeq.clear();
		this.ids.clear();
	}
	
	/**
	 * Добавляет в коллекцию пару из наблюдаемой и соответстующей скрытой 
	 * последовательности состояний.
	 * 
	 * @param sequence
	 *    добавляемый объект
	 */
	@Override
	public boolean add(Sequence sequence) {
		this.doAdd(sequence);
		return true;
	}
	
	/**
	 * Удаляет из коллекции прецедент с заданным индексом.
	 * 
	 * @param index
	 *    индекс прецедента, который надо удалить
	 */
	public Sequence remove(int index) {
		Sequence sequence = this.get(index);
		
		this.observedSeq.remove(index);
		this.hiddenSeq.remove(index);
		this.ids.remove(index);
		
		return sequence;
	}
}

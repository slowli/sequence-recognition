package ua.kiev.icyb.bio;

import java.io.IOException;
import java.io.ObjectInputStream;

import ua.kiev.icyb.bio.res.Messages;

/**
 * Именованная выборка данных.
 */
public class NamedSequenceSet extends SimpleSequenceSet {

	private static final long serialVersionUID = 1L;

	/** Название выборки (выборок), которое использовалось для создания контейнера. */
	private String datasetName = null;
	
	/** 
	 * Базовая выборка, при фильтрации которой была получена эта выборка.
	 * Если выборка не была получена путем фильтрации, значение поля равно {@code null}. 
	 */
	private NamedSequenceSet unfilteredSet = null;
	
	/** 
	 * Селектор, которой использовался при создании выборки путем фильтрации 
	 * {@linkplain #unfilteredSet базовой выборки}.
	 * Если выборка не была получена путем фильтрации, значение поля равно {@code null}.
	 */
	private boolean[] selector = null;
	
	/**
	 * Части, из которых составлена эта выборка.
	 */
	private SequenceSet[] setParts = null;
	
	/**
	 * Загружает именованную выборку. Соответствие между именем выборки
	 * и файлом, в которой она хранится, осуществляется с помощью метода
	 * {@link IOUtils#resolveDataset(String)}.
	 * 
	 * @param datasetName
	 *    название выборки
	 * @throws IOException
	 *    если при чтении файла выборки произошла ошибка ввода/вывода
	 * @throws IllegalArgumentException
	 *    если указанное имя выборки не соответствует файлу 
	 */
	public NamedSequenceSet(String datasetName, Env env) throws IOException {
		super(env.resolveDataset(datasetName));
		this.datasetName = datasetName;
		fillMissingIds(this.datasetName + ":");
	}
	
	/**
	 * Создает обертку вокруг выборки.
	 * 
	 * @param other
	 *    выборка данных, для которой создается обертка
	 */
	protected NamedSequenceSet(SequenceSet other) {
		super(other);
	}
	
	@Override
	public SequenceSet join(SequenceSet other, SequenceSet... more) {
		NamedSequenceSet union = new NamedSequenceSet(super.join(other, more));
		
		union.setParts = new SequenceSet[2 + more.length];
		union.setParts[0] = this;
		union.setParts[1] = other;
		for (int i = 0; i < more.length; i++) {
			union.setParts[2 + i] = more[i];
		}
		
		return union;
	}
	
	@Override
	public SequenceSet filter(boolean[] selector) {
		NamedSequenceSet filtered = new NamedSequenceSet(super.filter(selector));
		filtered.unfilteredSet = this;
		filtered.selector = selector.clone();
		return filtered;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>В представление входит имя выборки (если есть), количество строк и алфавиты
	 * наблюдаемых и скрытых состояний.
	 */
	@Override
	public String repr() {
		NamedSequenceSet set = this;
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
		String args = "";
		if (this.datasetName != null) {
			args = "'" + this.datasetName + "'";
		} else if (this.unfilteredSet != null) {
			args = this.unfilteredSet + "," + this.length();
		} else if (this.setParts != null) {
			args += this.setParts[0];
			for (int i = 1; i < this.setParts.length; i++) {
				args += "," + this.setParts[i];
			}
		}
		return "set(" + args + ")";
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
		Env env = ((Env.ObjInputStream) in).env;
		
		if (datasetName != null) {
			try {
				addSet(env.loadSet(datasetName));
			} catch (IOException e) {
				// TODO do smth
			}
		} else if (unfilteredSet != null) {
			SequenceSet filtered = unfilteredSet.filter(selector);
			this.addSet(filtered);
		} else if (setParts != null) {
			for (SequenceSet part : setParts) {
				this.addSet(part);
			}
		}
	}
}

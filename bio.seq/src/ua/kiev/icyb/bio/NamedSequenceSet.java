package ua.kiev.icyb.bio;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;

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
	 * Преобразование, которое использовалось при создании выборки путем трансформации.
	 * Если выборка не была получена преобразованием, значение поля равно {@code null}.
	 */
	private Transform transform;
	
	/**
	 * Части, из которых составлена эта выборка.
	 */
	private SequenceSet[] setParts = null;
	
	/**
	 * Загружает именованную выборку. Соответствие между именем выборки
	 * и файлом, в которой она хранится, осуществляется с помощью метода
	 * {@link Env#resolveDataset(String)}.
	 * 
	 * @param datasetName
	 *    название выборки
	 * @throws IOException
	 *    если при чтении файла выборки произошла ошибка ввода/вывода
	 * @throws IllegalArgumentException
	 *    если указанное имя выборки не соответствует файлу 
	 */
	public NamedSequenceSet(String datasetName, Env env) throws IOException {
		super("", "", null);
		this.writeContent = false;
		this.datasetName = datasetName;
		read(env.resolveDataset(datasetName));
	}
	
	@Override
	protected String autoID() {
		return this.datasetName + ":" + this.size();
	}
	
	/**
	 * Создает обертку вокруг выборки.
	 * 
	 * @param other
	 *    выборка данных, для которой создается обертка
	 */
	protected NamedSequenceSet(SequenceSet other) {
		super(other);
		this.writeContent = false;
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
	
	@Override
	public SequenceSet transform(Transform transform) {
		NamedSequenceSet transformed = new NamedSequenceSet(super.transform(transform));
		transformed.unfilteredSet = this;
		transformed.transform = transform;
		return transformed;
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
		for (int i = 0; i < this.size(); i++) {
			totalLength += observed(i).length;
		}
		
		String repr = "";
		if (set.datasetName != null) {
			repr += Messages.format("dataset.name", set.datasetName) + "\n";
		}
		repr += Messages.format("dataset.repr", 
				size(), observedStates(), hiddenStates()) + "\n";
		repr += Messages.format("dataset.seq_len", totalLength, 1.0 * totalLength / size());
		return repr;
	}
	
	@Override
	public String toString() {
		String args = "";
		if (this.datasetName != null) {
			args = "'" + this.datasetName + "'";
		} else if (this.selector != null) {
			args = this.unfilteredSet + "[" + this.size() + "]";
		} else if (this.transform != null) {
			args = this.unfilteredSet + ">" + this.transform;
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
	 * <li>Если набор был создан с помощью конструктора {@link #NamedSequenceSet(String, Env)},
	 * сохраняется строка — аргумент этого конструктора.
	 * <li>Если набор получен в результате фильтрации другого набора <code>src</code>,
	 * сохраняется исходный набор <code>src</code> и массив булевых величин, 
	 * характеризующий вхождение строк из него в отфильтрованный набор.
	 * <li>Если набор получен слиянием нескольких наборов, сохраняется последовательность этих наборов.
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
		} else if (selector != null) {
			SequenceSet filtered = unfilteredSet.filter(selector);
			this.addSet(filtered);
		} else if (transform != null) {
			SequenceSet transformed = unfilteredSet.transform(transform);
			this.addSet(transformed);
		} else if (setParts != null) {
			for (SequenceSet part : setParts) {
				this.addSet(part);
			}
		}
	}
	
	@Override
	public boolean add(Sequence sequence) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Sequence remove(int index) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends Sequence> c) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
}

package ua.kiev.icyb.bio.alg.tree;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.AbstractSeqAlgorithm;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Алгоритм распознавания, применяющий для распознавания скрытой последовательности
 * строки наблюдаемых состояний один из составляющих алгоритмов, выбираемый в зависимости
 * от свойств этой строки.
 */
public class SwitchAlgorithm extends AbstractSeqAlgorithm {

	/**
	 * Класс, представляющий последовательность состояний со сравнением по содержимому.
	 */
	private static class Sequence {

		private final byte[] seq;
		
		/**
		 * Создает оболочку для последовательности состояний.
		 * 
		 * @param seq
		 *    массив, представляющий последовательность состояний
		 */
		public Sequence(byte[] seq) {
			this.seq = seq;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(seq);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass())
				return false;
			Sequence other = (Sequence) obj;
			if (!Arrays.equals(seq, other.seq))
				return false;
			return true;
		}
	}
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Алгоритмы, использующиеся для распознавания. 
	 */
	private SeqAlgorithm algorithms[];

	/** 
	 * Соответствие между строками наблюдаемых состояний и составляющими алгоритмами,
	 * которые обрабатывают эти строки.
	 */
	private transient Map<Sequence, Integer> classes = new HashMap<Sequence, Integer>();
	
	/**
	 * Выборка, использованная при создании класса.
	 */
	private SequenceSet set = null;
	
	/**
	 * Индексы составляющих алгоритмов, которые обрабатывают каждую строку наблюдаемых
	 * состояний из {@link #set}.
	 */
	private byte[] classMarkers = null; 
	
	/**
	 * Создает новый алгоритм с заданными составляющими. Логика соответствия между 
	 * составляющими алгоритмами и строками наблюдаемых состояний не определяется;
	 * подклассы, использующие этот конструктор, должны позаботиться об этом.
	 * 
	 * @param algs
	 *    массив составляющих алгоритмов
	 */
	protected SwitchAlgorithm(SeqAlgorithm[] algs) {
		this.algorithms = algs.clone();
	}
	
	/**
	 * Создает новый алгоритм с априорно заданным распределением областей компетентности
	 * на конечном множестве прецедентов. При попытке использовать алгоритм на 
	 * строке наблюдаемых состояний, не входящей в прецеденты, будет вызвано исключение.
	 * 
	 * @param classes
	 *    индекс компетентного алгоритма для каждого прецедента
	 * @param set
	 *    набор прецедентов, который будет далее использоваться с этим алгоритмом
	 * @param algs
	 *    составляющие алгоритмы распознавания
	 */
	public SwitchAlgorithm(byte[] classes, SequenceSet set, SeqAlgorithm[] algs) {
		this(algs);

		assert(classes.length == set.length());
		for (int i = 0; i < classes.length; i++) {
			putIndex(set.observed(i), classes[i]);
		}

		this.set = set;
		this.classMarkers = classes;
	}
	
	
	/**
	 * Возвращает индекс компетентного составляющего алгоритма для заданной последовательности
	 * наблюдаемых состояний.
	 * 
	 * <p>Имплементация по умолчанию ищет строку состояний (по содержимому) в выборке,
	 * предоставленной с  {@linkplain #SwitchAlgorithm(byte[], SequenceSet, SeqAlgorithm[]) публичным конструктором}
	 * и возвращает соответствующий индекс алгоритма. Если строка не содержится в выборке,
	 * вызывается исключение времени исполнения.
	 * 
	 * <p>Подклассы должны переопределять метод, если они не используют упомянутый выше конструктор.
	 * 
	 * @param sequence 
	 *    последовательность наблюдаемых состояний
	 * @return 
	 *    индекс (с отсчетом от нуля) компетентного составляющего алгоритма
	 */
	public int index(byte[] sequence) {
		Integer idx = classes.get(new Sequence(sequence));
		if (idx == null) {
			throw new RuntimeException("Sequence is not in the training set!");
		}
		return idx;
	}
	
	/**
	 * Сохраняет индекс компетентного алгоритма для заданной последовательности
	 * состояний.
	 * 
	 * @param sequence 
	 *    последовательность наблюдаемых состояний
	 * @param index 
	 *    индекс (с отсчетом от нуля) компетентного составляющего алгоритма
	 */
	protected void putIndex(byte[] sequence, int index) {
		classes.put(new Sequence(sequence), index);
	}
	
	@Override
	public void train(byte[] observed, byte[] hidden) {
		algorithms[index(observed)].train(observed, hidden);
	}

	@Override
	public void reset() {
		for (SeqAlgorithm model: algorithms)
			model.reset();
	}

	@Override
	public byte[] run(byte[] sequence) {
		return algorithms[index(sequence)].run(sequence);
	}
	
	@Override
	public String toString() {
		return String.format("%d x %s", algorithms.length, algorithms[0]);
	}

	@Override
	public Object clearClone() {
		SwitchAlgorithm other = (SwitchAlgorithm) super.clearClone();
		other.algorithms = algorithms.clone();
		for (int i = 0; i < algorithms.length; i++) {
			other.algorithms[i] = (SeqAlgorithm) algorithms[i].clearClone();
		}
		other.classes = new HashMap<Sequence, Integer>();
		other.classes.putAll(classes);
		return other;
	}
	
	/**
	 * Восстанавливает поля объекта, которые не записываются в поток, на основе сохраненных полей.
	 * 
	 * @param stream
	 *    поток для считывания объекта
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream stream) 
			throws IOException, ClassNotFoundException {
		
		stream.defaultReadObject();
		classes = new HashMap<Sequence, Integer>();
		if (classMarkers != null) {
			assert(classMarkers.length == set.length());
			for (int i = 0; i < classMarkers.length; i++) {
				putIndex(set.observed(i), classMarkers[i]);
			}
		}
	}
	
	@Override
	public String repr() {
		String repr = super.repr() + "\n";
		repr += Messages.format("alg.algs_n", algorithms.length) + "\n";
		repr += Messages.format("alg.base", algorithms[0].repr());
		return repr;
	}
}

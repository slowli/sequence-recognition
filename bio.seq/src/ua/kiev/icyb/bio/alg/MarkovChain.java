package ua.kiev.icyb.bio.alg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.Trainable;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Обобщение марковских цепей произвольного порядка для вероятностных моделей,
 * в которых существуют как наблюдаемые, так и скрытые состояния.
 * 
 * <p>Модель, используемая классом, определяет вероятностное распределение на пространстве
 * строк полных состояний. Функция правдоподобия при этом состоит из трех частей, задаваемых
 * вероятностными распределениями:
 * <ul>
 * <li>распределение строк по длинам;
 * <li>распределение начальных фрагментов строк определенной длины <code>l</code>;
 * <li>условное распределение переходов из строки длины <code>l</code> в строки длины
 * <code>m</code>.
 * </ul>
 * Длина <code>l</code> называется <em>порядком</em> модели, <code>m</code> — длиной зависимой
 * цепочки состояний. При <code>m = 1</code> модель соответствует марковской цепи
 * <code>l</code>-го порядка на пространстве строк полных состояний.
 * 
 * <p>Класс содержит две основных группы методов: 
 * <ol>
 * <li>обучение параметров вероятностной модели;
 * <li>вычисление этих параметров.
 * </ol>
 * Параметры модели, возвращаемые методами второй группы, 
 * являются решением задачи максимизации совместного (взвешенного) правдоподобия
 * для набора строк полных состояний, которые были перед этим переданы методам первой группы.
 */
public class MarkovChain implements Serializable, Trainable, Representable {
	
	private static final long serialVersionUID = 1L;
	
	/** Длина зависимой цепочки состояний. */
	private int depLength;
	/** Порядок цепи Маркова. */
	protected int order;
	
	/** Алфавит наблюдаемых состояний. */
	private String observedStates;
	/** Алфавит скрытых состояний. */
	private String hiddenStates;
	/** Фабрика для работы с фрагментами цепочек состояний. */
	protected transient FragmentFactory factory;
	
	/** 
	 * Количество последовательностей, которые были использованы для обучения параметров
	 * вероятностной модели.
	 */
	protected int nSequences;
	
	/** Статистика по начальным состояниям цепочек полных состояний. */
	private Map<Fragment, Double> initial;
	
	/** Количество возможных различных зависимых цепочек состояний. */
	private transient int headsCount;
	
	/**
	 * Статистика по переходам из цепочек полных состояний длины, определяемой порядком
	 * марковской цепи, в цепочки длины зависимой части. Последовательности
	 * длины {@link #order} в таблице соответствует массив величин, каждая из которых равна взвешенному
	 * числу переходов из этой последовательности в одно из возможных зависимых состояний.
	 * Зависимые состояния упорядочены в алфавитном порядке, определяемом
	 * методом {@link FragmentFactory#getTotalIndex(Fragment)}.
	 * Последний элемент массива равен сумме остальных элементов (предназначен для ускорения
	 * вычислений).
	 */
	protected Map<Fragment, float[]> transitions;
	
	/** Вероятностное распределение строк по длинам. */
	protected Distribution lengthDistr;
	
	/**
	 * Создает марковскую цепь с заданными параметрами.
	 * 
	 * @param depLength
	 *    длина зависимой цепочки состояний 
	 * @param order
	 *    порядок цепочки
	 * @param observedStates
	 *    алфавит наблюдаемых состояний
	 * @param hiddenStates
	 *    алфавит скрытых состояний
	 */
	public MarkovChain(int depLength, int order, String observedStates, String hiddenStates) {
		this.depLength = depLength;
		this.order = order;
		this.observedStates = observedStates;
		this.hiddenStates = hiddenStates;
		initialize();
	}
	
	/**
	 * Копирует параметры вероятностной модели из другой марковской цепи.
	 * 
	 * @param other
	 */
	protected MarkovChain(MarkovChain other) {
		this(other.depLength, other.order, other.observedStates, other.hiddenStates);
	}
	
	/**
	 * Возвращает алфавит наблюдаемых состояний, используемый в этой вероятностной модели.
	 * 
	 * @return 
	 *    строка, каждый символ которой уникален и обозначает одно из наблюдаемых состояний
	 */
	public String observedStates() { 
		return observedStates; 
	}

	/**
	 * Возвращает алфавит скрытых состояний, используемый в этой вероятностной модели.
	 * 
	 * @return 
	 *    строка, каждый символ которой уникален и обозначает одно из скрытых состояний
	 */
	public String hiddenStates() { 
		return hiddenStates; 
	}

	/**
	 * Возвращает порядок марковской цепи, т.е. количество предшествующих полных состояний,
	 * от которых зависит вероятность вхождения определенного полного состояния 
	 * в цепочку состояний.
	 * 
	 * @return
	 *    порядок марковской цепи 
	 */
	public int order() { 
		return order; 
	}

	/**
	 * Возвращает длину зависимой цепочки состояний.
	 * 
	 * @return
	 *    длина зависимой цепочки состояний
	 */
	public int depLength() {
		return depLength;
	}
	
	/**
	 * Возвращает фабрику для работы с фрагментами цепочек состояний.
	 *   
	 * @return
	 *    фабрика фрагментов
	 */
	public FragmentFactory factory() {
		return factory;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		try {
			MarkovChain other = (MarkovChain) super.clone();
			other.initial = (Map<Fragment, Double>) 
					((HashMap<Fragment, Double>) this.initial).clone();
			other.transitions = (Map<Fragment, float[]>) 
					((HashMap<Fragment, float[]>) this.transitions).clone();
			return other;
		} catch (CloneNotSupportedException e) {
			// Should never happen
			return null;
		}
	}
	
	public Object clearClone() {
		try {
			MarkovChain other = (MarkovChain) super.clone();
			other.initialize();
			return other;
		} catch (CloneNotSupportedException e) {
			// Should never happen
			return null;
		}
	}
	
	/**
	 * Выполняет инициализацию большинства полей класса.
	 */
	protected void initialize() {
		headsCount = 1;
		for (int i = 0; i < depLength; i++)
			headsCount *= (observedStates.length() * hiddenStates.length());
		
		factory = new FragmentFactory(observedStates, hiddenStates, order + depLength);
		initial = new HashMap<Fragment, Double>();
		nSequences = 0;
		transitions = new HashMap<Fragment, float[]>();
		
		lengthDistr = new EmpiricalDistribution(20000, 100, 1e-7);
	}
	
	/**
	 * Возвращает условную вероятность того, что определенная последовательность полных состояний
	 * является началом цепочки. Если среди в процессе обучения не было
	 * строк, начинающихся с заданной последовательности, возвращается <code>0</code>.
	 * 
	 * @param state
	 *    фрагмент, для которого требуется вычислить начальную вероятность
	 * @return
	 *    начальная вероятность для фрагмента
	 */
	public double getInitialP(Fragment state) {
		Double count = initial.get(state);
		return (count == null) ? 0 : 1.0 * count / nSequences;
	}
	
	/**
	 * Обновляет статистику по начальным состояниям.
	 * 
	 * @param state
	 *    начальный фрагмент цепочки полных состояний
	 * @param weight
	 *    неотрицательный вес фрагмента
	 */
	protected final void incInitialStats(Fragment state, double weight) {
		Double count = initial.get(state);
		initial.put(state, (count == null) ? weight : (count + weight));
	}
	
	/**
	 * Возвращает условную вероятность перехода между заданными цепочками полных состояний.
	 * Если в процессе обучения ни в одном из прецедентов не наблюдалось желаемого перехода,
	 * возвращается <code>0</code>.
	 * 
	 * @param tail 
	 *    последовательность, из которой происходит переход
	 * @param head
	 *    последовательность, в которую происходит переход
	 * @return вероятность перехода
	 */
	public double getTransP(Fragment tail, Fragment head) {
		float[] trans = transitions.get(tail);
		// Index of the head state among all complete states of the same length
		int idx = factory.getTotalIndex(head);
		
		return ((trans == null) || (trans[headsCount] == 0)) 
				? 0 : (1.0 * trans[idx] / trans[headsCount]);
	}
	
	/**
	 * Обновляет статиситику по переходам между парой цепочек полных состояний.
	 * 
	 * @param tail 
	 *    последовательность, из которой происходит переход
	 * @param head
	 *    последовательность, в которую происходит переход
	 * @param weight
	 *    неотрицательный вес прецедента
	 */
	protected final void incTransStats(Fragment tail, Fragment head, double weight) {
		int totalIndex = factory.getTotalIndex(head);
		float[] trans = transitions.get(tail);
		if (trans == null) {
			trans = new float[headsCount + 1];
			transitions.put(tail, trans);
		}
		trans[totalIndex] += weight;
		trans[headsCount] += weight;
	}
	
	@Override
	public void reset() {
		nSequences = 0;
		initial.clear();
		transitions.clear();
		lengthDistr.reset();
	}
	
	/**
	 * Обучает модель на паре строк, состоящей из наблюдаемых и соответствующих им скрытых состояний.
	 * 
	 * @param observed
	 *    цепочка наблюдаемых состояний
	 * @param hidden
	 *    цепочка скрытых состояний, отвечающих наблюдаемым
	 */
	public void digest(byte[] observed, byte[] hidden) {
		this.digest(observed, hidden, 1.0);
	}
	
	/**
	 * Обучает модель на паре строк, состоящей из наблюдаемых и соответствующих им скрытых состояний,
	 * с заданным весом прецедента.
	 * 
	 * @param observed
	 *    цепочка наблюдаемых состояний
	 * @param hidden
	 *    цепочка скрытых состояний, отвечающих наблюдаемым
	 * @param weight
	 *    неотрицательный вес прецедента
	 */
	public void digest(byte[] observed, byte[] hidden, double weight) {
		if (observed.length < order) return;
		if (weight <= 0.0) return;
		
		assert(observed.length == hidden.length);
		doDigest(observed, hidden, weight);
		nSequences++;
	}
	
	/**
	 * Производит сбор статистики на паре строк, состоящей из наблюдаемых и соответствующих им скрытых состояний.
	 * При необходимости этот метод может переопределяться в подклассах.
	 * 
	 * @param observed
	 *    цепочка наблюдаемых состояний
	 * @param hidden
	 *    цепочка скрытых состояний, отвечающих наблюдаемым
	 * @param weight
	 *    неотрицательный вес прецедента
	 */
	protected void doDigest(byte[] observed, byte[] hidden, double weight) {
		lengthDistr.digest(observed.length, weight);
		
		Fragment state = factory.fragment(observed, hidden, 0, order);
		incInitialStats(state, weight);
		
		for (int i = order; i + depLength <= observed.length; i += depLength) {
			state = factory.fragment(observed, hidden, i - order, order);
			Fragment head = factory.fragment(observed, hidden, i, depLength);
			incTransStats(state, head, weight);
		}
	}
	
	/**
	 * Обучает параметры модели на наборе равнозначных прецедентов.
	 * 
	 * @param set
	 *    набор наблюдаемых и соответствующих им скрытых строк состояний
	 */
	public void digestSet(SequenceSet set) {
		for (int i = 0; i < set.length(); i++)
			digest(set.observed(i), set.hidden(i), 1.0);
	}
	
	/**
	 * Обучает параметры модели на наборе прецедентов с заданными весами.
	 * 
	 * @param set
	 *    набор наблюдаемых и соответствующих им скрытых строк состояний
	 * @param weights
	 *    веса прецедентов
	 */
	public void digestSet(SequenceSet set, double[] weights) {
		for (int i = 0; i < set.length(); i++)
			digest(set.observed(i), set.hidden(i), weights[i]);
	}
	
	/**
	 * Вычисляет функцию логарифмисеского правдоподобия для последовательности
	 * полных состояний. В процессе вычисления все составляющие начальные и переходные
	 * вероятности, а также вероятность наблюдения строки заданной длины ограничиваются
	 * снизу достаточно малыми неотрицательными числами. Таким образом, 
	 * вычисленное значение всегда является конечным.
	 * 
	 * @param observed
	 *    цепочка наблюдаемых состояний
	 * @param hidden
	 *    цепочка скрытых состояний, отвечающих наблюдаемым
	 * 
	 * @return
	 *    логарифмическое правдоподобие для цепочек
	 */
	public double estimate(byte[] observed, byte[] hidden) {
		double logP = 0.0;
		
		logP = Math.max(lengthDistr.estimate(observed.length), -15);
		if (observed.length < order) {
			return logP;
		}
		
		Fragment state = factory.fragment(observed, hidden, 0, order);
		logP = Math.log(Math.max(1e-4, getInitialP(state)));
		
		for (int i = order; i + depLength <= observed.length; i += depLength) {
			final Fragment lState = factory.fragment(observed, hidden, i - order, order);
			final Fragment mState = factory.fragment(observed, hidden, i, depLength);
			
			logP += Math.log(Math.max(1e-4, getTransP(lState, mState)));
		}
		
		return logP;
	}
	
	/**
	 * Восстанавливает поля объекта, которые не записываются в поток, 
	 * на основе сохраненных полей.
	 * 
	 * @param stream
	 *    поток для считывания объекта
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream stream) 
			throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		
		// Initialize transient fields
		headsCount = 1;
		for (int i = 0; i < depLength; i++)
			headsCount *= (observedStates.length() * hiddenStates.length());
		
		factory = new FragmentFactory(observedStates, hiddenStates, order + depLength);
	}
	
	@Override
	public String repr() {
		return Messages.format("alg.chain", this.depLength(), this.order());
	}
	
	@Override
	public String toString() {
		// cagG = 1*64 + 2*4 + 2 = 74; 8 + 4 + 2 = 14
		Fragment tail = new Fragment(74, 14, 4);
		System.out.println(factory.toString(tail));
		System.out.println(Arrays.toString(transitions.get(tail)));
		
		return String.format("h=%d, order=%d", depLength, nSequences);
	}
}

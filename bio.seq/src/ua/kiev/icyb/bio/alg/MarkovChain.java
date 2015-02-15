package ua.kiev.icyb.bio.alg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
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
public class MarkovChain extends AbstractDistribution<Sequence> implements Representable {
	
	private static final long serialVersionUID = 1L;
	
	/** Длина зависимой цепочки состояний. */
	private int depLength;
	/** Порядок цепи Маркова. */
	protected int order;
	
	/** Алфавит наблюдаемых состояний. */
	private final String observedStates;
	/** Алфавит скрытых состояний. */
	private final String hiddenStates;
	/** Алфавит полных состояний. */
	private final String completeStates;
	
	/** Фабрика для работы с фрагментами цепочек состояний. */
	protected transient FragmentFactory factory;
	
	/** 
	 * Количество последовательностей, которые были использованы для обучения параметров
	 * вероятностной модели.
	 */
	protected int nSequences;
	
	/** Статистика по начальным состояниям цепочек полных состояний. */
	private Map<Fragment, Double> initial;
	
	/**
	 * Возворащает статистику по начальным состояниям цепочек полных состояний.
	 * 
	 * Для каждой цепочки полных состояний длины {@link #order()} подсчитывается взвешенное количество
	 * строк, которые с этой цепочки начинаются. Если в таблице нет элемента, соответствующая цепочка не
	 * начинает ни одной строки из обучающей выборки.
	 * 
	 * @return
	 *    хэш-таблица со статистикой по начальным состояниям 
	 */
	public Map<Fragment, Double> getInitialTable() {
		return Collections.unmodifiableMap(initial);
	}
	
	public Collection<Fragment> getInitialStates() {
		return Collections.unmodifiableSet(initial.keySet());
	}
	
	/** Количество возможных различных зависимых цепочек состояний. */
	private transient int headsCount;
	
	/**
	 * Статистика по переходам из цепочек полных состояний длины, определяемой порядком
	 * марковской цепи, в цепочки длины зависимой части. Последовательности
	 * длины {@link #order} в таблице соответствует массив величин, каждая из которых равна взвешенному
	 * числу переходов из этой последовательности в одно из возможных зависимых состояний.
	 * Зависимые состояния упорядочены в алфавитном порядке, определяемом
	 * методом {@link Fragment#index()}.
	 * Последний элемент массива равен сумме остальных элементов (предназначен для ускорения
	 * вычислений).
	 */
	protected Map<Fragment, double[]> transitions;
	
	/**
	 * Возвращает статистику по переходам из цепочек полных состояний длины, определяемой порядком
	 * марковской цепи, в цепочки длины зависимой части. 
	 * 
	 * Последовательности длины {@link #order()} в таблице соответствует массив величин, 
	 * каждая из которых равна взвешенному числу переходов из этой последовательности 
	 * в одно из возможных зависимых состояний.
	 * Зависимые состояния упорядочены в алфавитном порядке, определяемом
	 * методом {@link Fragment#index()}.
	 * Последний элемент массива равен сумме остальных элементов (предназначен для ускорения
	 * вычислений).
	 * 
	 * @return
	 *    хэш-таблица с статистикой переходов
	 */
	public Map<Fragment, double[]> getTransitionTable() {
		return Collections.unmodifiableMap(transitions);
	}
	
	public Collection<Fragment> getTransitionTails() {
		return Collections.unmodifiableSet(transitions.keySet());
	}
	
	/** Вероятностное распределение строк по длинам. */
	protected Distribution<Integer> lengthDistr;
	
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
	 * @param completeStates
	 *    алфавит полных состояний (может равняться {@code null})
	 */
	public MarkovChain(int depLength, int order, String observedStates, 
			String hiddenStates, String completeStates) {
		
		if ((completeStates != null) 
				&& (completeStates.length() != observedStates.length() * hiddenStates.length())) {
			
			throw new IllegalArgumentException("Wrong number of complete states");
		}
		
		this.depLength = depLength;
		this.order = order;
		this.observedStates = observedStates;
		this.hiddenStates = hiddenStates;
		this.completeStates = completeStates;
		initialize();
	}
	
	/**
	 * Создает марковскую цепь с заданными параметрами.
	 * 
	 * @param depLength
	 *    длина зависимой цепочки состояний 
	 * @param order
	 *    порядок цепочки
	 * @param set
	 *    образец выборки, используемый для определения алфавитов наблюдаемых и скрытых
	 *    состояний
	 */
	public MarkovChain(int depLength, int order, SequenceSet set) {
		this(depLength, order, set.observedStates(), set.hiddenStates(), set.completeStates());
	}
	
	/**
	 * Копирует параметры вероятностной модели из другой марковской цепи.
	 * 
	 * @param other
	 */
	protected MarkovChain(MarkovChain other) {
		this(other.depLength, other.order, 
				other.observedStates, other.hiddenStates, other.completeStates);
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
	 * Возвращает алфавит полных состояний, используемый в этой вероятностной модели.
	 * 
	 * @return 
	 *    строка, каждый символ которой уникален и обозначает одно из полных состояний
	 *    
	 * @see ua.kiev.icyb.bio.SequenceSet#completeStates()
	 */
	public String completeStates() {
		return completeStates;
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
	public MarkovChain clone() {
		MarkovChain other = (MarkovChain) super.clone();
		other.initial = (Map<Fragment, Double>) 
				((HashMap<Fragment, Double>) this.initial).clone();
		other.transitions = (Map<Fragment, double[]>) 
				((HashMap<Fragment, double[]>) this.transitions).clone();
		return other;
	}
	
	@Override
	public MarkovChain clearClone() {
		MarkovChain other = (MarkovChain) super.clone();
		other.initialize();
		return other;
	}
	
	/**
	 * Выполняет инициализацию большинства полей класса.
	 */
	protected void initialize() {
		headsCount = 1;
		for (int i = 0; i < depLength; i++)
			headsCount *= (observedStates.length() * hiddenStates.length());
		
		factory = new FragmentFactory(observedStates, hiddenStates, completeStates,
				order + depLength);
		initial = new HashMap<Fragment, Double>();
		nSequences = 0;
		transitions = new HashMap<Fragment, double[]>();
		
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
		initial.put(state.clone(), (count == null) ? weight : (count + weight));
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
		double[] trans = transitions.get(tail);
		int idx = head.index();
		
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
		int totalIndex = head.index();
		double[] trans = transitions.get(tail);
		if (trans == null) {
			trans = new double[headsCount + 1];
			transitions.put(tail.clone(), trans);
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
		lengthDistr.train(observed.length, weight);
		
		Fragment tail = factory.fragment(), head = factory.fragment(); 
		factory.fragment(observed, hidden, 0, order, tail);
		incInitialStats(tail, weight);
		
		for (int i = order; i + depLength <= observed.length; i += depLength) {
			factory.fragment(observed, hidden, i - order, order, tail);
			factory.fragment(observed, hidden, i, depLength, head);
			incTransStats(tail, head, weight);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void writeObject(ObjectOutputStream stream) throws IOException {
		
		Map<Fragment, double[]> transitions = this.transitions;
		
		Map<Fragment, ?> _tempMap = new HashMap<Fragment, float[]>();
		for (Map.Entry<Fragment, double[]> entry : transitions.entrySet()) {
			double[] dVal = entry.getValue();
			float[] fVal = new float[dVal.length];
			
			for (int i = 0; i < fVal.length; i++) {
				fVal[i] = (float) dVal[i];
			}
			
			((Map<Fragment, float[]>) _tempMap).put(entry.getKey(), fVal);
		}
		
		this.transitions = (Map<Fragment, double[]>) _tempMap;
		stream.defaultWriteObject();
		this.transitions = transitions;
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
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream stream) 
			throws IOException, ClassNotFoundException {
		
		stream.defaultReadObject();
		
		for (Map.Entry<Fragment, ?> entry : this.transitions.entrySet()) {
			if (entry.getValue() instanceof float[]) {
				float[] fVal = (float[]) entry.getValue(); 
				double[] dVal = new double[fVal.length];
				for (int i = 0; i < fVal.length; i++) {
					dVal[i] = fVal[i];
				}
				
				((Map.Entry<Fragment, double[]>) entry).setValue(dVal);
			}
		}
		
		// Initialize transient fields
		headsCount = 1;
		for (int i = 0; i < depLength; i++) {
			headsCount *= (observedStates.length() * hiddenStates.length());
		}
		
		this.factory = new FragmentFactory(observedStates, hiddenStates, completeStates,
				order + depLength);
		for (Fragment tail : initial.keySet()) {
			if (tail.factory == null) tail.factory = this.factory;
		}
		for (Fragment tail : transitions.keySet()) {
			if (tail.factory == null) tail.factory = this.factory;
		}
	}
	
	@Override
	public String repr() {
		return Messages.format("alg.chain", this.depLength(), this.order());
	}
	
	@Override
	public String toString() {
		return String.format("h=%d, order=%d", depLength, nSequences);
	}

	@Override
	public void train(Sequence sample, double weight) {
		if (weight <= 0.0) return;
		if (sample.length() < order) return;
		
		doDigest(sample.observed, sample.hidden, weight);
		nSequences++;
	}

	@Override
	public double estimate(Sequence point) {
		final byte[] observed = point.observed, hidden = point.hidden;
		double logP = 0.0;
		
		logP = Math.max(lengthDistr.estimate(observed.length), -15);
		if (observed.length < order) {
			return logP;
		}
		
		Fragment tail = factory.fragment(), head = factory.fragment();
		
		factory.fragment(observed, hidden, 0, order, tail);
		logP = Math.log(Math.max(1e-4, getInitialP(tail)));
		
		for (int i = order; i + depLength <= observed.length; i += depLength) {
			factory.fragment(observed, hidden, i - order, order, tail);
			factory.fragment(observed, hidden, i, depLength, head);
			
			logP += Math.log(Math.max(1e-4, getTransP(tail, head)));
		}
		
		return logP;
	}
	
	@Override
	public Sequence generate() {
		int length = -1;
		while (length < this.order) {
			length = this.lengthDistr.generate();
		}
		
		byte[] observed = new byte[length], hidden = new byte[length];
		Sequence sequence = new Sequence(observed, hidden);
		
		Fragment[] tails = this.getInitialStates().toArray(new Fragment[0]);
		double[] p = new double[tails.length];
		
		int i = 0;
		for (Fragment tail : tails) {
			p[i] = this.getInitialP(tail);
			i++;
		}
		
		Fragment tail = DistributionUtils.choose(tails, p).clone();
		tail.embed(sequence, 0);
		
		Fragment[] heads = this.factory().allFragments(depLength()).toArray(new Fragment[0]);
		p = new double[heads.length];
		
		for (int pos = this.order(); pos < length; pos += depLength()) {
			i = 0;
			for (Fragment head : heads) {
				p[i] = this.getTransP(tail, head);
				i++;
			}
			
			Fragment chosenHead = DistributionUtils.choose(heads, p);
			chosenHead.embed(sequence, pos);
			
			tail.append(chosenHead, tail);
			tail.suffix(this.order(), tail);
		}
		
		return sequence;
	}
}

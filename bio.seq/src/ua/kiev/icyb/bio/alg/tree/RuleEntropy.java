package ua.kiev.icyb.bio.alg.tree;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.Fragment;
import ua.kiev.icyb.bio.alg.FragmentFactory;


/**
 * Вспомогательный класс для вычисления функционала качества разбиения
 * пространства строк состояний.
 * 
 * <p><b>Функционал качества</b> бинарного разбиения выборки <code>T</code> на части
 * <code>T<sub>1</sub></code> и <code>T<sub>2</sub></code> определяется как
 * <blockquote>
 * <code>H(T<sub>1</sub>) + H(T<sub>2</sub>) - H(T)</code>,
 * </blockquote>
 * где {@code H(T)} ― функция, приближенно равная информационной энтропии для эмпирических
 * распределений начальных и переходных вероятностей марковской цепи, обученной на
 * выборке <code>T</code>.
 */
public class RuleEntropy implements Serializable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Полная выборка, для разбиений которой вычисляются функционалы качества.
	 */
	private final SequenceSet fullSet;
	
	/**
	 * Порядок марковских цепей, используемых для подсчета функционала качества.
	 */
	private final int order;
	
	/**
	 * Следует ли учитывать в функционале качества информационную энтропию для
	 * распределения начальных вероятностей.
	 */
	private final boolean countInitials;

	/**
	 * Фабрика для операций над цепочками строк состояний.
	 */
	private transient FragmentFactory factory;
	
	private transient Map<Fragment, Integer> fullMap, headMap, tailMap;
	private transient double fullEntropy;

	/**
	 * Создает объект класса для подсчета функционала качества разбиений заданной выборки.
	 * 
	 * @param set
	 *        используемая выборка
	 * @param order
	 *        порядок марковских цепей, используемых для подсчета функционала качества
	 * @param countInitials
	 *        следует ли учитывать в функционале качества информационную энтропию для
	 *        распределения начальных вероятностей
	 */
	public RuleEntropy(SequenceSet set, int order, boolean countInitials) {
		this.fullSet = set;
		this.order = order;
		this.countInitials = countInitials;

		factory = new FragmentFactory(set.observedStates(), set.hiddenStates(), order + 1);
		this.fullEntropy = entropy();
	}

	/**
	 * Создает объект класса для подсчета функционала качества разбиений заданной выборки. 
	 * Распределение начальных вероятностей принимается в расчет.
	 * 
	 * @param set
	 *        используемая выборка
	 * @param order
	 *        порядок марковских цепей, используемых для подсчета функционала качества
	 */
	public RuleEntropy(SequenceSet set, int order) {
		this(set, order, true);
	}

	/**
	 * Возвращает полную выборку, для разбиений которой вычисляются функционалы качества.
	 * 
	 * @return
	 *    полная выборка
	 */
	public SequenceSet getSet() {
		return fullSet;
	}

	private Map<Fragment, Integer> headStats(SequenceSet set, int length) {
		Map<Fragment, Integer> stats = new HashMap<Fragment, Integer>();

		for (int i = 0; i < set.length(); i++) {
			byte[] observed = set.observed(i);
			byte[] hidden = set.hidden(i);
			if (observed.length < length)
				continue;

			Fragment state = factory.fragment(observed, hidden, 0, length);
			Integer val = stats.get(state);
			val = (val == null) ? 1 : (val + 1);
			stats.put(state, val);
		}

		return stats;
	}

	private Map<Fragment, Integer> stats(SequenceSet set, int length) {
		Map<Fragment, Integer> stats = new HashMap<Fragment, Integer>();

		for (int i = 0; i < set.length(); i++) {
			byte[] observed = set.observed(i);
			byte[] hidden = set.hidden(i);
			if (observed.length < length)
				continue;

			for (int pos = 0; pos < hidden.length - length; pos++) {
				Fragment state = factory.fragment(observed, hidden, pos, length);
				Integer val = stats.get(state);
				val = (val == null) ? 1 : (val + 1);
				stats.put(state, val);
			}
		}
		return stats;
	}

	private static double xlog(double x) {
		return (x == 0) ? 0 : x * Math.log(x);
	}

	private static double sum(Map<Fragment, Integer> all, Map<Fragment, Integer> part) {
		double result = 0;

		if (part == null) {
			for (Fragment key : all.keySet()) {
				Integer allVal = all.get(key);
				result += allVal * Math.log(allVal);
			}

			return result;
		}

		for (Fragment key : all.keySet()) {
			Integer allVal = all.get(key), partVal = part.get(key);
			if (partVal == null)
				partVal = 0;

			result += xlog(partVal) + xlog(allVal - partVal);
		}
		return result;
	}

	/**
	 * Вычисляет информационную энтропию {@code H} для полной выборки. 
	 * 
	 * @return
	 *    информационная энтропия
	 */
	private double entropy() {
		tailMap = stats(fullSet, order);
		fullMap = stats(fullSet, order + 1);
		double result = sum(fullMap, null) - sum(tailMap, null);

		if (countInitials) {
			headMap = headStats(fullSet, order);
			result += sum(headMap, null) - xlog(fullSet.length());
		}

		return result;
	}

	/**
	 * Вычисляет функционал качества для бинарного разбиения, заданного
	 * подмножеством выборки.
	 * 
	 * @param subset
	 *        подмножество полной выборки, указанной при создании объекта
	 * @return
	 *    значение функционала качества разбиения
	 */
	public double fitness(SequenceSet subset) {
		Map<Fragment, Integer> ruleHeadMap = headStats(subset, order), ruleTailMap = stats(
				subset, order), ruleFullMap = stats(subset, order + 1);

		double result = sum(fullMap, ruleFullMap) - sum(tailMap, ruleTailMap);
		if (countInitials) {
			double headProb = sum(headMap, ruleHeadMap) - xlog(subset.length())
					- xlog(fullSet.length() - subset.length());
			result += headProb;
		}
		result -= fullEntropy;

		return result;
	}

	/**
	 * Вычисляет функционал качества для бинарного разбиения, заданного
	 * предикатом.
	 * 
	 * @param rule
	 *        предикат, порождающий разбиение
	 * @return
	 *    значение функционала качества разбиения
	 */
	public double fitness(PartitionRule rule) {
		boolean[] complies = rule.test(fullSet);
		SequenceSet subset = fullSet.filter(complies);
		return fitness(subset);
	}
	
	/**
	 * Восстанавливает поля объекта, которые не записываются в поток, на основе сохраненных полей.
	 * 
	 * @param in
	 *    поток для считывания объекта
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		
		factory = new FragmentFactory(fullSet.observedStates(), fullSet.hiddenStates(), 
				order + 1);
		this.fullEntropy = entropy();
	}
}

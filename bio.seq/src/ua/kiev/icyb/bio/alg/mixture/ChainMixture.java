package ua.kiev.icyb.bio.alg.mixture;

import java.io.Serializable;
import java.util.Arrays;

import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.Trainable;
import ua.kiev.icyb.bio.alg.MarkovChain;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Взвешенная композиция марковских цепей.
 * 
 * <p>Под взвешенной композицией подразумевается вероятностное распределение с функцией
 * правдоподобия
 * <blockquote>
 * <code>P(x) = ∑<sub>i</sub> w<sub>i</sub>P<sub>i</sub>(x),</code>
 * </blockquote>
 * где сумма неотрицательных весов <code>w<sub>i</sub></code> равна единице, а составные
 * распределения имеют вид {@linkplain MarkovChain марковских цепей}.
 */
public class ChainMixture implements Serializable, Trainable, Representable {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Значения порога достоверности, испольуемые при отображении смеси с помощью
	 * метода {@link #reprDistribution(SequenceSet)}.
	 */
	private static final double[] ALIGNMENT_THRESHOLDS = { 0.99, 0.95, 0.9, 0.5 };
	
	/**
	 * Печатает сводку по распеределению прецедентов выборки по вероятностым моделям
	 * из взвешенной смеси.
	 * 
	 * @param weights
	 *    веса, полученные с помощью метода {@link #getWeights(SequenceSet, ChainMixture)}
	 * @param threshold 
	 *    порог достоверности, т.е. минимальная апостериорная вероятность, досатачная, чтобы 
	 *    отнести прецедент к некоторой вероятностной модели
	 * @return
	 *    число прецедентов, принадлежащих каждой модели из смеси
	 */
	private static int[] getAlignments(double[][] weights, double threshold) {
		final int count = weights.length;
		
		int[] counts = new int[count];
		for (int alg = 0; alg < count; alg++)
			for (int i = 0; i < weights[0].length; i++)
				if (weights[alg][i] > threshold) {
					counts[alg]++;
				}
		
		return counts;
	}

	/**
	 * Печатает сводку по распеределению прецедентов выборки по вероятностым моделям
	 * из взвешенной композиции.
	 * 
	 * @param weights
	 *    веса для объектов выборки, полученные с помощью метода {@link #getWeights(SequenceSet)}
	 * @return
	 *    сводка по распределению
	 */
	public static String reprDistribution(double[][] weights) {
		String repr = ""; 
		
		for (double thres : ALIGNMENT_THRESHOLDS) {
			repr += Messages.format("em.alignments", thres, 
					Arrays.toString(getAlignments(weights, thres))) + "\n";
		}
		
		return repr;
	}

	/**
	 * Массив марковских цепей, соответствующих распределениям, входящим в композицию.
	 */
	public MarkovChain[] chains;
	/**
	 * Веса распределений в композиции, т.е. априорные вероятности для этих распределений.
	 */
	public double[] weights;
	
	/**
	 * Создает пустую композицию.
	 */
	public ChainMixture() {
		chains = new MarkovChain[0];
		weights = new double[0];
	}
	
	/**
	 * Создает новую взвешенную композицию с заданным количеством марковских цепей.
	 * 
	 * @param count
	 *    число марковских цепей в композиции
	 * @param order
	 *    порядок марковских цепей в композиции
	 * @param set
	 *    образец выборки, используемый для определения алфавитов наблюдаемых и скрытых
	 *    состояний 
	 */
	public ChainMixture(int count, int order, SequenceSet set) {
		chains = new MarkovChain[count];
		for (int i = 0; i < count; i++) {
			chains[i] = new MarkovChain(1, order, set.observedStates(), 
					set.hiddenStates());
		}
		
		weights = new double[count];
		Arrays.fill(weights, 1.0 / count);
	}
	
	/**
	 * Возвращает количество марковских цепей, взодящих в композицию. 
	 * 
	 * @return
	 *    число марковских цепей в композиции
	 */
	public int size() {
		return chains.length;
	}
	
	/**
	 * Удаляет одну из марковских цепей из композиции. Веса остальных
	 * моделей пропорционально увеличиваются так, чтобы в сумме они по-прежнему
	 * составляли единицу.
	 * 
	 * @param index
	 *    индекс (с отсчетом от нуля) марковской цепи, которую надо удалить
	 */
	public void delete(int index) {
		for (int i = index + 1; i < size(); i++) {
			chains[i - 1] = chains[i];
			weights[i - 1] = weights[i];
		}
		chains = Arrays.copyOf(chains, chains.length - 1);
		weights = Arrays.copyOf(weights, weights.length - 1);
		
		double sum = 0;
		for (int i = 0; i < size(); i++)
			sum += weights[i];
		for (int i = 0; i < size(); i++)
			weights[i] /= sum;
	}
	
	/**
	 * Добавляет новую модель в композицию. Веса остальных марковских цепей в композиции
	 * пропорционально уменьшаются так, чтобы в сумме все веса по-прежнему
	 * составляли единицу.
	 * 
	 * @param chain
	 *    марковская цепь, которая добавляется в композицию
	 * @param weight
	 *    вес новой модели
	 */
	public void add(MarkovChain chain, double weight) {
		chains = Arrays.copyOf(chains, size() + 1);
		chains[size() - 1] = chain;
		weights = Arrays.copyOf(weights, size());
		for (int i = 0; i < size() - 1; i++) {
			weights[i] *= (1 - weight);
		}
		weights[size() - 1] = weight;
	}
	
	/**
	 * Обучает параметры цепей, входящих в композицию, на случайных подмножествах
	 * выборки. Выборка случайным образом делится на приблизительно равные непересекающиеся 
	 * части, количество которых равно числу марковских цепей в композиции.
	 * Все цепи в композиции сбрасываются с помощью метода {@link MarkovChain#reset()}
	 * и затем обучаются на соответствующей части выборки.
	 * 
	 * @param set
	 *    набор полных состояний, используемый для обучения марковских цепей в композиции
	 */
	public void randomFill(SequenceSet set) {
		for (MarkovChain chain : chains)
			chain.reset();
		
		int[] counts = new int[size()];
		for (int i = 0; i < set.length(); i++) {
			int idx = (int) Math.floor(size() * Math.random());
			byte[] observed = set.observed(i);
			byte[] hidden = set.hidden(i);
			chains[idx].digest(observed, hidden);
			counts[idx]++;
		}
		
		for (int i = 0; i < size(); i++) {
			weights[i] = 1.0 * counts[i] / set.length();
		}
	}
	
	/**
	 * Вычисляет функцию логарифмического правдоподобия композиции для заданной
	 * последовательности полных состояний.
	 * 
	 * @param observed
	 *    строка наблюдаемых состояний
	 * @param hidden
	 *    строка скрытых состояний, соответствующих наблюдаемым
	 * @return
	 *    логарифмическое правдоподобие взвешенной композиции распределений
	 */
	public double estimate(byte[] observed, byte[] hidden) {
		double[] logP = new double[size()];
		double maxP = Double.NEGATIVE_INFINITY;
		for (int j = 0; j < size(); j++) {
			logP[j] = chains[j].estimate(observed, hidden);
			maxP = Math.max(maxP, logP[j]);
		}
		
		double weightC = 0.0;
		for (int j = 0; j < size(); j++) {
			logP[j] -= maxP;
			weightC += weights[j] * Math.exp(logP[j]);
		}
		
		return (maxP + Math.log(weightC));
	}
	
	@Override
	public String toString() {
		return String.format("[%d models]", chains.length);
	}
	
	@Override
	public Object clearClone() {
		try {
			ChainMixture other = (ChainMixture) super.clone();
			other.weights = this.weights.clone();
			other.chains = this.chains.clone();
			for (int i = 0; i < this.chains.length; i++) {
				other.chains[i] = (MarkovChain) this.chains[i].clearClone();
			}
			return other;
		} catch (CloneNotSupportedException e) {
			// Should never happen
			return null;
		}
	}
	
	@Override
	public Object clone() {
		try {
			ChainMixture other = (ChainMixture) super.clone();
			other.weights = this.weights.clone();
			other.chains = this.chains.clone();
			for (int i = 0; i < this.chains.length; i++) {
				other.chains[i] = (MarkovChain) this.chains[i].clone();
			}
			return other;
		} catch (CloneNotSupportedException e) {
			// Should never happen
			return null;
		}
	}

	public String repr() {
		String repr = Messages.format("em.n_models", size());
		if (size() > 0) {
			repr += "\n" + Messages.format("em.weights", Arrays.toString(this.weights)) + "\n";
			repr += Messages.format("em.chain", this.chains[0].repr());
		}
		return repr;
	}
	
	@Override
	public void reset() {
		for (MarkovChain chain : this.chains) {
			chain.reset();
		}
	}
}
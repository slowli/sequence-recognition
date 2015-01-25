package ua.kiev.icyb.bio.alg;

import java.util.Arrays;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.res.Messages;



/**
 * Модификация марковской цепи с аппроксимацией неизвестных
 * начальных и переходных вероятностей. Длина зависимой цепочки полных состояний
 * полагается равной <code>1</code>.
 * 
 * <p>При использовании алгоритмов распознавания скрытых последовательностей
 * на основе принципа максимума правдоподобия возникает вопрос оценки начальных
 * и переходных вероятностей, которые не могут быть получены из обучающей выборки.
 * Если полагать, что эти вероятности равны нулю (как это делается в классе {@link MarkovChain}),
 * алгоритм будет во многих случаях отказываться от распознавания. Поэтому
 * необходима аппроксимация неизвестных вероятностей с помощью сведений, которые
 * могут быть добыты из обучающей выборки.
 * 
 * <p>В данном классе аппроксимация производится на основе марковских цепей меньшего порядка.
 * Таким образом, для цепи <code>l</code>-го порядка условная вероятность
 * <blockquote>
 * <code>p(y|x), &nbsp;&nbsp x = x<sub>1</sub>x<sub>2</sub>...x<sub>l</sub></code>
 * </blockquote>
 * оценивается с использованием вероятностей
 * <blockquote>
 * <code>p(y|x<sub>2</sub>...x<sub>l</sub>), p(y|x<sub>3</sub>...x<sub>l</sub>), ..., p(y|x<sub>l</sub>).</code>
 * </blockquote>
 * Аналогично, начальная вероятность <code>π(x<sub>1</sub>x<sub>2</sub>...x<sub>l</sub>)</code> приближается
 * вероятностями
 * <blockquote>
 * <code>π(x<sub>2</sub>...x<sub>l</sub>), π(x<sub>3</sub>...x<sub>l</sub>), ..., π(x<sub>l</sub>).</code>
 * </blockquote>
 * Конкретные способы агрегации вероятностей моделей меньшего порядка определяются классом
 * {@link Approximation}.
 */
public class FallthruChain extends MarkovChain {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Минимальный используемый порядок цепи.
	 * 
	 * @see Approximation#minOrder
	 */
	private final int minOrder;
	
	/**
	 * Метод аппроксимации.
	 * 
	 * @see Approximation#strategy
	 */
	private final Approximation.Strategy strategy;
	
	/**
	 * Априорное значение для неизвестных начальных вероятностей.
	 * 
	 * @see Approximation#initThreshold
	 */
	private final double iThreshold;
	/**
	 * Априорное значение для неизвестных переходных вероятностей.
	 * 
	 * @see Approximation#transThreshold
	 */
	private final double tThreshold;
	
	/**
	 * Марковские цепи меньшего порядка, используемые для аппроксимации.
	 */
	private MarkovChain[] subchains;
	
	/**
	 * Создает марковскую цепь с аппроксимацией неизвестных вероятностей.
	 * 
	 * @param approx
	 *    используемые параметры аппроксимации
	 * @param alphabet
	 *    алфавит наблюдаемых состояний
	 * @param hAlphabet
	 *    алфавит скрытых состояний
	 */
	public FallthruChain(Approximation approx, String alphabet, String hAlphabet) {
		super(1, approx.order, alphabet, hAlphabet);
		this.minOrder = approx.minOrder;
		this.strategy = approx.strategy;
		this.iThreshold = approx.initThreshold;
		this.tThreshold = approx.transThreshold;
		
		initializeSubchains(approx.order);
	}
	
	@Override
	public FallthruChain clearClone() {
		FallthruChain other = (FallthruChain) super.clearClone();
		other.initializeSubchains(subchains.length - 1);
		return other;
	}
	
	private void initializeSubchains(int maxOrder) {
		subchains = new MarkovChain[maxOrder + 1];		
		for (int i = minOrder; i < maxOrder; i++)
			subchains[i] = new MarkovChain(1, i, observedStates(), hiddenStates());
		subchains[maxOrder] = this;
	}
	
	@Override
	public void train(Sequence sample, double weight) {
		super.train(sample, weight);
		for (int i = minOrder; i < order; i++) 
			if (order - i < sample.length()) {
				byte[] truncObs = Arrays.copyOfRange(sample.observed, order - i, sample.length());
				byte[] truncHid = Arrays.copyOfRange(sample.hidden, order - i, sample.length());
				subchains[i].train(new Sequence(null, truncObs, truncHid), weight);
			}
	}
	
	@Override
	public double getInitialP(Fragment state) {
		// TODO: implement different strategies
		return Math.max(super.getInitialP(state), iThreshold);
	}
	
	@Override
	public double getTransP(Fragment tail, Fragment head) {
		if (strategy == Approximation.Strategy.FIXED) {
			double[] trans = transitions.get(tail);
			int idx = factory.getTotalIndex(head);
			if ((trans == null) || (trans[idx] == 0)) 
				return tThreshold;
			return (1.0 * trans[idx] / trans[trans.length - 1]); 
		}
		
		Fragment suffix;
		double result = 0.0;
		int count = 0;
		
		int idx = factory.getTotalIndex(head);
		
		for (int tlen = order; tlen >= minOrder; tlen--) {
			suffix = factory.suffix(tail, tlen);
			double[] trans = subchains[tlen].transitions.get(suffix);
			if (trans != null) {
				result += 1.0 * trans[idx]/trans[trans.length - 1];
				count++;
				if ((strategy == Approximation.Strategy.FIRST) && (result > 0)) {
					count = 1;
					break;
				}
			} else {
				count++;
			}
		}
		if (count > 0) result /= count;
		
		return result;
	}
	
	@Override
	public void reset() {
		super.reset();
		for (int i = minOrder; i < order; i++) {
			subchains[i].reset();
		}
	}
	
	@Override
	public String repr() {
		String repr = super.repr() + "\n";
		repr += Messages.format("alg.approx", this.strategy, this.minOrder);
		return repr;
	}
}

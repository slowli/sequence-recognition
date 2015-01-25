package ua.kiev.icyb.bio.alg.mixture;

import java.util.Arrays;

import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
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
public class MarkovMixture extends Mixture<Sequence> implements Representable {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Создает пустую композицию.
	 */
	public MarkovMixture() {
		super();
	}
	
	/**
	 * Создает новую взвешенную композицию с заданным количеством марковских цепей.
	 * 
	 * @param size
	 *    число марковских цепей в композиции
	 * @param order
	 *    порядок марковских цепей в композиции
	 * @param set
	 *    образец выборки, используемый для определения алфавитов наблюдаемых и скрытых
	 *    состояний 
	 */
	public MarkovMixture(int size, int order, SequenceSet set) {
		super();
		for (int i = 0; i < size; i++) {
			MarkovChain chain = new MarkovChain(1, order, 
					set.observedStates(), 
					set.hiddenStates());
			this.add(chain, 1.0);
		}
		
		double[] weights = new double[size];
		Arrays.fill(weights, 1.0 / size);
		this.setWeights(weights);
	}
	
	@Override
	public MarkovChain model(int index) {
		return (MarkovChain) super.model(index);
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
		// XXX 
		/*for (MarkovChain chain : chains)
			chain.reset();
		
		int[] counts = new int[size()];
		for (Sequence sequence : set) {
			int idx = (int) Math.floor(size() * Math.random());
			chains[idx].train(sequence);
			counts[idx]++;
		}
		
		for (int i = 0; i < size(); i++) {
			weights[i] = 1.0 * counts[i] / set.size();
		}*/
	}

	public String repr() {
		// XXX move?
		String repr = Messages.format("em.n_models", size());
		if (size() > 0) {
			repr += "\n" + Messages.format("em.weights", Arrays.toString(this.weights())) + "\n";
			repr += Messages.format("em.chain", this.model(0).repr());
		}
		return repr;
	}
}
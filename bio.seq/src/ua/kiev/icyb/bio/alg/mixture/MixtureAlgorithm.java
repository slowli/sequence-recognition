package ua.kiev.icyb.bio.alg.mixture;

import java.util.Arrays;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.Fragment;
import ua.kiev.icyb.bio.alg.GeneViterbiAlgorithm;
import ua.kiev.icyb.bio.alg.MarkovChain;

/**
 * Алгоритм поиска наиболее вероятной последовательности скрытых состояний
 * для смеси скрытых марковских моделей. Находит последовательность скрытых состояний
 * при помощи итеративного двухфазного алгоритма оптимизации, напоминающего
 * EM-алгоритм.
 */
public class MixtureAlgorithm extends GeneViterbiAlgorithm {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Максимальное количество итераций в алгоритме нахождения наиболее вероятной последовательности
	 * скрытых состояний.
	 */
	private static final int MAX_ITERATIONS = 10;
	
	/**
	 * Граничное расстояние между оценками апостериорных вероятностей составляющих распределений в смеси,
	 * которое используется в алгоритме нахождения наиболее вероятной последовательности
	 * скрытых состояний.
	 * 
	 * Если растояние между оценками, полученными в результате двух последовательных итераций
	 * алгоритма оптимизации, меньше граничного расстояния, алгоритм прекращает свою работу.
	 */
	private static final double MIN_WEIGHT_DISTANCE = 1e-4;

	/**
	 * Марковская модель, применяемая для нахождения промежуточных 
	 * решений при поиске наиболее вероятной последовательности скрытых состояний.
	 * Логарифмы начальных и переходных вероятностей в этой модели равны 
	 * взвешенной сумме соответствущих логарифмов вероятностей для моделей, 
	 * входящих в смесь.
	 */
	private static class MixtureMarkovChain extends MarkovChain {

		private static final long serialVersionUID = 1L;
		
		
		private final MarkovMixture mixture;
		private double[] weights;
		
		public MixtureMarkovChain(MarkovMixture mixture) {
			super(mixture.model(0));
			this.mixture = mixture;
		}
		
		public void setWeights(double[] weights) {
			this.weights = weights;
		}
		
		public double getWeight(int index) {
			return weights[index];
		}
		
		@Override
		public double getInitialP(Fragment state) {
			double logP = 0;
			for (int i = 0; i < this.mixture.size(); i++) {
				logP += getWeight(i) * Math.max(-1000.0, 
						Math.log(this.mixture.model(i).getInitialP(state)));
			}
			return Math.exp(logP);
		}
		
		@Override
		public double getTransP(Fragment tail, Fragment head) {
			double logP = 0;
			for (int i = 0; i < this.mixture.size(); i++) {
				logP += getWeight(i) * Math.max(-1000.0,
						Math.log(this.mixture.model(i).getTransP(tail, head)));
			}
			return Math.exp(logP);
		}
	}
	
	private static double distance(double[] x, double[] y) {
		double dist = 0.0;
		for (int i = 0; i < x.length; i++) {
			dist = Math.max(dist, Math.abs(x[i] - y[i]));
		}
		return dist;
	}
	
	/** Начальное приближение для используемых смесей распределений. */
	private final MarkovMixture baseMixture;
	
	/**
	 * Текущая смесь распределений, полученная в результате подгонки базовой смеси
	 * на обучающей выборке.
	 */
	private transient MarkovMixture currentMixture;
	
	/**
	 * Создает новый алгоритм распознавания, использующий смесь марковских моделей.
	 * Предоставленная смесь служит начальным приближением; 
	 * она уточняется с помощью EM-алгоритма при обучении на определенной выборке. 
	 * 
	 * @param mixture
	 * 		начальное приближение для используемых смесей распределений
	 */
	public MixtureAlgorithm(MarkovMixture mixture) {
		super(mixture.model(0).order(), false);
		this.baseMixture = mixture;
	}
	
	@Override
	public void train(Sequence sequence) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void train(SequenceSet set) {
		this.currentMixture = (MarkovMixture) this.baseMixture.clone();
		
		// TODO брать параметры откуда-то
		EMAlgorithm emAlgorithm = new EMAlgorithm();
		emAlgorithm.mixture = this.currentMixture;
		emAlgorithm.set = set;
		emAlgorithm.saveTemplate = null;
		emAlgorithm.nIterations = 5;
		emAlgorithm.stochastic = true;
		emAlgorithm.ordinaryRun();
		
		this.currentMixture = emAlgorithm.mixture;
	}
	
	@Override
	public void reset() {
		this.currentMixture = null;
	}
	
	@Override
	public byte[] run(Sequence sequence) {
		// Проверить несколько начальных комбинаций весов, чтобы избежать попадания
		// в локальные максимумы
		
		double[] weights = new double[this.currentMixture.size()];
		byte[][] hidden = new byte[this.currentMixture.size()][];
		for (int k = 0; k < weights.length; k++) {
			Arrays.fill(weights, 0.0);
			weights[k] = 1.0;
			hidden[k] = this.run(sequence.observed, weights);
		}
		
		double maxP = Double.NEGATIVE_INFINITY;
		int maxIdx = -1;
		for (int k = 0; k < weights.length; k++) {
			if (hidden[k] != null) {
				double logP = this.currentMixture.estimate(new Sequence(sequence.observed, hidden[k]));
				if (logP > maxP) {
					maxP = logP;
					maxIdx = k;
				}
			}
		}
		
		if (maxIdx < 0) return null;
		return hidden[maxIdx];
	}
	

	/**
	 * Выполняет алгоритм с заданными начальными значениями апостериорных вероятностей марковских моделей,
	 * входящих в смесь.
	 * 
	 * @param sequence
	 * 		последовательность наблюдаемых состояний
	 * @param initialWeights
	 * 		массив апостериорных вероятностей; сумма вероятностей должна быть равна единице
	 * @return
	 * 		наиболее вероятная последовательность скрытых состояний, соответствующих наблюдаемой цепочке
	 */
	public byte[] run(byte[] sequence, double[] initialWeights) {
		double[] aposterioriP = initialWeights.clone();
		
		double[] oldAposterioriP;
		byte[] hidden = null;
		
		double distance = 1.0;
		int nIterations = 0;
		final MixtureMarkovChain chain = new MixtureMarkovChain(this.currentMixture);
		
		do {
			oldAposterioriP = aposterioriP.clone();
			chain.setWeights(aposterioriP);
			
			hidden = super.run(sequence, chain);
			if (hidden == null) {
				return null;
			}
			
			double maxLogP = Double.NEGATIVE_INFINITY;
			for (int k = 0; k < currentMixture.size(); k++) {
				aposterioriP[k] = currentMixture.model(k).estimate(new Sequence(sequence, hidden)) 
						+ Math.log(currentMixture.weight(k));
				if (aposterioriP[k] > maxLogP) {
					maxLogP = aposterioriP[k];
				}
			}
			
			double sum = 0;
			for (int k = 0; k < currentMixture.size(); k++) {
				aposterioriP[k] -= maxLogP;
				aposterioriP[k] = Math.exp(aposterioriP[k]);
				sum += aposterioriP[k];
			}
			for (int k = 0; k < currentMixture.size(); k++) {
				aposterioriP[k] /= sum;
			}
			
			distance = distance(aposterioriP, oldAposterioriP);
			nIterations++;
		} while ((distance > MIN_WEIGHT_DISTANCE) && (nIterations < MAX_ITERATIONS));
		
		return hidden;
	}
}

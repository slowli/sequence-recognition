package ua.kiev.icyb.bio.alg.mixture;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.Fragment;
import ua.kiev.icyb.bio.alg.GeneViterbiAlgorithm;
import ua.kiev.icyb.bio.alg.MarkovChain;
import ua.kiev.icyb.bio.alg.ViterbiAlgorithm;

/**
 * Алгоритм поиска наиболее вероятной последовательности скрытых состояний
 * для смеси скрытых марковских моделей. Находит последовательность скрытых состояний
 * при помощи итеративного двухфазного алгоритма оптимизации, напоминающего
 * EM-алгоритм.
 */
public class MixtureAlgorithm extends GeneViterbiAlgorithm {

	private static final long serialVersionUID = 1L;

	/**
	 * Марковская модель, применяемая для нахождения промежуточных 
	 * решений при поиске наиболее вероятной последовательности скрытых состояний.
	 * Логарифмы начальных и переходных вероятностей в этой модели равны 
	 * взвешенной сумме соответствущих логарифмов вероятностей для моделей, 
	 * входящих в смесь.
	 */
	private static class MixtureMarkovChain extends MarkovChain {

		private static final long serialVersionUID = 1L;
		
		
		private transient ChainMixture mixture;
		private transient Map<Thread, double[]> weights;
		
		public MixtureMarkovChain(ChainMixture mixture) {
			super(mixture.chains[0]);
			this.mixture = mixture;
		}
		
		public void setMixture(ChainMixture mixture) {
			this.mixture = mixture;
		}
		
		public void setWeights(double[] weights) {
			if (this.weights == null) {
				this.weights = new HashMap<Thread, double[]>();
			}
			this.weights.put(Thread.currentThread(), weights);
		}
		
		public double getWeight(int index) {
			double[] weights = this.weights.get(Thread.currentThread());
			return weights[index];
		}
		
		@Override
		public double getInitialP(Fragment state) {
			double logP = 0;
			for (int i = 0; i < this.mixture.size(); i++) {
				logP += getWeight(i) * Math.max(-1000.0, 
						Math.log(this.mixture.chains[i].getInitialP(state)));
			}
			return Math.exp(logP);
		}
		
		@Override
		public double getTransP(Fragment tail, Fragment head) {
			double logP = 0;
			for (int i = 0; i < this.mixture.size(); i++) {
				logP += getWeight(i) * Math.max(-1000.0,
						Math.log(this.mixture.chains[i].getTransP(tail, head)));
			}
			return Math.exp(logP);
		}
	}
	
	private static double distance(double[] x, double[] y) {
		double dist = 0;
		for (int i = 0; i < x.length; i++) {
			dist = Math.max(dist, Math.abs(x[i] - y[i]));
		}
		return dist;
	}
	
	private final ChainMixture baseMixture;
	private transient ChainMixture currentMixture;
	
	/**
	 * Создает новый алгоритм распознавания, использующий смесь марковских моделей.
	 * Предоставленная смесь служит начальным приближением; 
	 * она уточняется с помощью EM-алгоритма при обучении на определенной выборке. 
	 * 
	 * @param mixture
	 * 		начальное приближение для используемых смесей распределений
	 */
	public MixtureAlgorithm(ChainMixture mixture) {
		super(new MixtureMarkovChain(mixture), false);
		this.baseMixture = mixture;
	}
	
	@Override
	public void train(byte[] observed, byte[] hidden) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void train(SequenceSet set) {
		this.currentMixture = (ChainMixture) this.baseMixture.clone();
		((MixtureMarkovChain) this.chain).setMixture(this.currentMixture);
		
		/*EMAlgorithm emAlgorithm = new EMAlgorithm();
		emAlgorithm.mixture = this.currentMixture;
		emAlgorithm.set = set;
		emAlgorithm.saveTemplate = null;
		emAlgorithm.nIterations = 10;
		emAlgorithm.stochastic = true;
		emAlgorithm.ordinaryRun();
		
		this.currentMixture = emAlgorithm.mixture;*/
	}
	
	@Override
	public void reset() {
		this.currentMixture = null;
	}
	
	@Override
	public byte[] run(byte[] sequence) {
		// Проверить несколько начальных комбинаций весов, чтобы избежать попадания
		// в локальные максимумы
		
		double[] weights = new double[this.currentMixture.size()];
		byte[][] hidden = new byte[this.currentMixture.size()][];
		for (int k = 0; k < weights.length; k++) {
			Arrays.fill(weights, 0.0);
			weights[k] = 1.0;
			hidden[k] = this.run(sequence, weights);
		}
		
		double maxP = Double.NEGATIVE_INFINITY;
		int maxIdx = -1;
		for (int k = 0; k < weights.length; k++) {
			if (hidden[k] != null) {
				double logP = this.currentMixture.estimate(sequence, hidden[k]);
				if (logP > maxP) {
					maxP = logP;
					maxIdx = k;
				}
			}
		}
		
		if (maxIdx < 0) return null;
		System.out.println("maxIdx = " + maxIdx);
		return hidden[maxIdx];
	}
	

	public byte[] run(byte[] sequence, double[] initialWeights) {
		double[] aposterioriP = initialWeights.clone();
		
		double[] oldAposterioriP;
		byte[] hidden = null;
		
		do {
			System.out.println("weights: " + Arrays.toString(aposterioriP));
			oldAposterioriP = aposterioriP.clone();
			((MixtureMarkovChain) this.chain).setWeights(aposterioriP);
			
			hidden = super.run(sequence);
			if (hidden == null) {
				return null;
			}
			
			double maxLogP = Double.NEGATIVE_INFINITY;
			for (int k = 0; k < currentMixture.size(); k++) {
				aposterioriP[k] = currentMixture.chains[k].estimate(sequence, hidden) 
						+ Math.log(currentMixture.weights[k]);
				if (aposterioriP[k] > maxLogP) {
					maxLogP = aposterioriP[k];
				}
			}
			
			System.out.println("logP: " + Arrays.toString(aposterioriP));
			
			double sum = 0;
			for (int k = 0; k < currentMixture.size(); k++) {
				aposterioriP[k] -= maxLogP;
				aposterioriP[k] = Math.exp(aposterioriP[k]);
				sum += aposterioriP[k];
			}
			for (int k = 0; k < currentMixture.size(); k++) {
				aposterioriP[k] /= sum;
			}
		} while (distance(aposterioriP, oldAposterioriP) > 1e-4);
		
		return hidden;
	}
	
	private void readObject(ObjectInputStream stream) 
			throws IOException, ClassNotFoundException {
		
		stream.defaultReadObject();
		this.chain = new MixtureMarkovChain(this.baseMixture);
	}
}

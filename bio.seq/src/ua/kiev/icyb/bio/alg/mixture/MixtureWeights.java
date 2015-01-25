package ua.kiev.icyb.bio.alg.mixture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.Launchable;
import ua.kiev.icyb.bio.SequenceSet;


/**
 * Вычисляет апостериорные вероятности для каждой пары из марковской цепи
 * и строки полных состояний из выборки. Для вычисления используется
 * стандартный пул вычислительных потоков {@link Env#executor()}.
 */
public class MixtureWeights implements Launchable {

	private static final long serialVersionUID = 1L;
	

	private static class WeightTask implements Callable<Void> {

		private final SequenceSet set;
		private final MarkovMixture mixture;
		private final double[][] outWeights;
		private final int index;
		
		public WeightTask(SequenceSet set, MarkovMixture mixture, double[][] outWeights, int index) {
			this.set = set;
			this.mixture = mixture;
			this.outWeights = outWeights;
			this.index = index;
		}
		
		@Override
		public Void call() throws Exception {
			
			double[] logP = new double[mixture.size()],
					exp = new double[mixture.size()];
			double sum, diff;
			
			for (int alg = 0; alg < mixture.size(); alg++) {
				logP[alg] = mixture.model(alg).estimate(set.get(index));
			}			
			
			for (int alg = 0; alg < mixture.size(); alg++) {
				sum = 0;
				for (int alg1 = 0; alg1 < mixture.size(); alg1++) {
					diff = logP[alg1] - logP[alg];
					exp[alg1] = (diff > 50) ? Math.exp(50) : Math.exp(diff);
					exp[alg1] *= mixture.weight(alg1);
					sum += exp[alg1];
				}
				
				outWeights[alg][index] = exp[alg] / sum;
				
				if (Double.isNaN(outWeights[alg][index])) {
					throw new IllegalStateException("Invalid mixture (not trained?)");
				}
			}
			
			return null;
		}
	}
	
	private final MarkovMixture mixture;
	
	private final SequenceSet set;
	
	/**
	 * Вычисленные апостериорные вероятности.
	 * Строки массива соответствуют моделям смеси, столбцы - прецедентам выборки.
	 */
	public final double[][] weights;
	
	/**
	 * Хэш-таблица, сопоставляющая идентификатору каждой строки из выборки
	 * номер модели из смеси, имеющеей максимальную апостериорную вероятность на этой строке.
	 */
	public final Map<String, Integer> labels = new HashMap<String, Integer>();
	
	/**
	 * Создает новое задание.
	 * 
	 * @param mixture
	 *    смесь марковских моделей
	 * @param set
	 *    выборка, для которой вычисляются вероятности
	 */
	public MixtureWeights(MarkovMixture mixture, SequenceSet set) {
		this.mixture = mixture;
		this.set = set;
		this.weights = new double[mixture.size()][set.size()];
	}
	
	@Override
	public void run(Env env) {
		final ExecutorService executor = env.executor();
		
		List<WeightTask> tasks = new ArrayList<WeightTask>(); 
		for (int i = 0; i < set.size(); i++) {
			tasks.add(new WeightTask(set, mixture, weights, i));
		}
		
		try {
			for (Future<Void> future: executor.invokeAll(tasks)) {
				future.get();
			}
		} catch (InterruptedException e) {
			env.exception(e);
		} catch (ExecutionException e) {
			env.exception(e);
		}
		
		labels.clear();
		for (int i = 0; i < set.size(); i++) {
			int maxChain = -1;
			double maxP = Double.NEGATIVE_INFINITY;
			for (int k = 0; k < mixture.size(); k++) {
				if (weights[k][i] > maxP) {
					maxP = weights[k][i];
					maxChain = k;
				}
			}
			
			labels.put(set.id(i), maxChain);
		}
	}
	
	@Override
	public Env getEnv() {
		return null;
	}
}

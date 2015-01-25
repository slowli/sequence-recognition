package ua.kiev.icyb.bio.alg.mixture;

import ua.kiev.icyb.bio.res.Messages;

/**
 * EM-алгоритм для построения взвешенной смеси марковских цепей с 
 * последовательным удалением компонент.
 */
public class DecrementalEMAlgorithm extends EMAlgorithm {

	private static final long serialVersionUID = 1L;
	
	/** Окончательное количество вероятностных моделей в композиции. */
	public int minModels = 3;

	/**
	 * Выполняет EM-алгоритм с последовательным удалением компонент.
	 * 
	 * После оптимизации весов и параметров моделей взвешенной композиции 
	 * с фиксированным количеством компонент удаляется модель с наименьшим весом.
	 * Так продолжается до тех пор, пока количество моделей не станет меньше заданного полем 
	 * {@link #minModels}.
	 */
	protected void decrementalRun() {
		while (mixture.size() >= minModels) {
			ordinaryRun();
			resetIteration();
			
			if (mixture.size() == minModels) {
				break;
			}
			
			// Убрать компоненту с наименьшим весом
			double[] weights = mixture.weights(); 
			int minModel = -1;
			double minWeight = 1.0; 
			
			for (int i = 0; i < weights.length; i++) {
				if (weights[i] < minWeight) {
					minWeight = weights[i];
					minModel = i;
				}
			}
			getEnv().debug(1, Messages.format("em.remove", minModel + 1, minWeight));
			mixture.delete(minModel);
			
			saveMixture();
			save();
		}
	}
	
	@Override
	protected void doRun() {
		getEnv().debug(1, repr());
		decrementalRun();
	}
	
	@Override
	protected String reprOptions() {
		String repr = super.reprOptions() + "\n";
		repr += Messages.format("em.min_models", minModels) + "\n";
		return repr;
	}
}

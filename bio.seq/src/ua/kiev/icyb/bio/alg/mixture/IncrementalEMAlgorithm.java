package ua.kiev.icyb.bio.alg.mixture;

import java.util.Arrays;
import java.util.Comparator;

import ua.kiev.icyb.bio.alg.MarkovChain;
import ua.kiev.icyb.bio.res.Messages;

/**
 * EM-алгоритм для построения взвешенной смеси марковских цепей с 
 * последовательным добавлением компонент.
 */
public class IncrementalEMAlgorithm extends EMAlgorithm {

	private static final long serialVersionUID = 1L;
	
	private static class Record {
		public final int index;
		public final double data;
		
		public Record(int index, double data) {
			this.index = index;
			this.data = data;
		}
		
		public String toString() {
			return String.format("(%d, %.6f)", index, data);
		}
	}
	
	private static final Comparator<Record> R_COMPARATOR = new Comparator<Record>() {

		@Override
		public int compare(Record x, Record y) {
			return Double.compare(x.data, y.data);
		}
	};
	
	/** 
	 * Метод выбора наихудших прецедентов из выборки для обучения новых моделей.
	 * 
	 * @see EMSelectionMethod
	 */
	public EMSelectionMethod selectionMethod = EMSelectionMethod.MEAN;
	
	/** 
	 * Использовать ли в качестве меры качества прецедента максимальную 
	 * апостериорную вероятность его генерации моделями в композиции.
	 * Если <code>false</code> (значение по умолчанию), для меры качества используется
	 * логарифмическое правдоподобие, нормализованное на длину строки.
	 */
	public boolean selectWeights = false;
	
	/** 
	 * Абсолютный сдвиг границы, отделяющей "плохие" прецеденты. Например, значение поля {@code 1}
	 * означает, что в плохие прецеденты будет добавлена еще одна строка с наименьшим значением 
	 * меры качества; {@code -10} означает, что из плохих прецедентов будет удалено десять
	 * строк с наибольшими значениями меры качества.
	 */
	public int indexOffset = 0;
	
	/**
	 * Сдвиг границы, отделяющей "плохие" прецеденты. Это значение добавляется к пороговому значению,
	 * определяемому на основе {@link #selectionMethod}.
	 */
	public double valueOffset = 0.0;

	/** Окончательное количество вероятностных моделей в композиции. */
	public int maxModels = 3;
	
	/**
	 * Следует ли применять EM-алгоритм к начальной смеси, не увеличивая число ее компонент.
	 * Если значение поля равно {@code false}, алгоритм немедленно приступает к поиску "плохих"
	 * прецедентов.
	 */
	public boolean fitInitialMixture = true;
	
	/**
	 * Поиск в выборке прецедентов, хуже всего описывающихся композицией вероятностных моделей.
	 * Метод поиска зависит от заданных параметров алгоритма.
	 *  
	 * @return 
	 *    индексы наихудших прецедентов по отношению к заданной композиции
	 */
	protected int[] worstSamples() {
		getEnv().debug(1, Messages.getString("em.bad_search"));
		
		int count = mixture.size();
		final int len = set.size();
		double[] prob = new double[set.size()];
		
		Arrays.fill(prob, Double.NEGATIVE_INFINITY);
		
		if (selectWeights) {
			MixtureWeights mw = new MixtureWeights(mixture, set);
			mw.run(getEnv());
			double[][] weights = mw.weights;
			
			for (int i = 0; i < set.size(); i++)
				for (int alg = 0; alg < count; alg++) {
					prob[i] = Math.max(prob[i], weights[alg][i]);
				}
		} else {
			for (int i = 0; i < set.size(); i++) {
				for (int alg = 0; alg < count; alg++)
					prob[i] = Math.max(prob[i], mixture.model(alg).estimate(set.get(i)));
				
				prob[i] /= set.get(i).length();
			}
		}
		
		Record[] records = new Record[set.size()];
		for (int i = 0; i < set.size(); i++)
			records[i] = new Record(i, prob[i]);
		Arrays.sort(records, R_COMPARATOR);
		
		double median = records[len/2].data;
		double mean = 0;
		for (int i = 0; i < len; i++)
			mean += records[i].data / len;
		
		count = 0;
		double threshold = 0.0;
		switch (selectionMethod) {
			case FIXED: 
				threshold = 0;
				break;
			case MEDIAN:
				threshold = median;
				break;
			case MEAN:
				threshold = mean;
				break;
		}
		count = Arrays.binarySearch(records, 
				new Record(-1, threshold + valueOffset), R_COMPARATOR);
		if (count < 0) count = -count - 1;
		count += indexOffset;
		count = Math.min(Math.max(count, 0), len);
		
		int[] indices = new int[count];
		for (int i = 0; i < count; i++)
			indices[i] = records[i].index;
		getEnv().debug(1, Messages.format("em.bad_found", count));
		
		return indices;
	}

	/**
	 * Выполняет EM-алгоритм с последовательным добавлением марковских цепей.
	 * После оптимизации весов и параметров моделей взвешенной композиции 
	 * с фиксированным количеством компонент,
	 * новая компонента строится на основе прецедентов, которые хуже всего описываются этой композицией.
	 */
	protected void incrementalRun() {
		boolean fitMixture = this.fitInitialMixture;
		
		while (mixture.size() <= maxModels) {
			if ((mixture.size() > 1) && fitMixture) {
				ordinaryRun();
			}

			fitMixture = true;
			resetIteration();
			
			if (mixture.size() == maxModels) {
				break;
			}
			
			int[] idx = worstSamples();
			
			MarkovChain chain = (MarkovChain) mixture.model(0).clearClone();
			int newCount = idx.length;
			for (int i = 0; i < newCount; i++) {
				chain.train(set.get(idx[i]));
			}
			double newWeight = 1.0 * newCount / set.size();
			getEnv().debug(1, Messages.format("em.add", newCount, newWeight));
			mixture.add(chain, newWeight);
		}
	}
	
	@Override
	protected void doRun() {
		getEnv().debug(1, repr());
		incrementalRun();
	}
	
	@Override
	protected String reprOptions() {
		String repr = super.reprOptions() + "\n";
		repr += Messages.format("em.sel_method", selectionMethod) + "\n";
		repr += Messages.format("em.offsets", indexOffset, valueOffset) + "\n";
		repr += Messages.format("em.max_models", maxModels);
		return repr;
	}
}

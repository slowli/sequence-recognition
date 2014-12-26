package ua.kiev.icyb.bio.alg.tree;

import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.mixture.ChainMixture;

/**
 * Подкласс композиций алгоритмов распознавания, в которых области компетентности
 * определяются на основе взвешенных смесей марковских цепей. Номер алгоритма,
 * классифицирующего строку наблюдаемых состояний, соответствует максимальной
 * апостериорной вероятности среди марковских цепей из смеси, вычисленной для этой строки.
 *   
 * <p>Для применения алгоритма необходимо знать цепочки скрытых состояний для всех
 * прецедентов. Для случая, когда они неизвестны, можно использовать алгоритм
 * {@link SwitchAlgorithm} с маркерами прецедентов, вычисленными с помощью методов машинного
 * обучения (например, SVM). 
 */
public class MixtureSwitchAlgorithm extends SwitchAlgorithm {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Вычисляет номера компетентного алгоритма для всех элементов выборки.
	 * 
	 * @param set
	 *    выборка
	 * @param mixture
	 *    смесь марковских цепей, на основании которой опредляются номера
	 * @return
	 *    индекс компетентного алгоритма для всех строк в выборке
	 */
	private static byte[] calculateClasses(SequenceSet set, ChainMixture mixture) {
		double[][] weights = mixture.getWeights(set);
		byte[] markers = new byte[set.length()];
		for (int i = 0; i < set.length(); i++) {
			double max = 0;
			int maxChain = -1;
			for (int chain = 0; chain < weights.length; chain++)
				if (max < weights[chain][i]) {
					max = weights[chain][i];
					maxChain = chain;
				}
			markers[i] = (byte)maxChain;
		}
		return markers;
	}

	/**
	 * Создает новый алгоритм распознавания.
	 * 
	 * @param mixture
	 *    взвешенная смесь марковских цепей, используемая для определения
	 *    областей компетентности для каждого из прецедентов
	 * @param set
	 *    набор прецедентов, который будет далее использоваться с этим алгоритмом
	 * @param algs
	 *    составляющие алгоритмы распознавания 
	 */
	public MixtureSwitchAlgorithm(ChainMixture mixture, SequenceSet set, SeqAlgorithm[] algs) {
		super(calculateClasses(set, mixture), set, algs);
	}
}

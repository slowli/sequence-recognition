package ua.kiev.icyb.bio.alg;

import java.io.IOException;

import ua.kiev.icyb.bio.CommonTasks;
import ua.kiev.icyb.bio.IOUtils;
import ua.kiev.icyb.bio.QualityEstimation;
import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.Task;
import ua.kiev.icyb.bio.CrossValidation;
import ua.kiev.icyb.bio.alg.tree.PriorityCompAlgorithm;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Задания, связанные с простыми алгоритмами распознавания скрытых последовательностей.
 */
public class AlgTasks {
	
	/**
	 * Вычисляет качество распознавания алгоритма, обученного на некоторой выборке, на другой
	 * выборке.
	 * 
	 * <p>В качестве алгоритма распознавания используется класс {@link ViterbiAlgorithm}.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя обучающей выборки/выборок;
	 * <li>имя контрольной выборки/выборок;
	 * <li>порядок марковской цепи, используемой в алгоритме распознавания;
	 * <li>файл, в который следует сохранять результаты кросс-валидации.
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * quality human elegans 6 human-elegans-6.dbg
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	@Task(id="quality",res=Messages.BUNDLE_NAME)
	public static void quality(String[] args) throws IOException {
		final SequenceSet trainSet = IOUtils.createSet(args[0]);
		final SequenceSet controlSet = IOUtils.createSet(args[1]);
		
		final int order = Integer.parseInt(args[2]);
		final String debugFile = args[3];
		
		SeqAlgorithm algorithm;
		if (controlSet.hiddenStates().length() == 2) {
			algorithm = new GeneViterbiAlgorithm(order, false, controlSet);
		} else {
			algorithm = new ViterbiAlgorithm(1, order, controlSet);
		}
		
		QualityEstimation testCase = new QualityEstimation(trainSet, controlSet);
		testCase.attachAlgorithm(algorithm);
		testCase.setSaveFile(debugFile);
		testCase.save();
		
		CommonTasks.launch(testCase);
	}
	
	/**
	 * Осуществляет кросс-валидацию для алгоритма распознавания скрытых последовтельностей,
	 * основанного на алгоритме Витерби.
	 * 
	 * <p>В качестве алгоритма распознавания используется класс {@link ViterbiAlgorithm}.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>порядок марковской цепи, используемой в алгоритме распознавания;
	 * <li>файл, в который следует сохранять результаты кросс-валидации.
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * viterbi.cv human 6 human-6.dbg
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	@Task(id="viterbi.cv",res=Messages.BUNDLE_NAME)
	public static void viterbiCV(String[] args) throws IOException {
		final String input = args[0];
		final int order = Integer.parseInt(args[1]);
		final String debugFile = args[2];
		
		SequenceSet set = IOUtils.createSet(input);
		
		SeqAlgorithm algorithm;
		if (set.hiddenStates().length() == 2) {
			algorithm = new GeneViterbiAlgorithm(order, false, set);
		} else {
			algorithm = new ViterbiAlgorithm(1, order, set);
		}
		
		int nFolds = (set.hiddenStates().length() == 2) ? 5 : 10;
		CrossValidation testCase = new CrossValidation(set, nFolds);
		testCase.attachAlgorithm(algorithm);
		testCase.setSaveFile(debugFile);
		testCase.save();
		
		CommonTasks.launch(testCase);
	}
	
	/**
	 * Осуществляет кросс-валидацию для алгоритма распознавания скрытых последовтельностей
	 * с аппроксимацией неизвестных вероятностей.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>метод аппроксимации неизвестных вероятностей. Допустимые значения:
	 * <ul>
	 * <li>{@code "priority"} для голосования по старшинству среди моделей меньшего порядка;
	 * <li>{@code "fixed"} для аппроксимации фиксированными значениями;
	 * <li>{@code "mean"} для усреднения соответствующих вероятностей в моделях меньшего порядка;
	 * <li>{@code "first"} для оценки вероятности первой ненулевой вероятностью среди моделей меньшего порядка;
	 * </ul>
	 * <li>максимальный и минимальный порядок марковской цепи, 
	 * используемой в алгоритме распознавания, разделенные дефисом <code>'-'</code>;
	 * <li>файл, в который следует сохранять результаты кросс-валидации
	 * </ol>
	 * 
	 * <p><b>Пример.</b>
	 * <pre>
	 * approx.cv proteins mean 5-2 proteins-5-2-mean.dbg
	 * </pre>
	 * 
	 * @see PriorityCompAlgorithm
	 * @see FallthruAlgorithm
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	@Task(id="approx.cv",res=Messages.BUNDLE_NAME)
	public static void approxCV(String[] args) throws IOException {
		final String input = args[0];
		
		Approximation.Strategy strategy = null;
		if (!args[1].equalsIgnoreCase("priority")) {
			strategy = Approximation.Strategy.valueOf(args[1].toUpperCase());
		}
		String[] parts = args[2].split("-", 2);
		final int order = Integer.parseInt(parts[0]);
		final int minOrder = Integer.parseInt(parts[1]);
		
		final String debugFile = args[3];
		
		SequenceSet set = IOUtils.createSet(input);
		
		SeqAlgorithm algorithm;
		if (strategy == null) {
			algorithm = new PriorityCompAlgorithm(minOrder, order, set);
		} else {
			algorithm = new FallthruAlgorithm(
					new Approximation(order, minOrder, strategy), set);
		}
		
		int nFolds = (set.hiddenStates().length() == 2) ? 5 : 10;
		CrossValidation testCase = new CrossValidation(set, nFolds);
		testCase.attachAlgorithm(algorithm);
		testCase.setSaveFile(debugFile);
		testCase.save();
		
		CommonTasks.launch(testCase);
	}
}

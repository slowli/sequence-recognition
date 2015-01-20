package ua.kiev.icyb.bio.alg.mixture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

import ua.kiev.icyb.bio.CommonTasks;
import ua.kiev.icyb.bio.CrossValidation;
import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.IOUtils;
import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.Task;
import ua.kiev.icyb.bio.alg.Approximation;
import ua.kiev.icyb.bio.alg.FallthruAlgorithm;
import ua.kiev.icyb.bio.alg.FallthruChain;
import ua.kiev.icyb.bio.alg.Fragment;
import ua.kiev.icyb.bio.alg.GeneViterbiAlgorithm;
import ua.kiev.icyb.bio.alg.MarkovChain;
import ua.kiev.icyb.bio.alg.tree.MixtureSwitchAlgorithm;
import ua.kiev.icyb.bio.alg.tree.SwitchAlgorithm;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Задания, связанные со взвешенными смесями распределений, задающихся марковскими цепями.
 */
public final class MixtureTasks {
	
	/**
	 * Запускает EM-алгоритм для построения взвешенной смеси марковских цепей
	 * с последовательным добавлением компонент.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>порядок марковских цепей в композиции;
	 * <li>максимальное число вероятностных моделей в композиции;
	 * <li>{@linkplain EMAlgorithm#saveTemplate шаблон} для сохранения композиций после каждой итерации EM-алгоритма;
	 * <li>файл для сохранения промежуточных результатов алгоритма.
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * em.inc human 6 5 "human-inc.{n}-{i}" "human-inc.run"
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 * 
	 * @see IncrementalEMAlgorithm
	 */
	@Task(id="em.inc",res=Messages.BUNDLE_NAME)
	public static void emIncrement(String[] args) throws IOException {
		final String input = args[0];
		String[] parts = args[1].split("-", 2);
		final int order = Integer.parseInt(parts[0]);
		final int minOrder = (parts.length > 1) ? Integer.parseInt(parts[1]) : order;
		final int maxModels = Integer.parseInt(args[2]);
		final String saveTemplate = args[3];
		final String saveFile = args[4];
		
		final SequenceSet set = IOUtils.createSet(input);
		
		IncrementalEMAlgorithm em = new IncrementalEMAlgorithm();
		Env.setFields(em, "em.inc.");
		em.saveTemplate = saveTemplate;
		em.maxModels = maxModels;
		
		ChainMixture comp = new ChainMixture(1, order, set);
		if (minOrder < order) {
			comp.chains[0] = new FallthruChain(
					new Approximation(order, minOrder, Approximation.Strategy.MEAN),
					set.observedStates(), set.hiddenStates());
		}
		comp.weights[0] = 1.0;
		comp.chains[0].digestSet(set);
		
		em.set = set;
		em.mixture = comp;
		em.setSaveFile(saveFile);
		em.save();
		CommonTasks.launch(em);
	}
	
	/**
	 * Запускает EM-алгоритм для построения взвешенной смеси марковских цепей
	 * с последовательным удалением компонент.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>порядок марковских цепей в композиции;
	 * <li>начальное число вероятностных моделей в композиции;
	 * <li>конечное число вероятностных моделей в композиции;
	 * <li>{@linkplain EMAlgorithm#saveTemplate шаблон} для сохранения композиций после каждой итерации EM-алгоритма;
	 * <li>файл для сохранения промежуточных результатов алгоритма.
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * em.dec human 6 10 5 "human-dec.{n}-{i}""human-dec.run"
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 * 
	 * @see DecrementalEMAlgorithm
	 */
	@Task(id="em.dec",res=Messages.BUNDLE_NAME)
	public static void emDecrement(String[] args) throws IOException {
		final String input = args[0];
		String[] parts = args[1].split("-", 2);
		final int order = Integer.parseInt(parts[0]);
		final int minOrder = (parts.length > 1) ? Integer.parseInt(parts[1]) : order;
		final int initModels = Integer.parseInt(args[2]);
		final int nModels = Integer.parseInt(args[3]);
		final String saveTemplate = args[4];
		final String saveFile = args[5];
		
		final SequenceSet set = IOUtils.createSet(input);
		DecrementalEMAlgorithm em = new DecrementalEMAlgorithm();
		Env.setFields(em, "em.dec.");
		em.saveTemplate = saveTemplate;
		em.minModels = nModels;
		
		ChainMixture comp = null;
		comp = new ChainMixture(initModels, order, set);
		if (minOrder < order) {
			for (int i = 0; i < comp.size(); i++) {
				comp.chains[i] = new FallthruChain(
						new Approximation(order, minOrder, Approximation.Strategy.MEAN),
						set.observedStates(), set.hiddenStates());
			}
		}
		comp.randomFill(set);
		
		em.set = set;
		em.mixture = comp;
		em.setSaveFile(saveFile);
		em.save();
		CommonTasks.launch(em);
	}
	
	/**
	 * Осуществляет кросс-валидацию для алгоритма распознавания скрытых последовательностей,
	 * основанного на линейной смеси скрытых марковских моделей произвольного порядка.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>смесь скрытых марковских моделей, которая служит эталоном для построения смесей 
	 * на основе обучающих выборок;
	 * <li>файл для сохранения промежуточных результатов алгоритма.
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * mixture.cv human human.3 human.em-3.run
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 * 
	 * @see MixtureAlgorithm
	 */
	@Task(id="mixture.cv",res=Messages.BUNDLE_NAME)
	public static void mixtureCV(String[] args) throws IOException {
		final String input = args[0];
		final String mixtureFile = args[1];
		final String saveFile = args[2];
		
		SequenceSet set = IOUtils.createSet(input);
		final ChainMixture mixture = IOUtils.readObject(mixtureFile);
		final SeqAlgorithm algorithm = new MixtureAlgorithm(mixture);
		
		int nFolds = (set.hiddenStates().length() == 2) ? 5 : 10;
		CrossValidation testCase = new CrossValidation(set, nFolds);
		testCase.attachAlgorithm(algorithm);
		testCase.setSaveFile(saveFile);
		testCase.save();
		CommonTasks.launch(testCase);
	}
	
	/**
	 * Осуществляет кросс-валидацию для композиции алгоритмов распознавания скрытых последовательностей
	 * с эксклюзивной компетентностью составляющих. Области компетентности строятся на основе
	 * смесей марковских цепей.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>метод определения областей компетентности:
	 * <ul>
	 * <li>{@code "classes"} для загрузки информации об областях компетентности из текстового файла.
	 * Каждому прецеденту из выборки соответствует индекс алгоритма (с отсчетом от нуля) композиции, которым
	 * он должен классифицироваться.
	 * <li>{@code "mixture"} для вычисления областей компетентности на основе взвешенной смеси алгоритмов.
	 * Индекс алгоритма композиции, которым классифицируется прецедент из выборки, определяется как индекс
	 * цепи Маркова из смеси с наибольшей апостериорной вероятностью на этом прецеденте.
	 * </ul>
	 * <li>файл, определяющий области компетентности. Тип файла зависит от описанного выше метода
	 * определения областей.
	 * <li>порядок марковской цепи, используемой в алгоритме распознавания;
	 * <li>файл, в который следует сохранять результаты кросс-валидации.
	 * </ol>
	 * 
	 * <p><b>Примеры.</b> 
	 * <pre>
	 * switch.cv human classes "human-cls-3.dat" 6 human-6-cls-3.run
	 * switch.cv human mixture "human.3-10" 6 human-6-comp-3.run
	 * </pre>
	 * 
	 * @see SwitchAlgorithm
	 * @see MixtureSwitchAlgorithm
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	@Task(id="switch.cv",res=Messages.BUNDLE_NAME)
	public static void switchCV(String[] args) throws IOException {
		final String input = args[0];
		final String type = args[1];
		final String mixtureFile = args[2];
		String[] parts = args[3].split("-", 2);
		final int chainOrder = Integer.parseInt(parts[0]);
		final int minOrder = (parts.length > 1) ? Integer.parseInt(parts[1]) : chainOrder;
		final String saveFile = args[4];
		
		final SequenceSet set = IOUtils.createSet(input);
		
		byte[] classes = null;
		ChainMixture mixture = null;
		int nAlgs = 0;
		if (type.equalsIgnoreCase("classes")) {
			BufferedReader reader = new BufferedReader(new FileReader(mixtureFile));
			parts = reader.readLine().split("\\s+");
			classes = new byte[parts.length];
			for (int i = 0; i < parts.length; i++) {
				classes[i] = Byte.parseByte(parts[i]);
				nAlgs = Math.max(nAlgs, classes[i] + 1);
			}
			
			reader.close();
		} else {
			mixture = IOUtils.readObject(mixtureFile);
			nAlgs = mixture.size();
		}
		
		SeqAlgorithm[] algs = new SeqAlgorithm[nAlgs];
		for (int i = 0; i < algs.length; i++) {
			if (chainOrder == minOrder) {
				algs[i] = new GeneViterbiAlgorithm(chainOrder, false, set); 
			} else {
				algs[i] = new FallthruAlgorithm(
						new Approximation(chainOrder, minOrder, Approximation.Strategy.MEAN), 
						set); 
			}
		}
		
		SeqAlgorithm algorithm = (mixture != null) 
				? new MixtureSwitchAlgorithm(mixture, set, algs)
				: new SwitchAlgorithm(classes, set, algs);
		
		int nFolds = (set.hiddenStates().length() == 2) ? 5 : 10;
		CrossValidation testCase = new CrossValidation(set, nFolds);
		testCase.attachAlgorithm(algorithm);
		testCase.setSaveFile(saveFile);
		testCase.save();
		CommonTasks.launch(testCase);
	}
	
	private static double[][] getFeatures(SequenceSet set, int order) {
		final int nStates = set.observedStates().length();
		int pow = 1;
		for (int i = 0; i <= order; i++) {
			pow *= nStates;
		}
		double[][] features = new double[set.length()][pow];
		MarkovChain chain = new MarkovChain(1, order, set.observedStates(), set.hiddenStates());
		
		for (int i = 0; i < set.length(); i++) {
			chain.reset();
			chain.digest(set.observed(i), new byte[set.observed(i).length]);
			
			Fragment tail = new Fragment(0, 0, order);
			Fragment head = new Fragment(0, 0, 1);
			
			for (int feat = 0; feat < pow; feat++) {
				tail.observed = feat / nStates;
				head.observed = feat % nStates;
				features[i][feat] = chain.getTransP(tail, head);
			}
		}
		
		return features;
	}
	
	/**
	 * Печатает таблицу вещественных признаков, соответствующих последовательностям
	 * наблюдаемых состояний в некоторой выборке. Признаки считаются как значения всех
	 * переходных вероятностей определенного порядка, вычисленные для каждой из строк
	 * наблюдаемых состояний.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>имя файла, в который выводятся вычисленные свойства;
	 * <li>порядок условных вероятностей (по умолчанию 4).
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * em.features human "human-3.dat" 3
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	@Task(id="features",res=Messages.BUNDLE_NAME)
	public static void features(String[] args) throws IOException {
		final SequenceSet set = IOUtils.createSet(args[0]);
		final String outFile = args[1];
		final int order = (args.length > 2) ? Integer.parseInt(args[2]) : 4;
		
		Env.debug(1, Messages.format("misc.dataset", set.repr()));
		Env.debug(1, Messages.format("misc.out_file", outFile));
		Env.debug(1, Messages.format("misc.order", order));
		Env.debug(1, "");
		
		PrintStream out = new PrintStream(outFile);
		double[][] features = getFeatures(set, order);
		for (int i = 0; i < set.length(); i++) {
			for (int feat = 0; feat < features[i].length; feat++) {
				out.format(Locale.ENGLISH, "%.4f ", features[i][feat]);
			}
			out.println();
			
			if ((i + 1) % 100 == 0) {
				Env.debug(1, Messages.format("misc.n_processed", i + 1));
			}
		}
		out.close();
	}
	
	/**
	 * Выводит индекс марковской цепи из заданной композиции с максимальной апостериорной
	 * вероятностью для всех строк из выборки.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>имя файла, содержащего композицию распределений;
	 * <li>имя выходного файла.
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * mix.labels human "human.3-10" "human-cls-3.dat"
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	@Task(id="mix.labels",res=Messages.BUNDLE_NAME)
	public static void labels(String[] args) throws IOException {
		final String input = args[0];
		final String mixtureFile = args[1];
		final String outFile = args[2];
		final SequenceSet set = IOUtils.createSet(input);
		final ChainMixture mixture = IOUtils.readObject(mixtureFile);
		
		Env.debug(1, Messages.format("misc.dataset", set.repr()));
		Env.debug(1, Messages.format("misc.mixture", mixture.repr()));
		Env.debug(1, "");
		
		final PrintStream outStream = new PrintStream(outFile);
		Env.debug(1, Messages.format("task.mix.classes.out", outFile));
		
		double[][] weights = mixture.getWeights(set);
		for (int i = 0; i < set.length(); i++) {
			double max = 0;
			int maxChain = -1;
			for (int chain = 0; chain < weights.length; chain++) {
				if (max < weights[chain][i]) {
					max = weights[chain][i];
					maxChain = chain;
				}
			}
			outStream.println(set.get(i).id() + " " + maxChain);
		}
		outStream.close();
	}
	
	/**
	 * Печатает среднее значение логарифмического правдоподобия для строк из выборки
	 * и одной или более взвешенных смесей распределений.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>имя одного или более файлов, содержащих композиции распределений.
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * em.likelihood human "human.2-10" "human.3-10" "human.4-10"
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	@Task(id="mix.likelihood",res=Messages.BUNDLE_NAME)
	public static void likelihood(String[] args) throws IOException {
		final String input = args[0];
		final int count = args.length - 1;
		final SequenceSet set = IOUtils.createSet(input);
		
		Env.debug(0, Messages.format("misc.dataset", set.repr()));
		Env.debug(0, Messages.format("misc.in_files_n", count));
		Env.debug(0, "");
		
		for (int i = 1; i < args.length; i++) {
			System.out.println(Messages.format("misc.file", args[i]));
			ChainMixture comp = IOUtils.readObject(args[i]);
			double logProb = 0;
			for (int s = 0; s < set.length(); s++) {
				logProb += comp.estimate(set.observed(s), set.hidden(s)) / set.length();
			}
			
			System.out.println(Messages.format("task.mix.likelihood.p", logProb));
		}
	}
}

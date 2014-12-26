package ua.kiev.icyb.bio.alg.tree;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ua.kiev.icyb.bio.CommonTasks;
import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.IOUtils;
import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.Task;
import ua.kiev.icyb.bio.CrossValidation;
import ua.kiev.icyb.bio.alg.Approximation;
import ua.kiev.icyb.bio.alg.FallthruAlgorithm;
import ua.kiev.icyb.bio.alg.GeneViterbiAlgorithm;
import ua.kiev.icyb.bio.alg.GeneticAlgorithm;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Контейнер для заданий, связанных с композициями алгоритмов распознавания.
 */
public class TreeTasks {
	
	/**
	 * Запускает генетический алгоритм для отбора множеств цепочек наблюдаемых
	 * состояний, использование которых наиболее эффективно при построении 
	 * областей компетентности.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>длина цепочек во множествах;
	 * <li>шаблон имен файлов, в которые сохраняются отобранные множества цепочек;
	 * <code>"{i}"</code> в шаблоне заменяется на индекс поколения (с отсчетом от единицы);
	 * <li>файл для сохранения промежуточных результатов алгоритма.
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * tree.genetic human 2 "human-gen.{i}" "human-gen.run"
	 * </pre>
	 * 
	 * @see GeneticAlgorithm
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода 
	 */
	@Task(id="tree.genetic",res=Messages.BUNDLE_NAME)
	public static void geneticAlg(String[] args) throws IOException {
		final String input = args[0];
		final int seqLength = Integer.parseInt(args[1]);
		final String fileTemplate = args[2];
		final String saveFile = args[3];
		
		final int nItems = Env.intProperty("tree.genetic.initSize");
		final int order = Env.intProperty("tree.genetic.order");
		
		SequenceSet set = IOUtils.createSet(input);
		RuleEntropy entropy = new RuleEntropy(set, order);
		
		Set<FragmentSetWrapper> items = new HashSet<FragmentSetWrapper>();
		for (int i = 0; i < nItems; i++) {
			items.add(FragmentSetWrapper.random(entropy, seqLength));
		}
		
		final RuleGeneticAlgorithm alg = new RuleGeneticAlgorithm();
		Env.setFields(alg, "tree.genetic.");
		alg.initialPopulation = items;
		alg.saveTemplate = fileTemplate;
		
		alg.setSaveFile(saveFile);
		alg.save();
		CommonTasks.launch(alg);
	}
	
	/**
	 * Запускает алгоритм итеративного добавления признаков для отбора множеств цепочек наблюдаемых
	 * состояний, использование которых наиболее эффективно при построении 
	 * областей компетентности.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>длина цепочек;
	 * <li>имя файла, в который сохраняются отобранные множества цепочек после выполнения каждой итерации;
	 * <li>файл для сохранения промежуточных результатов алгоритма.
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * tree.add human 2 "human-add.sets" "human-add.run"
	 * </pre>
	 * 
	 * @see RuleAddAlgorithm
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода 
	 */
	@Task(id="tree.add",res=Messages.BUNDLE_NAME)
	public static void addAlg(String[] args) throws IOException {
		final String input = args[0];
		final int seqLength = Integer.parseInt(args[1]);
		final String setsFile = args[2];
		final String saveFile = args[3];
		
		SequenceSet set = IOUtils.createSet(input);
		FragmentSet bases = FragmentSet.getSequences(set.observedStates(), seqLength);
		
		RuleAddAlgorithm alg = new RuleAddAlgorithm();
		Env.setFields(alg, "tree.add.");
		alg.maxSize = bases.size() / 2;
		alg.setsFile = setsFile;
		alg.set = set;
		alg.bases = bases;
		alg.setSaveFile(saveFile);
		alg.save();
		CommonTasks.launch(alg);
	}
	
	/**
	 * Генерирует оптимальное разбиение пространства строк наблюдаемых состояний
	 * на области компетентности с использованием предикатов. Значение используемых
	 * предикатов на какой-либо строке зависит от концентрации в ней определенного множества 
	 * коротких цепочек наблюдаемых состояний.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>файл, содержащий множества цепочек наблюдаемых состояний, которые будут использоваться
	 * в предикатах в текстовом формате, или длина цепочек-эталонов для того, чтобы
	 * проверять все множества цепочек определенной длины;
	 * <li>количество правил в дереве;
	 * <li>файл, в который сериализуется полученное дерево предикатов;
	 * <li>файл для сохранения промежуточных результатов алгоритма.
	 * </ol>
	 * 
	 * <p><b>Примеры.</b> 
	 * <pre>
	 * tree human "human-2.sets" 5 "human-2.tree" "human-tree.run"
	 * tree human 1 5 "human-1.tree" "human-tree.run"
	 * </pre>
	 * 
	 * @see RuleTreeGenerator
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	@Task(id="tree",res=Messages.BUNDLE_NAME)
	public static void generateTree(String[] args) throws IOException {
		final String input = args[0];
		final String rulesFile = args[1];
		final int treeSize = Integer.parseInt(args[2]);
		final String treeFile = args[3];
		final String saveFile = args[4];
		
		SequenceSet set = IOUtils.createSet(input);
		
		@SuppressWarnings("unchecked")
		Collection<FragmentSet> baseSets = (new File(rulesFile).exists())
				? (Collection<FragmentSet>)IOUtils.readObject(rulesFile)
				: FragmentSet.getSequences(set.observedStates(), Integer.parseInt(rulesFile)).subsets();
		
		RuleTreeGenerator generator = new RuleTreeGenerator();
		Env.setFields(generator, "tree.gen.");
		generator.set = set;
		generator.baseSets = baseSets;
		generator.rules = treeSize;
		generator.treeFile = treeFile;
		
		generator.setSaveFile(saveFile);
		generator.save();
		CommonTasks.launch(generator);
	}

	/**
	 * Осуществляет кросс-валидацию для композиции алгоритмов распознавания скрытых последовтельностей,
	 * основанного на бинарном дереве предикатов.
	 * 
	 * <p><b>Аргументы.</b>
	 * <ol>
	 * <li>имя используемой выборки/выборок;
	 * <li>имя файла, содержащего дерево предикатов;
	 * <li>порядок марковской цепи, используемой в алгоритме распознавания;
	 * <li>число алгоритмов в композиции;
	 * <li>файл, в который следует сохранять результаты кросс-валидации.
	 * </ol>
	 * 
	 * <p><b>Пример.</b> 
	 * <pre>
	 * tree.cv human "human-1.tree" 6 3 human-6.t3.dbg
	 * </pre>
	 * 
	 * @param args
	 *    аргументы, перечисленные выше
	 * @throws IOException
	 *    в случае ошибки ввода/вывода
	 */
	@Task(id="tree.cv",res=Messages.BUNDLE_NAME)
	public static void treeCV(String[] args) throws IOException {
		final String input = args[0];
		final String treeFile = args[1];
		String[] parts = args[2].split("-", 2);
		final int chainOrder = Integer.parseInt(parts[0]);
		final int minOrder = (parts.length > 1) ? Integer.parseInt(parts[1]) : chainOrder;
		final int treeSize = Integer.parseInt(args[3]);
		final String debugFile = args[4];
		
		final SequenceSet set = IOUtils.createSet(input);
		PartitionRuleTree tree = IOUtils.readObject(treeFile);
		tree.trim(treeSize);
		
		SeqAlgorithm[] algs = new SeqAlgorithm[tree.size()];
		for (int i = 0; i < algs.length; i++) {
			if (chainOrder > minOrder) {
				algs[i] = new FallthruAlgorithm(new Approximation(chainOrder, minOrder, 
						Approximation.Strategy.MEAN), set);
			} else {
				algs[i] = new GeneViterbiAlgorithm(chainOrder, false, set);
			}
		}
		
		SeqAlgorithm algorithm = new TreeSwitchAlgorithm(tree, algs);
		int nFolds = (set.hiddenStates().length() == 2) ? 5 : 10;
		CrossValidation testCase = new CrossValidation(set, nFolds);
		testCase.attachAlgorithm(algorithm);
		testCase.setSaveFile(debugFile);
		testCase.save();
		CommonTasks.launch(testCase);
	}
}

package ua.kiev.icyb.bio.alg.tree;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ua.kiev.icyb.bio.AbstractLaunchable;
import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Класс для построения областей компетентности алгоритмов распознавания
 * на основе деревьев предикатов. Используемые предикаты используют концентрации 
 * коротких последовательностей наблюдаемых состояний.
 */
public class RuleTreeGenerator extends AbstractLaunchable implements Representable {
	
	private static final long serialVersionUID = 1L;

	/** Количество правил в дереве, которое надо построить. */
	public int treeSize;
	
	/** Порядок марковских цепей при вычислении функционала качества для правил. */
	public int order;
	
	/** 
	 * Набор отношений, в которых тестируемые предикаты делят выборку.
	 * Например, значение {@code 0.6} означает, что предикат должен быть 
	 * истинен на 60% прецедентов выборки.
	 */
	public double[] percentages;
	
	/**
	 * Определяет минимальное количество генов в частях выборки, генерируемых
	 * в процессе построения дерева. Если значение поля меньше единицы, оно рассматривается
	 * как доля от размера выборки; если же значение больше единицы, это абсолютное
	 * число. 
	 */
	public double minPartSize;
	
	/**
	 * Выборка, испольуемая для вычисления функционала качества предикатов.
	 */
	public SequenceSet set;
	
	/**
	 * Множества наборов цепочек наблюдаемых состояний, рассматриваемых в предикатах.
	 */
	public Collection<FragmentSet> baseSets;
	
	/**
	 * Имя файла, в который необходимо сохранить полученное дерево предикатов.
	 * Дерево сохраняется каждый раз при добавлении нового предиката.
	 */
	public String treeFile;
	
	/**
	 * Дерево предикатов, которое строится алгоритмом.
	 */
	private PartitionRuleTree tree;
	
	/**
	 * Текущий индекс части разбиения для всех прецедентов из выборки.
	 */
	private int[] partIdx;
	
	/**
	 * Максимальное значение функционала качества для разбиений каждой из текущих частей
	 * выборки. Значение {@link Double#NaN} означает, что функционал качества
	 * для соответствующей части выборки еще не вычислен.
	 */
	private double[] maxFitness;
	
	/**
	 * Оптимальное правило разбиения для каждой из текущих частей выборки.
	 */
	private PartitionRule[] optRule;
	
	/**
	 * Индекс рассматриваемой в данный момент части текущего разбиения выборки.
	 */
	private int currentPart;
	
	/**
	 * Сгенерированы ли предикаты для рассматриваемой в данный момент части 
	 * текущего разбиения выборки.
	 */
	private boolean rulesInferred = false;
	
	/**
	 * Предикаты для рассматриваемой в данный момент части текущего разбиения выборки.
	 */
	private List<PartitionRule> partRules;
	
	/**
	 * Отображение, связывающее предикаты и значения функционала качества порождаемых
	 * ими разбиений текущей части выборки. Значение {@link Double#NaN} обозначает
	 * предикат, для которого функционал качества еще не вычислен. 
	 */
	private Map<PartitionRule, Double> ruleFitness = null;
	
	/**
	 * Объект, используемый для вычисления функционала качества предикатов.
	 */
	private transient RuleEntropy entropy;
	
	/**
	 * Создает новый алгоритм построения дерева предикатов.
	 */
	public RuleTreeGenerator() {
	}
	
	/**
	 * Возвращает объект, используемый для вычисления функционала качества предикатов.
	 * 
	 * @return
	 *    объект для оценки функционала качества
	 */
	private RuleEntropy getEntropy() {
		if (entropy == null) {
			entropy = new RuleEntropy(set, order);
		}
		return entropy;
	}

	/**
	 * Вычисляет значения функционала качества для заданных предикатов.
	 * 
	 * @param set
	 *    выборка, используемая для вычисления функционала качества
	 * @param rules
	 *    правила, качество которых требуется оценить
	 * @return
	 *    отображение, связывающее предикаты и значения функционала качества порождаемых
	 *    ими разбиений
	 */
	private Map<PartitionRule, Double> getFitness(SequenceSet set, List<PartitionRule> rules) {
		if (this.ruleFitness == null) {
			this.ruleFitness = new HashMap<PartitionRule, Double>();
		}
		for (PartitionRule rule : rules) {
			if (!ruleFitness.containsKey(rule)) {
				ruleFitness.put(rule, Double.NaN);
			}
		}
		
		ExecutorService executor = getEnv().executor();
		RuleEntropy entropy = getEntropy();
		List<RuleTask> tasks = new ArrayList<RuleTask>();
		for (Map.Entry<PartitionRule, Double> entry : this.ruleFitness.entrySet()) {
			if (entry.getValue().isNaN()) {
				tasks.add(new RuleTask(set, entropy, entry));
			}
		}
		
		try {
			List<Future<Void>> futures = executor.invokeAll(tasks);
			for (int i = 0; i < futures.size(); i++) {
				futures.get(i).get();
			}
		} catch (InterruptedException e) {
			getEnv().exception(e);
		} catch (ExecutionException e) {
			getEnv().exception(e);
		}

		return ruleFitness;
	}
	
	/**
	 * Возвращает тестируемые предикаты для определенной выборки.
	 * 
	 * @param set
	 *    выборка, для которой надо построить предикаты
	 * @return
	 *    список предикатов, среди которых будет искаться оптимальный
	 */
	private List<PartitionRule> inferRules(SequenceSet set) {
		if (rulesInferred && (partRules != null)) {
			return partRules;
		}
		
		partRules = new ArrayList<PartitionRule>();
		ExecutorService executor = getEnv().executor();
		List<InferTask> tasks = new ArrayList<InferTask>();
		for (FragmentSet comb : baseSets) {
			tasks.add(new InferTask(set, comb, percentages));
		}
		
		try {
			getEnv().debugInline(1, Messages.getString("tree.infer"));
			List<Future<List<PartitionRule>>> futures = executor.invokeAll(tasks);
			for (Future<List<PartitionRule>> future : futures)
				partRules.addAll(future.get());
			getEnv().debug(1, "");
		} catch (InterruptedException e) {
			getEnv().exception(e);
		} catch (ExecutionException e) {
			getEnv().exception(e);
		}

		rulesInferred = true;
		return partRules;
	}

	private final class InferTask implements Callable<List<PartitionRule>> {
		private final SequenceSet set;
		private final double[] percentages;
		private final FragmentSet comb;

		public InferTask(SequenceSet set, FragmentSet comb, double[] percentages) {
			this.percentages = percentages;
			this.set = set;
			this.comb = comb;
		}

		public List<PartitionRule> call() throws Exception {
			List<PartitionRule> rules = new ArrayList<PartitionRule>();

			double[] vars = new double[set.size()];
			for (int i = 0; i < set.size(); i++)
				vars[i] = comb.calc(set.observed(i));
			Arrays.sort(vars);

			for (int i = 0; i < percentages.length; i++) {
				int idx = (int) (set.size() * percentages[i]);
				PartitionRule r = new ContentPartitionRule(comb, vars[idx]);
				rules.add(r);
			}
			getEnv().debugInline(1, ".");

			return rules;
		}
	}

	private class RuleTask implements Callable<Void> {
		private final SequenceSet fullSet;
		private final RuleEntropy entropy;
		private final Map.Entry<PartitionRule, Double> entry;
		
		public RuleTask(SequenceSet set, RuleEntropy entropy, Map.Entry<PartitionRule, Double> entry) {
			this.fullSet = set;
			this.entropy = entropy;
			this.entry = entry;
		}
		
		@Override
		public Void call() throws Exception {
			final PartitionRule rule = entry.getKey();
			
			boolean[] complies = rule.test(fullSet);
			SequenceSet subset = fullSet.filter(complies);
			double fitness = -1;
			if ((subset.size() < minPartSize) || (subset.size() > fullSet.size() - minPartSize)) {
				fitness = -1;
				getEnv().debug(1, Messages.format("tree.small_set", 
						Messages.format("tree.rule", rule, subset.size()) ));
			} else {
				fitness = entropy.fitness(subset);
				getEnv().debug(1, Messages.format("misc.fitness", 
						Messages.format("tree.rule", rule, subset.size()), 
						fitness));
			}
			
			entry.setValue(fitness);
			return null;	
		}
	}

	@Override
	protected void doRun() {
		if (minPartSize < 1) {
			minPartSize = set.size() * minPartSize;
		}
		getEnv().debug(1, this.repr());
		
		if (tree == null) {
			tree = new PartitionRuleTree();
			partIdx = new int[set.size()];
			maxFitness = new double[treeSize];
			Arrays.fill(maxFitness, Double.NEGATIVE_INFINITY);
			optRule = new PartitionRule[treeSize];
		}
		
		while (tree.size() <= treeSize) { 			
			for (int p = this.currentPart; p < tree.size(); this.currentPart = ++p) {
				getEnv().debug(1, "");
				getEnv().debug(1, Messages.format("tree.part", p + 1, tree.size()));
				
				if (maxFitness[p] > Double.NEGATIVE_INFINITY) {
					getEnv().debug(1, Messages.format("tree.opt_rule",
							p + 1, optRule[p], maxFitness[p]));
					continue;
				}				
				
				boolean b[] = new boolean[set.size()];
				for (int i = 0; i < b.length; i++) {
					b[i] = (partIdx[i] == p);
				}
				
				// Currently viewed part of the set
				SequenceSet partSet = set.filter(b);
				List<PartitionRule> rules = inferRules(partSet);
				save();
				
				Map<PartitionRule, Double> fitness = getFitness(partSet, rules);
				double partMax = Double.NEGATIVE_INFINITY;
				PartitionRule partMaxRule = null;
				
				for (Map.Entry<PartitionRule, Double> entry : fitness.entrySet()) { 
					if (entry.getValue() > partMax) {
						partMax = entry.getValue();
						partMaxRule = entry.getKey();
					}
				}
				maxFitness[p] = partMax;
				optRule[p] = partMaxRule;
				getEnv().debug(1, Messages.format("tree.opt_rule",
						p + 1, optRule[p], maxFitness[p]));
				
				rulesInferred = false;
				partRules.clear();
				ruleFitness.clear();
				save();
			}
			
			addNewRule();
			this.currentPart = 0;
			save();
		}
	}
	
	/**
	 * Добавляет новый предикат в дерево.
	 */
	private void addNewRule() {
		int maxPart = -1;
		double overallMax = Double.NEGATIVE_INFINITY;
		for (int p = 0; p < tree.size(); p++) {
			if (maxFitness[p] > overallMax) {
				overallMax = maxFitness[p];
				maxPart = p;
			}
		}
		
		getEnv().debug(1, Messages.format("tree.g_opt_rule", 
				maxPart + 1, optRule[maxPart], overallMax));
		tree.add(optRule[maxPart], maxPart);
		
		boolean[] selector = new boolean[set.size()];
		for (int i = 0; i < selector.length; i++)
			selector[i] = (partIdx[i] == maxPart);
		SequenceSet partSet = set.filter(selector);
		boolean[] partSelector = optRule[maxPart].test(partSet);
		for (int i = 0, ptr = 0; i < selector.length; i++)
			if (selector[i]) { 
				selector[i] = partSelector[ptr];
				ptr++;
			}
		int count = 0;
		for (int i = 0; i < selector.length; i++)
			if (selector[i]) {
				partIdx[i] = tree.size() - 1;
				count++;
			}
		maxFitness[maxPart] = Double.NEGATIVE_INFINITY;
		optRule[maxPart] = null;
		
		getEnv().debug(1, Messages.format("tree.new_part", 
				count, partSelector.length, 1.0 * count/partSelector.length) + "\n");
		
		saveTree();
	}
	
	/**
	 * Сохраняет дерево предикатов в файл.
	 */
	private void saveTree() {
		if (treeFile != null) {
			getEnv().debug(1, Messages.format("tree.save_tree", treeFile));
			try {
				getEnv().save(tree, treeFile);
			} catch (IOException e) {
				getEnv().error(1, Messages.format("tree.e_save_tree", e));
			}
		}
	}
	
	@Override
	public String repr() {
		String repr = "";
		repr += Messages.format("tree.rules", this.treeSize) + "\n";
		repr += Messages.format("tree.order", this.order) + "\n";
		repr += Messages.format("tree.percentages", Arrays.toString(this.percentages)) + "\n";
		repr += Messages.format("tree.min_part_size", this.minPartSize) + "\n";
		
		repr += Messages.format("misc.dataset", this.set.repr()) + "\n";
		repr += Messages.format("tree.bases", this.baseSets) + "\n";
		repr += Messages.format("tree.tree_file", this.treeFile);
		
		if (tree != null) {
			repr += "\n" + Messages.format("tree.tree", tree.repr());
		}
		return repr;
	}
}

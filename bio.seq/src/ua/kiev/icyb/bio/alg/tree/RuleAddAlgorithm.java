package ua.kiev.icyb.bio.alg.tree;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ua.kiev.icyb.bio.AbstractLaunchable;
import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.IOUtils;
import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Алгоритм фильтрации наборов цепочек наблюдаемых состояний для использования в предикатах
 * на основе последовательного наращивания размера множеств цепочек. По своей сути
 * аналогичен алгоритму последовательного добавления признаков (ADD).
 */
public class RuleAddAlgorithm extends AbstractLaunchable implements Representable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Максимальный размер множества цепочек состояний, рассматриваемый алгоритмом.
	 */
	public int maxSize;
	
	/**
	 * Максимальное количество оптимальных множеств каждой длины, на основе которых
	 * строятся множества большего размера.
	 */
	public int optCombinations;
	
	/**
	 * Порядок марковских цепей, используемый при вычислении функционала качества для
	 * множеств цепочек.
	 */
	public int order;
	
	/**
	 * Выборка, на основании которой считаются значения функционала качества.
	 */
	public SequenceSet set;
	
	/**
	 * Множество цепочек состояний, подмножества которого рассматриваются алгоритмом.
	 */
	public FragmentSet bases;
	
	/**
	 * Имя файла, в который сохраняются отобранные множества цепочек. Цепочки сохраняются
	 * после выполнения каждой итерации алгоритма.
	 */
	public String setsFile;
	
	/**
	 * Значения функционала качества для множеств цепочек.
	 * Множествам цепочек размером {@code i} соответствует {@code i}-й элемент массива.
	 */
	private Map<FragmentSet, Double>[] fitness = null;
	
	/**
	 * Текущий размер множеств цепочек.
	 */
	private int currentSize = 1;
	
	@SuppressWarnings("unchecked")
	protected void doRun() {
		Env.debug(1, reprHeader());
		
		RuleEntropy ruleEntropy = new RuleEntropy(set, order);
		
		if (fitness == null) {
			fitness = new Map[maxSize + 1];
			for (int i = 0; i < maxSize + 1; i++) {
				fitness[i] = new HashMap<FragmentSet, Double>();
			}
			fitness[0].put(new FragmentSet(set.observedStates(), bases.getFragmentLength()), 0.0);
		}
		
		for (int count = currentSize; count <= maxSize; count++) {
			this.currentSize = count;
			
			if (fitness[count].isEmpty()) {
				for (FragmentSet comb: fitness[count - 1].keySet())
					for (int base: bases) 
						if (!comb.contains(base)) {
							FragmentSet newComb = new FragmentSet(comb);
							newComb.add(base);
							fitness[count].put(newComb, Double.NaN);
						}
			}
			Env.debug(1, Messages.format("add.process", fitness[count].size(), count));
			
			evaluate(fitness[count], ruleEntropy);
			fitness[count] = trim(fitness[count], optCombinations);
			Env.debug(1, Messages.format("add.trimmed", fitness[count].keySet()));
			
			save();
			saveSets();
		}
	}
	
	/**
	 * Фильтрует набор множеств цепочек, оставляя обладающие наибольшим значением
	 * функционала качества.
	 * 
	 * @param data 
	 *    отображение, связывающее множества цепочек и значения функционала качества
	 * @param maxCount
	 *    максимальный размер сокращенного отображения
	 * @return 
	 *    сокращенное отображение, содержащее не более {@code maxCount} элементов
	 */
	private Map<FragmentSet, Double> trim(Map<FragmentSet, Double> data, int maxCount) {
		if (data.size() <= maxCount) 
			return data;
		
		Map<FragmentSet, Double> trimmedData = new HashMap<FragmentSet, Double>();
		Double[] values = data.values().toArray(new Double[0]);
		Arrays.sort(values);
		double threshold = values[values.length - maxCount];
		for (Entry<FragmentSet, Double> entry: data.entrySet())
			if (entry.getValue() >= threshold)
				trimmedData.put(entry.getKey(), entry.getValue());
		return trimmedData;
	}
	
	/**
	 * Вычисляет значения функционала качества для заданных цепочек.
	 * 
	 * @param entropy
	 *    объект, используемый для вычисления функционала
	 * @param combinations
	 *    отображение, связывающее множества цепочек и значения функционала качества;
	 *    множествам с невычисленным функционалом соответствуют значения {@link Double#NaN}. 
	 */
	private void evaluate(Map<FragmentSet, Double> combinations, RuleEntropy entropy) {
		ExecutorService executor = Env.executor();
		List<RunnableTask> tasks = new ArrayList<RunnableTask>();
		for (Map.Entry<FragmentSet, Double> entry: combinations.entrySet()) {
			if (entry.getValue().isNaN()) {
				tasks.add(new RunnableTask(entropy, entry));
			}
		}
		
		try {
			final List<Future<Void>> futures = executor.invokeAll(tasks);
			for (int i = 0; i < futures.size(); i++) {
				futures.get(i).get();
			}
		} catch (InterruptedException e) {
		} catch (ExecutionException e) {
		}
	}
	
	/**
	 * Возвращает текущие наборы цепочек состояний, отобранные алгоритмом.
	 * 
	 * @return
	 *    список наборов цепочек
	 */
	public List<FragmentSet> getSets() {
		List<FragmentSet> sets = new ArrayList<FragmentSet>();
		for (int size = 1; size <= this.currentSize; size++) {
			for (Map.Entry<FragmentSet, Double> entry : fitness[size].entrySet()) {
				if (!entry.getValue().isNaN()) {
					sets.add(entry.getKey());
				}
			}
		}
		
		return sets;
	}
	
	/**
	 * Сохраняет текущие наборы цепочек состояний, отобранные алгоритмом в файл
	 * {@link #setsFile}.
	 */
	private void saveSets() {
		if (setsFile != null) {
			try {
				Env.debug(1, Messages.format("add.save_sets", setsFile));
				IOUtils.writeObject(setsFile, (Serializable)getSets());
			} catch (IOException e) {
				Env.error(1, Messages.format("add.e_save_sets", e));
			}
		}
	}
	
	private static class RunnableTask implements Callable<Void> {

		private final RuleEntropy entropy;
		private final Map.Entry<FragmentSet, Double> entry;
		
		public RunnableTask(RuleEntropy entropy, Map.Entry<FragmentSet, Double> entry) {
			this.entropy = entropy;
			this.entry = entry;
		}
		
		@Override
		public Void call() throws Exception {
			final FragmentSet combination = entry.getKey();
			
			ContentPartitionRule rule = new ContentPartitionRule(combination, 0.0);
			rule.setThreshold(combination.median(entropy.getSet()));
			double difference = entropy.fitness(rule);
			Env.debug(2, Messages.format("misc.fitness", rule, difference));
			entry.setValue(difference);
			
			return null;
		}
	}
	
	/**
	 * Печатает сводку по параметрам алгоритма.
	 */
	private String reprHeader() {
		String repr = Messages.format("misc.dataset", set.repr()) + "\n";
		repr += Messages.format("add.bases", bases) + "\n";
		repr += Messages.format("add.order", order) + "\n";
		repr += Messages.format("add.max_size", maxSize) + "\n";
		repr += Messages.format("add.combs", optCombinations) + "\n";
		repr += Messages.format("add.curr_size", currentSize) + "\n";
		repr += Messages.format("add.sets_file", setsFile);
		
		return repr;
	}
	
	public String repr() {
		String repr = reprHeader() + "\n";
		
		if (fitness != null) {
			for (int i = 1; i <= currentSize; i++) {
				repr += Messages.format("add.size", i) + "\n";
				for (Map.Entry<FragmentSet, Double> entry : fitness[i].entrySet()) {
					final FragmentSet comb = entry.getKey();
					final Double fitness = entry.getValue();
					
					if (!fitness.isNaN()) {
						repr += Messages.format("misc.fitness", comb, fitness) + "\n";
					}
				}
				repr += "\n";
			}
		}
		
		return repr;
	}
}

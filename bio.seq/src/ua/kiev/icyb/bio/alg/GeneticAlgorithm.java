package ua.kiev.icyb.bio.alg;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ua.kiev.icyb.bio.AbstractLaunchable;
import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Простая реализация генетического алгоритма оптимизации.
 * 
 * <p>Генетический алгоритм работает по принципу эволюции живых организмов.
 * Элементы множества, на котором производится оптимизация, представляются
 * в виде {@linkplain Organism организмов}, для которых определены операции
 * мутации и скрещивания. На каждой итерации каждый организм из текущего множества организмов
 * (<em>поколения</em> или <em>популяции</em>) подвергается определенному
 * количеству обеих этих операций; результаты операций добавляются в популяцию. 
 * После этого определенное число организмов с наивысшим показателем функционала качества 
 * переходит в следующее поколение.
 * 
 * <p>Начальная популяция организмов задается из априорных соображений; во многих случаях
 * ее можно создавать случайным образом.
 */
public class GeneticAlgorithm extends AbstractLaunchable implements Representable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Класс, сопоставляющий организму его функционал качества.
	 * 
	 * Экземпляры класса сравнимы между собой; для сравнения используется значение функционала
	 * качества.
	 * 
	 * @param <T>
	 *    тип организмов, используемых в алгоритме оптимизации
	 */
	private static class FitnessRecord implements Comparable<FitnessRecord> {
		/** Значение функционала качества. */
		public final double fitness;
		/** Организм, для которого вычисляется функционал качества. */
		public final Organism organism;
		
		/**
		 * Создает новую запись для организма.
		 * 
		 * @param organism
		 * @param cache
		 */
		private FitnessRecord(Map.Entry<Organism, Double> mapEntry) {
			this.organism = mapEntry.getKey();
			this.fitness = mapEntry.getValue();
		}
		
		@Override
		public int compareTo(FitnessRecord other) {
			return -Double.compare(this.fitness, other.fitness);
		}
	}
	
	private static class FitnessTask implements Callable<Void> {
		private final Map.Entry<Organism, Double> entry;
		
		public FitnessTask(Map.Entry<Organism, Double> entry) {
			this.entry = entry;
		}
		
		@Override
		public Void call() throws Exception {
			entry.setValue(entry.getKey().fitness());
			return null;
		}
	}
	
	/**
	 * Число генерируемых алгоритмом поколений организмов.
	 */
	public int generations;
	
	/**
	 * Число скрещиваний организма с другими организмами того же поколения
	 * в пределах каждой итерации генетического алгоритма.
	 */
	public int crossovers;
	
	/**
	 * Число мутаций организма в пределах каждой итерации генетического алгоритма.
	 */
	public int mutations;
	
	/**
	 * Максимальный размер поколения организмов.
	 */
	public int maxSize;
	
	/**
	 * Вероятность <em>атомарной</em> мутации.
	 * 
	 * @see Organism#mutate(double)
	 */
	public double mutationP;
	
	/**
	 * Переключатель, определяющий, следует ли использовать для кэширования
	 * значений функционала качества карту со слабыми ссылками (класс {@link WeakHashMap}).
	 * Если значение параметра равно {@code false}, для кэширования используется 
	 * обычная хэш-таблица (класс {@link HashMap}).
	 */
	public boolean weakCache = false;
	
	/**
	 * Шаблон для сохранения поколений, получаемых алгоритмом. <code>{i}</code>
	 * заменяется на номер текущего поколения.
	 */
	public String saveTemplate = null;

	/**
	 * Начальная популяция организмов.
	 */
	public Collection<? extends Organism> initialPopulation;
	
	/**
	 * Текущая популяция организмов.
	 */
	private Set<Organism> population;
	
	/**
	 * Номер текущего поколения.
	 */
	private int generationIdx = 0;
	
	/**
	 * Полностью ли сформировано ли текущее поколение (т.е. выполнены ли операции скрещивания
	 * и мутации). 
	 */
	private boolean populationFormed = false;
	
	/**
	 * Отображения, связывающее организмы текущего поколения и их функционал качества.
	 * Значения {@link Double#NaN} соответствуют невычисленным значениям.
	 */
	private Map<Organism, Double> fitness;
	
	/**
	 * Создает новый генетический алгоритм.
	 */
	public GeneticAlgorithm() {
	}
	
	@Override
	protected void doRun() {
		if (population == null) {
			population = new HashSet<Organism>(initialPopulation);
			populationFormed = false;
		}
		// Create cache
		Map<Organism, Double> cache = weakCache ? 
				new WeakHashMap<Organism, Double>() : new HashMap<Organism, Double>();
		if (fitness == null) {
			fitness = new HashMap<Organism, Double>();
		}
		
		for (int t = this.generationIdx; t < generations; this.generationIdx = ++t) {
			getEnv().debug(1, Messages.format("gen.generation", t + 1));
			getEnv().debug(1, Messages.format("gen.pop_size", population.size()));
			getEnv().debug(2, Messages.format("gen.cache", cache.size()));
			
			if (!populationFormed) {
				// Crossbreed
				List<Organism> list = new ArrayList<Organism>(population);
				for (Organism item: list)
					for (int i = 0; i < crossovers; i++) {
						int index = (int)Math.floor(Math.random() * list.size());
						Organism other = list.get(index);
						population.add(item.crossover(other));
					};
				
				// Mutate
				list = new ArrayList<Organism>(population);
				for (Organism item: list)
					for (int i = 0; i < mutations; i++)
						population.add(item.mutate(mutationP));

				getEnv().debug(1, Messages.format("gen.new_pop_size", population.size()));
				onGenerationFormed(population);
				populationFormed = true;
			}
			
			// Choose most fitting items
			if (population.size() > maxSize) {
				getEnv().debug(1, Messages.getString("gen.filter"));
				
				int cached = 0;
				for (Organism item : population) {
					if (!fitness.containsKey(item)) {
						Double val = cache.get(item);
						if (val != null) {
							cached++;
						}
						fitness.put(item, (val == null) ? Double.NaN : val);
					}
				}
				
				
				ExecutorService executor = getEnv().executor();
				List<FitnessTask> tasks = new ArrayList<FitnessTask>();
				for (Map.Entry<Organism, Double> entry : fitness.entrySet()) {
					if (entry.getValue().isNaN()) {
						tasks.add(new FitnessTask(entry));
					}
				}
				getEnv().debug(1, Messages.format("gen.tasks", 
						tasks.size(), fitness.size() - tasks.size(), cached));
				
				try {
					final List<Future<Void>> futures = executor.invokeAll(tasks);
					for (Future<Void> future: futures) {
						future.get();
					}
				} catch (InterruptedException e) {
					getEnv().exception(e);
				} catch (ExecutionException e) {
					getEnv().exception(e);
				}
				
				population = trimPopulation(fitness, maxSize);
				cache.putAll(fitness);
				fitness.clear();
			}
			
			populationFormed = false;
			save();
			savePopulation();
		}
	}
	
	/**
	 * Уменьшает размер популяции, оставляее в ней организмы с наибольшими значениями
	 * функционала качества.
	 * 
	 * @param fitness
	 *    отображение, связывающее организмы и их функционал качества
	 * @param size
	 *    максимальный допустимый размер популяции
	 * @return
	 *    сокращенная популяция организмов
	 */
	private Set<Organism> trimPopulation(Map<Organism, Double> fitness, int size) {
		List<FitnessRecord> list = new ArrayList<FitnessRecord>();
		for (Map.Entry<Organism, Double> entry : fitness.entrySet()) {
			list.add(new FitnessRecord(entry));
		}
		Collections.sort(list);
		list = list.subList(0, size);
		
		Set<Organism> newPopulation = new HashSet<Organism>();
		for (FitnessRecord r : list) {
			newPopulation.add(r.organism);
		}
		return newPopulation;
	}
	
	/**
	 * Сохраняет текущую популяцию.
	 */
	private void savePopulation() {
		if (saveTemplate != null) {
			String filename = saveTemplate.replaceAll("\\{i\\}", "" + (this.generationIdx + 1));
			getEnv().debug(1, Messages.format("gen.save_pop", filename));
			try {
				getEnv().save((Serializable) this.population, filename);
			} catch (IOException e) {
				getEnv().debug(1, Messages.format("gen.e_save_pop", e));
			}
		}
	}
	
	/**
	 * Вызывается каждый раз после формирования нового поколения организмов.
	 * 
	 * @param population
	 *    сформированное поколение
	 */
	protected void onGenerationFormed(Set<Organism> population) {
	}

	@Override
	public String repr() {
		String repr = Messages.format("gen.generations", this.generations) + "\n";
		repr += Messages.format("gen.crossovers", this.crossovers) + "\n";
		repr += Messages.format("gen.mutations", this.mutations) + "\n";
		repr += Messages.format("gen.max_size", this.maxSize) + "\n";
		repr += Messages.format("gen.mutation_p", this.mutationP) + "\n";
		repr += Messages.format("gen.weak_cache", this.weakCache);
		
		if ((initialPopulation != null) && !initialPopulation.isEmpty()) {
			Organism item = initialPopulation.iterator().next();
			repr += "\n" + Messages.format("gen.init_pop", 
					initialPopulation.size(), item.getClass().getName());
		}
		
		if ((population != null) && !population.isEmpty()) {
			repr += "\n" + Messages.format("gen.curr_gen", generationIdx + 1) + "\n";
			
			Organism item = population.iterator().next();
			repr += Messages.format("gen.curr_pop", 
					population.size(), item.getClass().getName());
		}
		
		return repr;
	}
}

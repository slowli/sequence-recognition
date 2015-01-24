package ua.kiev.icyb.bio.alg.mixture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ua.kiev.icyb.bio.AbstractLaunchable;
import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Класс, реализующий EM-алгоритм для разделения смесей вероятностных распределений,
 * задаваемых цепями Маркова произвольного порядка 
 * (<a href="http://ru.wikipedia.org/wiki/EM-алгоритм">википедия</a>).
 */
public class EMAlgorithm extends AbstractLaunchable implements Representable {
	
	private static final long serialVersionUID = 1L;

	private static class MaximizationTask implements Callable<Void> {

		private final SequenceSet set;
		private final ChainMixture mixture;
		private final int index;
		private final double[] sampleWeights;
		
		public MaximizationTask(SequenceSet set, ChainMixture mixture, int index, double[] sampleW) {
			this.set = set;
			this.mixture = mixture;
			this.index = index;
			this.sampleWeights = sampleW;
		}
		
		@Override
		public Void call() throws Exception {
			mixture.chains[index].reset();
			mixture.chains[index].digestSet(set, sampleWeights);
			return null;
		}
	}
	
	/** 
	 * Использовать ли стохастическую модификацию EM-алгоритма.
	 * <p> 
	 * В стохастическом EM-алгоритме на этапе ожидания вместо решения задач максимизации 
	 * взвешенного правдоподобия решаются задачи максимизации обычного правдоподобия 
	 * для подмножеств выборки, вероятность входжения в которые для каждого образца выборки
	 * равна соответствующей апостериорной вероятности его генерации соответствующей 
	 * вероятностной моделью. Это делается, чтобы "выбить" алгоритм из точек локальных максимумов.
	 */
	public boolean stochastic = false;
	
	/** Число итераций EM-алгоритма. */
	public int nIterations = 10;
	
	/** 
	 * Шаблон названия файлов для сохранения композиций, полученных после каждой итерации алгоритма.
	 * Заменяемые символы:
	 * <table>
	 * <tr><th>{n}</th>
	 * <td>количество марковских моделей в композиции;</td></tr>
	 * <tr><th>{i}</th>
	 * <td>номер итерации с отсчетом от нуля.</td>
	 * </table> 
	 */
	public String saveTemplate = null;

	/**
	 * Выборка, с помощью которой производится построение смеси распределений.
	 */
	public SequenceSet set;
	
	/**
	 * Текущая смесь распределений.
	 */
	public ChainMixture mixture;
	
	private Random random = null;

	/**
	 * Текущий номер итерации алгоритма (с отсчетом от нуля).
	 */
	private int iteration;
	
	/**
	 * Создает новую копию алгоритма.
	 */
	public EMAlgorithm() {
	}
	
	/**
	 * Оптимизирует правдоподобие для взвешенной композиции марковских цепей,
	 * используя EM-алгоритм.
	 */
	public void ordinaryRun() {
		if (random == null)
			random = new Random();
		ExecutorService executor = getEnv().executor();
		
		final int count = mixture.size();
		
		double[][] weights = new double[0][0];
		
		for (int t = this.iteration; t < nIterations; this.iteration = ++t) {
			
			// Expectation step
			getEnv().debug(1, "\n" + Messages.format("em.e_step", t + 1));
			// XXX 
			//weights = mixture.getWeights(set);
			getEnv().debug(1, ChainMixture.reprDistribution(weights));
			
			// Maximization step			
			getEnv().debug(1, Messages.format("em.m_step", t + 1));
			ChainMixture newMixture = (ChainMixture) mixture.clearClone();
			
			List<MaximizationTask> tasks = new ArrayList<MaximizationTask>(); 
			for (int alg = 0; alg < count; alg++) {
				double[] sampleWeights = new double[set.length()];
				if (stochastic) {
					// Transform weights to the set {0, 1}
					double r = 0;
					for (int i = 0; i < set.length(); i++) {
						r = random.nextDouble();
						sampleWeights[i] = (r < weights[alg][i]) ? 1 : 0;
					}
				} else {
					System.arraycopy(weights[alg], 0, sampleWeights, 0, set.length());
				}
				tasks.add(new MaximizationTask(set, newMixture, alg, sampleWeights));
				
				double sum = 0;
				for (int i = 0; i < set.length(); i++) {
					sum += weights[alg][i];
				}
				newMixture.weights[alg] = sum / set.length();
			}
			
			try {
				for (Future<Void> future : executor.invokeAll(tasks)) {
					future.get();
				}
			} catch (InterruptedException e) {
				getEnv().exception(e);
			} catch (ExecutionException e) {
				getEnv().exception(e);
			}
			
			mixture = newMixture;
			getEnv().debug(1, mixture.repr());
			
			saveMixture();
			save();
		}
	}
	
	/**
	 * Сохраняет смесь распределений в файл, имя которого
	 * получается из шаблона {@link #saveTemplate}.
	 */
	protected void saveMixture() {
		if (saveTemplate != null) {
			String filename = saveTemplate
					.replaceAll("\\{n\\}", "" + mixture.size())
					.replaceAll("\\{i\\}", "" + (iteration + 1));
			getEnv().debug(2, Messages.format("em.save_comp", filename));
			
			try {
				getEnv().save(mixture, filename);
			} catch (IOException e) {
				getEnv().error(0, Messages.format("em.save_comp_error", e));
			}
		}
	}
	
	/**
	 * Обнуляет счетчик числа итераций, выполненных алгоритмом.
	 */
	protected void resetIteration() {
		this.iteration = 0;
	}

	public String repr() {
		String repr = "";
		repr += reprOptions();
		if (mixture != null) {
			repr += "\n" + Messages.format("misc.mixture", mixture.repr());
		}
		
		return repr;
	}
	
	/**
	 * Печатает сводку по параметрам алгоритма.
	 */
	protected String reprOptions() {
		String repr = "";
		repr += Messages.format("em.stochastic", stochastic) + "\n";
		repr += Messages.format("em.iterations", nIterations) + "\n";
		repr += Messages.format("em.template", saveTemplate) + "\n";
		return repr;
	}

	@Override
	protected void doRun() {
		getEnv().debug(1, repr());
		ordinaryRun();
	}
}

package ua.kiev.icyb.bio.alg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.alg.AbstractDistribution;
import ua.kiev.icyb.bio.alg.Distribution;

/**
 * Распределение с многопоточной реализацией некоторых методов.
 * 
 * @param <T>
 *    пространство объектов, на котором задано распределение
 */
public class ThreadedDistribution<T> extends AbstractDistribution<T> {

	private static final long serialVersionUID = 1L;
	

	/**
	 * Задача по оценке правдоподобия для конкретного прецедента.
	 */
	private class EstimateTask implements Callable<Double> {

		private final T point;
		
		public EstimateTask(T point) {
			this.point = point;
		}
		
		@Override
		public Double call() throws Exception {
			return ThreadedDistribution.this.estimate(point);
		}
	}
	
	/** Базовое вероятностное распределение. */
	private final Distribution<T> base;
	
	/** Окружение. */
	private final Env env;
	
	/**
	 * Создает распределение с многопоточной реализацией вычислений.
	 * 
	 * @param base
	 *    базовое распределение
	 * @param env
	 *    окружение, которое обеспечивает многопоточность
	 */
	public ThreadedDistribution(Distribution<T> base, Env env) {
		this.base = base;
		this.env = env;
	}
	
	@Override
	public void train(T sample, double weight) {
		this.base.train(sample, weight);
	}

	@Override
	public void reset() {
		this.base.reset();
	}

	@Override
	public double estimate(T point) {
		return this.base.estimate(point);
	}

	@Override
	public double estimate(Collection<? extends T> points) {
		List<EstimateTask> tasks = new ArrayList<EstimateTask>();
		for (T point : points) {
			tasks.add(new EstimateTask(point));
		}
		
		double logP = 0.0;
		try {
			for (Future<Double> f : env.executor().invokeAll(tasks)) {
				logP += f.get();
			}
		} catch (InterruptedException e) {
			env.exception(e);
		} catch (ExecutionException e) {
			env.exception(e);
		}
		
		return logP;
	}
}

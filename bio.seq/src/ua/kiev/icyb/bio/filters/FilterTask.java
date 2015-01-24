package ua.kiev.icyb.bio.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.Launchable;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;

/**
 * Фильтрация с использованием нескольких потоков выполнения.
 */
public class FilterTask implements Launchable {

	private static final long serialVersionUID = 1L;
	

	private class PassTask implements Callable<Boolean> {

		private final Sequence sequence;
		
		public PassTask(Sequence sequence) {
			this.sequence = sequence;
		}
		
		@Override
		public Boolean call() throws Exception {
			return FilterTask.this.baseFilter.eval(sequence);
		}
	}
	
	private final SequenceSet set;
	
	private final SequenceSet.Filter baseFilter;
	
	/**
	 * Результат фильтрации.
	 */
	public SequenceSet filtered;
	
	/**
	 * Вектор булевых величин, соответствующий прохождению фильтра строками выборки.
	 */
	public boolean[] selector;
	
	/**
	 * Создает новое задание фильтрации.
	 * 
	 * @param set
	 *    выборка, которую надо отфильтровать
	 * @param baseFilter
	 *    используемый фильтр
	 */
	public FilterTask(SequenceSet set, SequenceSet.Filter baseFilter) {
		this.set = set;
		this.baseFilter = baseFilter;
	}

	/**
	 * Вычисляет значение фильтра на всех строках выборки.
	 * 
	 * @param env
	 *    используемое окружение
	 * @return
	 *    вектор булевых величин, соответствующий прохождению фильтра строками выборки
	 */
	private boolean[] eval(Env env) {
		final ExecutorService executor = env.executor();
		
		List<PassTask> tasks = new ArrayList<PassTask>();
		for (int i = 0; i < set.length(); i++) {
			tasks.add(new PassTask( set.get(i) ));
		}

		boolean[] selector = new boolean[set.length()];
		
		try {
			List<Future<Boolean>> futures = executor.invokeAll(tasks);
			
			for (int i = 0; i < set.length(); i++) {
				selector[i] = futures.get(i).get();
			}	
		} catch (InterruptedException e) {
			env.exception(e);
		} catch (ExecutionException e) {
			env.exception(e);
		}
		
		return selector;
	}

	@Override
	public void run(Env env) {
		selector = this.eval(env);
		filtered = set.filter(selector);
	}

	@Override
	public Env getEnv() {
		return null;
	}
}

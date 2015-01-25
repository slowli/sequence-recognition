package ua.kiev.icyb.bio.alg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.JobListener;
import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Многопоточная имплементация интерфейса {@link SeqAlgorithm}.
 * Может строиться на основе любой однопоточной реализации алгоритма распознавания
 * скрытых состояний.
 */
public class ThreadedAlgorithm extends AbstractSeqAlgorithm {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Базовый алгоритм распознавания.
	 */
	private SeqAlgorithm baseAlgorithm;
	
	private final Env env;
	
	/**
	 * Создает алгоритм на основе заданного алгоритма распознавания с использованием
	 * нескольких рабочих потоков.
	 * 
	 * @param base
	 *    базовый алгоритм распознавания
	 * @param env
	 *    среда выполнения, которая предоставляет потоки
	 */
	public ThreadedAlgorithm(SeqAlgorithm base, Env env) {
		this.baseAlgorithm = base;
		this.env = env;
	}
	
	@Override
	public void train(Sequence sequence) {
		baseAlgorithm.train(sequence);
	}
	
	@Override
	public void train(SequenceSet set) {
		baseAlgorithm.train(set);
	}

	@Override
	public void reset() {
		baseAlgorithm.reset();
	}

	@Override
	public byte[] run(Sequence sequence) {
		// Вычисления на отдельных строках не распараллеливаются
		return baseAlgorithm.run(sequence);
	}
	
	private class SequenceTask implements Callable<byte[]> {

		private final SequenceSet set;
		private final int index;
		private final JobListener listener;
		
		public SequenceTask(SequenceSet set, int index, JobListener listener) {
			this.set = set;
			this.index = index;
			this.listener = listener;
		}
		
		@Override
		public byte[] call() throws Exception {
			byte[] result = baseAlgorithm.run(set.get(index));
			if (listener != null)
				listener.seqCompleted(index, result);
			return result;
		}
	}
	
	@Override
	public synchronized SequenceSet runSet(SequenceSet set) {
		return runSet(set, null);
	}

	@Override
	public synchronized SequenceSet runSet(SequenceSet set, final JobListener listener) {
		ExecutorService executor = env.executor();
		List<SequenceTask> tasks = new ArrayList<SequenceTask>();
		for (int i = 0; i < set.size(); i++)
			tasks.add(new SequenceTask(set, i, listener));
		
		EstimatesSet estimates = new EstimatesSet(set);
		
		try {
			List<Future<byte[]>> results = executor.invokeAll(tasks);
			for (int i = 0; i < set.size(); i++)
				estimates.put(i, results.get(i).get());
			if (listener != null)
				listener.finished();
			return estimates;
		} catch (InterruptedException e) {
			env.exception(e);
		} catch (ExecutionException e) {
			env.exception(e);
		}
		
		// Недостижимый код
		return null;
	}

	@Override
	public Object clearClone() {
		ThreadedAlgorithm other = (ThreadedAlgorithm) super.clearClone();
		other.baseAlgorithm = (SeqAlgorithm) baseAlgorithm.clearClone();
		return other;
	}
	
	@Override
	public String repr() {
		String repr = super.repr() + "\n";
		repr += Messages.format("alg.base", baseAlgorithm.repr());
		return repr;
	}
}

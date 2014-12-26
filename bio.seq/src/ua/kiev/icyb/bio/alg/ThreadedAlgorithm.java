package ua.kiev.icyb.bio.alg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ua.kiev.icyb.bio.JobListener;
import ua.kiev.icyb.bio.SeqAlgorithm;
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
	
	/**
	 * Число используемых потоков исполнения.
	 */
	private int nThreads;
	
	/**
	 * Создает алгоритм на основе заданного алгоритма распознавания с использованием
	 * фиксированного числа рабочих потоков.
	 * 
	 * @param base
	 *    базовый алгоритм распознавания
	 * @param threads
	 *    число потоков
	 */
	public ThreadedAlgorithm(SeqAlgorithm base, int threads) {
		this.baseAlgorithm = base;
		this.nThreads = threads;
	}
	
	@Override
	public void train(byte[] observed, byte[] hidden) {
		baseAlgorithm.train(observed, hidden);
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
	public byte[] run(byte[] sequence) {
		// Single sequence computations can't be threaded
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
			byte[] result = baseAlgorithm.run(set.observed(index));
			if (listener != null)
				listener.seqCompleted(index, result);
			return result;
		}
	}
	
	@Override
	public synchronized SequenceSet runSet(SequenceSet set) {
		return runSet(set, new DefaultJobListener());
	}

	@Override
	public synchronized SequenceSet runSet(SequenceSet set, final JobListener listener) {
		// TODO use Env.executor()?
		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		List<SequenceTask> tasks = new ArrayList<SequenceTask>();
		for (int i = 0; i < set.length(); i++)
			tasks.add(new SequenceTask(set, i, listener));
		EstimatesSet estimates = new EstimatesSet(set);
		try {
			List<Future<byte[]>> results = executor.invokeAll(tasks);
			for (int i = 0; i < set.length(); i++)
				estimates.put(i, results.get(i).get());
			if (listener != null)
				listener.finished();
			return estimates;
		} catch (InterruptedException e) {
			return estimates;
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		} finally {
			executor.shutdown();
		}
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
		repr += Messages.format("alg.threads", nThreads) + "\n";
		repr += Messages.format("alg.base", baseAlgorithm.repr());
		return repr;
	}
}

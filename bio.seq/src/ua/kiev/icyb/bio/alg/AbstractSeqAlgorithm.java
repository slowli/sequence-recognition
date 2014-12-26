package ua.kiev.icyb.bio.alg;

import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.JobListener;
import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Имплементация базовых методов интерфейса {@link SeqAlgorithm}, которые
 * являются общими для большинства алгоритмов распознавания скрытых последовательностей.
 * 
 * <p>Также этот класс включает в себя методы по выделению локальной по отношению к
 * нити исполнения памяти, полезные при работе с большими объемами данных.
 */
public abstract class AbstractSeqAlgorithm implements SeqAlgorithm {

	private static final long serialVersionUID = 1L;
	
	private transient Map<Thread, Object> memoryCache;
	
	@Override
	public abstract void train(byte[] observed, byte[] hidden);
	
	@Override
	public void train(SequenceSet set) {
		for (int i = 0; i < set.length(); i++) {
			this.train(set.observed(i), set.hidden(i));
		}
	}

	@Override
	public abstract void reset();

	@Override
	public abstract byte[] run(byte[] sequence);

	@Override
	public SequenceSet runSet(SequenceSet set) {
		// Default work listener is enough for most applications
		return runSet(set, new DefaultJobListener());
	}

	@Override
	public SequenceSet runSet(SequenceSet set, JobListener listener) {
		EstimatesSet est = new EstimatesSet(set);
		for (int i = 0; i < set.length(); i++) {
			if (Thread.interrupted()) break;
				
			est.put(i, run(set.observed(i)));
			listener.seqCompleted(i, est.hidden(i));
		}
		listener.finished();	
		return est;
	}
	
	/**
	 * Возвращает область памяти для использования в методе {@link #run(byte[])}.
	 * Если необходимо, память выделяется при помощи метода {@link #allocateMemory()}.
	 * Память выделяется заново для каждой нити исполнения, вызывающей этот метод, что
	 * позволяет безопасно его использовать при параллельных вычислениях.
	 * 
	 * @return
	 *    область памяти
	 */
	protected final Object getMemory() {
		if (memoryCache == null) {
			memoryCache = new HashMap<Thread, Object>();
		}
		
		Object mem = memoryCache.get(Thread.currentThread());
		if (mem == null) {
			mem = allocateMemory();
			memoryCache.put(Thread.currentThread(), mem);
		}
		return mem;
	}
	
	/**
	 * Выделяет память для использования в методе {@link #run(byte[])}.
	 * Подклассы, которым необходима память для вычислений, должны переопределять этот метод.
	 * 
	 * @return
	 *    выделенная область памяти
	 */
	protected Object allocateMemory() {
		return new Object();
	}
	
	@Override
	public Object clearClone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// Should not happen
			return null;
		}
	}
	
	@Override
	public String repr() {
		String repr = Messages.format("alg.class", this.getClass().getName());
		
		final String key = this.getClass().getName();
		if (Messages.contains(key)) {
			repr += "\n" + Messages.getString(key);
		}
		return repr;
	}
}

package ua.kiev.icyb.bio.alg;

import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.JobListener;
import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.Sequence;
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
	public abstract void train(Sequence sequence);
	
	@Override
	public void train(SequenceSet set) {
		for (Sequence sequence : set) {
			this.train(sequence);
		}
	}

	@Override
	public abstract void reset();
	
	@Override 
	public abstract byte[] run(Sequence sequence);

	@Override
	public SequenceSet runSet(SequenceSet set) {
		EstimatesSet est = new EstimatesSet(set);
		for (Sequence sequence : set) {
			if (Thread.interrupted()) break;
				
			est.put(sequence.index, run(sequence));
		}
		return est;
	}

	@Override
	public SequenceSet runSet(SequenceSet set, JobListener listener) {
		EstimatesSet est = new EstimatesSet(set);
		for (Sequence sequence : set) {
			if (Thread.interrupted()) break;
				
			Sequence estimated = est.put(sequence.index, run(sequence));
			listener.seqCompleted(estimated);
		}
		listener.finished();
		return est;
	}
	
	/**
	 * Возвращает область памяти для использования в методе {@link #run(Sequence)}.
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
	 * Выделяет память для использования в методе {@link #run(Sequence)}.
	 * Подклассы, которым необходима память для вычислений, должны переопределять этот метод.
	 * 
	 * @return
	 *    выделенная область памяти
	 */
	protected Object allocateMemory() {
		return new Object();
	}
	
	@Override
	public SeqAlgorithm clone() {
		try {
			return (SeqAlgorithm) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	@Override
	public SeqAlgorithm clearClone() {
		try {
			return (SeqAlgorithm) super.clone();
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

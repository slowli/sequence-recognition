package ua.kiev.icyb.bio.alg.tree;

import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.AbstractSeqAlgorithm;
import ua.kiev.icyb.bio.alg.ViterbiAlgorithm;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Алгоритм распознавания, использующий голосование по старшинству
 * среди нескольких базовых алгоритмов. 
 * 
 * <p>При подаче на вход определенной строки наблюдаемых состояний она распознается первым базовым алгоритмом;
 * в случае удачного распознавания возвращается его результат. Если же первый алгоритм отказался
 * от распознавания, строка подается на вход второго алгоритма, в случае его отказа — третьим и т.д.
 */
public class PriorityCompAlgorithm extends AbstractSeqAlgorithm {

	private static final long serialVersionUID = 1L;
	
	/** Минимальный порядок марковской цепи, используемой в алгоритме. */
	private int minOrder = -1;
	/** Максимальный порядок марковской цепи, используемой в алгоритме. */
	private int maxOrder = -1;
	
	/**
	 * Базовые алгоритмы, использующиеся для распознавания. 
	 */
	protected SeqAlgorithm[] algorithms;
	
	/**
	 * Создает новый алгоритм, использующий для голосования по старшинству 
	 * заданную последовательность базовых алгоритмов. Для эффективной работы
	 * голосования сложность базовых алгоритмов должна уменьшаться при увеличении
	 * индекса в последовательности.
	 * 
	 * @param algs
	 *    массив базовых алгоритмов распознавания
	 */
	public PriorityCompAlgorithm(SeqAlgorithm... algs) {
		this.algorithms = algs;
	}
	
	/**
	 * Создает новый алгоритм, использующий для голосования по старшинству
	 * {@linkplain ViterbiAlgorithm алгоритмы Витерби} с заданным порядком марковских цепей.
	 * Количество базовых алгоритмов составляет
	 * <pre>
	 * maxOrder - minOrder + 1
	 * </pre>
	 * 
	 * @param minOrder
	 *    минимальный порядок марковской цепи
	 * @param maxOrder
	 *    максимальный порядок марковской цепи
	 */
	public PriorityCompAlgorithm(int minOrder, int maxOrder) {
		algorithms = new SeqAlgorithm[maxOrder - minOrder + 1];
		for (int i = maxOrder; i >= minOrder; i--)
			algorithms[maxOrder - i] = new ViterbiAlgorithm(1, i);
		
		this.minOrder = minOrder;
		this.maxOrder = maxOrder;
	}

	@Override
	public void train(Sequence sequence) {
		for (SeqAlgorithm m: algorithms)
			m.train(sequence);
	}
	
	@Override
	public void train(SequenceSet set) {
		for (SeqAlgorithm m: algorithms)
			m.train(set);
	}

	@Override
	public void reset() {
		for (SeqAlgorithm m: algorithms)
			m.reset();
	}

	@Override
	public byte[] run(Sequence sequence) {
		for (SeqAlgorithm m: algorithms) {
			byte[] result = m.run(sequence);
			if (result != null) {
				return result;
			}
		}
		
		return null;
	}
	
	@Override
	public PriorityCompAlgorithm clearClone() {
		PriorityCompAlgorithm other = (PriorityCompAlgorithm) super.clearClone();
		other.algorithms = algorithms.clone();
		for (int i = 0; i < algorithms.length; i++) {
			other.algorithms[i] = (SeqAlgorithm) algorithms[i].clearClone();
		}
		return other;
	}
	
	@Override 
	public String toString() {
		return "[" + this.getClass().getSimpleName() + ": " + algorithms.length + " models]";
	}
	
	@Override
	public String repr() {
		String repr = super.repr();
		if (minOrder > 0) {
			repr += "\n" + Messages.format("alg.chain", 1, maxOrder) + "\n";
			repr += Messages.format("alg.approx", "priority", minOrder);	
		}
		return repr;
	}
}

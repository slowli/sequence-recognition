package ua.kiev.icyb.bio.alg;

import java.util.Arrays;

import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Алгоритм распознавания скрытых последовательностей на основе принципа
 * максимума правдоподобия с использованием динамического программирования.
 * По сути является модификацией алгоритма Витерби для предсказания оптимальной последовательности
 * скрытых состояний для обыкновенных скрытых марковских моделей.
 * 
 * @see MarkovChain
 */
public class ViterbiAlgorithm extends AbstractSeqAlgorithm {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Максимальная обрабатываемая длина последовательности.
	 */
	private static final int MAX_SEQ_LENGTH = 50000;
	
	private static class Memory {
		
		public final short[][] pointer;
		
		public Memory(int nHiddenTails) {
			this.pointer = new short[nHiddenTails][MAX_SEQ_LENGTH];
		}
	}

	/**
	 * Параметры марковской цепи, использующиеся в алгоритме распознавания.
	 */
	protected MarkovChain chain;
	
	/**
	 * Создает новый алгоритм распознавания с заданными параметрами вероятностной модели.
	 * 
	 * @param depLength
	 *    длина зависимой цепочки состояний, используемая в алгоритме
	 * @param order
	 *    порядок марковской цепи, используемый в алгоритме
	 * @param set
	 *    образец выборки, используемый для определения алфавитов наблюдаемых и скрытых
	 *    состояний
	 */
	public ViterbiAlgorithm(int depLength, int order, SequenceSet set) {
		this(new MarkovChain(depLength, order, 
				set.observedStates(), set.hiddenStates()));
	}
	
	/**
	 * Создает новый алгоритм с заданной вероятностной моделью.
	 * 
	 * @param chain
	 *    марковская цепь, используемая в алгоритме
	 */
	public ViterbiAlgorithm(MarkovChain chain) {
		this.chain = chain;
	}
	
	@Override
	public void train(byte[] observed, byte[] hidden) {
		chain.digest(observed, hidden);
	}
	
	@Override
	public void train(SequenceSet set) {
		chain.digestSet(set);
	}

	@Override
	public void reset() {
		chain.reset();
	}

	@Override
	public byte[] run(byte[] seq) {
		return run(seq, this.chain);
	}
	
	/**
	 * Определяет наиболее вероятную последовательность скрытых состояний с использованием
	 * заданного вероятностного распределения.
	 * 
	 * @param seq
	 *    строка наблюдаемых состояний
	 * @param chain
	 *    марковская цепь, которая задает начальные и переходные вероятности, используемые в алгоритме
	 *    оптимизации
	 * @return
	 *    цепочка скрытых состояний, наиболее вероятная для заданной вероятностной модели;
	 *    {@code null} в случае отказа от распознавания
	 */
	protected byte[] run(byte[] seq, MarkovChain chain) {
		if (seq.length < chain.order()) return null;
		
		final int order = chain.order(), 
			depLength = chain.depLength(),
			nHiddenStates = chain.hiddenStates().length();
		int nHiddenHeads = 1;
		for (int i = 0; i < chain.depLength(); i++) {
			nHiddenHeads *= nHiddenStates;
		}
		int nHiddenTails = 1;
		for (int i = 0; i < chain.order(); i++) {
			nHiddenTails *= nHiddenStates;
		}

		final FragmentFactory factory = chain.factory();
		
		Memory mem = (Memory) getMemory();
		
		double curProb[] = new double[nHiddenTails], nextProb[] = new double[nHiddenTails]; 
		short pointer[][] = mem.pointer; 
		// Обрезать последовательность
		int trimmedLength = ((seq.length - order) / depLength) * depLength + order; 

		// Инициализировать промежуточные массивы
		for (int i = 0; i < nHiddenTails; i++) {
			Fragment state = chain.factory.fragment(seq, i, 0, order);
			curProb[i] = Math.log(Math.max(chain.getInitialP(state), 0));
		}
		
		int ptrIdx = 0; // текущая позиция в массиве указателей
		for (int pos = order; pos <= trimmedLength - depLength; pos += depLength) {
			Arrays.fill(nextProb, Double.NEGATIVE_INFINITY);
			
			for (int i = 0; i < nHiddenHeads; i++) {
				Fragment newState = factory.fragment(seq, i, pos, depLength);	
				
				for (short j = 0; j < nHiddenTails; j++) {
					Fragment tailState = factory.fragment(seq, j, pos - order, order);
					double val = Math.log(Math.max(0, chain.getTransP(tailState, newState))) 
							+ curProb[j];
					int idx = factory.shift(tailState, newState);
					
					if (nextProb[idx] < val) {
						nextProb[idx] = val;
						pointer[idx][ptrIdx] = j;
					}
				}
			}
			System.arraycopy(nextProb, 0, curProb, 0, nextProb.length);
			ptrIdx++;
		}
		
		// Обратный шаг алгоритма	
		double maxProb = Double.NEGATIVE_INFINITY;
		int maxPtr = -1;
		
		for (int i = 0; i < nHiddenTails; i++)
			if (curProb[i] > maxProb) {
				maxProb = curProb[i];				
				maxPtr = i;
			}
		if (maxPtr == -1) {
			return null;
		}
		
		byte[] result = new byte[seq.length];
		for (int pos = trimmedLength; pos > order; pos -= depLength) {
			// XXX проверить, работает ли для depLength > 1
			result[pos - 1] = (byte)(maxPtr % nHiddenHeads); 
			maxPtr = pointer[maxPtr][ptrIdx - 1];			
			ptrIdx--;
		}
		insertStates(result, nHiddenStates, maxPtr, 0, order);

		return result;
	}
	
	/**
	 * Превращает индекс последовательности скрытых состояний в саму эту последовательность
	 * и вставляет ее в заданное место массива.
	 * 
	 * @param array
	 *    массив, куда следует вставлять скрытые состояния
	 * @param nStates
	 *    размер множества скрытых состояний
	 * @param idx
	 *    индекс последовательности скрытых состояний среди всех последовательностей 
	 *    фиксированной длины
	 * @param start
	 *    начальная позиция для вставки
	 * @param length
	 *    длина последовательности
	 */
	protected static void insertStates(byte[] array, int nStates, int idx, int start, int length) {
		for (int i = 0; i < length; i++) {
			array[start + length - 1 - i] = (byte)(idx % nStates);
			idx /= nStates;
		}
	}
	
	@Override
	public Object clearClone() {
		ViterbiAlgorithm other = (ViterbiAlgorithm) super.clearClone();
		other.chain = (MarkovChain) other.chain.clearClone();
		return other;
	}
	
	@Override
	public String repr() {
		String repr = super.repr() + "\n";
		repr += Messages.format("alg.chain", chain.depLength(), chain.order());
		return repr;
	}
	
	@Override
	public String toString() {
		return String.format("[%s: %s]", this.getClass().getSimpleName(), chain.toString());
	}
	
	@Override
	protected Object allocateMemory() {
		int nHiddenTails = 1;
		for (int i = 0; i < chain.order(); i++) {
			nHiddenTails *= chain.hiddenStates().length();
		}
		
		return new Memory(nHiddenTails);
	}
}

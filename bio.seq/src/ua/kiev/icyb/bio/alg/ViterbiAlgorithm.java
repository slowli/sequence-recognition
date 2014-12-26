package ua.kiev.icyb.bio.alg;

import java.io.IOException;
import java.io.ObjectInputStream;
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
	
	private class Memory {
		public short[][] pointer = new short[nHiddenTails][MAX_SEQ_LENGTH];
	}

	/**
	 * Параметры марковской цепи, использующиеся в алгоритме распознавания.
	 */
	protected MarkovChain chain;
	
	/**
	 * Фабрика для фрагментов полных состояний.
	 */
	protected transient FragmentFactory factory;
	
	/** Порядок марковской цепи, используемый в алгоритме. */
	protected transient int order;
	
	/** Длина зависимой цепочки состояний, используемая в алгоритме. */
	protected transient int depLength;
	
	/** Размер алфавита скрытых состояний. */
	private transient int nHiddenStates;
	
	/**
	 * Число возможных скрытых состояний для цепочки длины {@link #depLength}.
	 */
	protected transient int nHiddenHeads;
	
	/**
	 * Число возможных скрытых состояний для цепочки длины {@link #order}.
	 */
	protected transient int nHiddenTails;
	
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
		initialize(chain);
	}
	
	private void initialize(MarkovChain chain) {
		this.chain = chain;
		this.depLength = chain.depLength();
		this.order = chain.order();
		this.nHiddenStates = chain.hiddenStates().length();
		this.factory = new FragmentFactory(chain.observedStates(), chain.hiddenStates(), 
				order + depLength);
		
		nHiddenHeads = 1;
		for (int i = 0; i < depLength; i++)
			nHiddenHeads *= nHiddenStates;
		nHiddenTails = 1;
		for (int i = 0; i < order; i++)
			nHiddenTails *= nHiddenStates;
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
		if (seq.length < order) {
			return null;
		}
		
		Memory mem = (Memory) getMemory();
		
		double curProb[] = new double[nHiddenTails], nextProb[] = new double[nHiddenTails]; 
		short pointer[][] = mem.pointer; 
		// Trim the sequence
		int trimmedLength = ((seq.length - order) / depLength) * depLength + order; 

		// Initialize arrays
		for (int i = 0; i < nHiddenTails; i++) {
			Fragment state = factory.fragment(seq, i, 0, order);
			curProb[i] = Math.log(Math.max(chain.getInitialP(state), 0));
		}
		
		int ptrIdx = 0; // position of the current token in the pointer array
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
		
		// Reverse propagation		
		double maxProb = Double.NEGATIVE_INFINITY;
		int maxPtr = -1; // !!!
		
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
			result[pos - 1] = (byte)(maxPtr % nHiddenHeads); //!!!
			maxPtr = pointer[maxPtr][ptrIdx - 1];			
			ptrIdx--;
		}
		insertStates(result, maxPtr, 0, order);

		// Restore trimmed sequence to the full one; assume that tailing chars correspond to exon
		return result;
	}
	
	/**
	 * Превращает индекс последовательности скрытых состояний в саму эту последовательность
	 * и вставляет ее в заданное место массива.
	 * 
	 * @param array
	 *    массив, куда следует вставлять скрытые состояния
	 * @param idx
	 *    индекс последовательности скрытых состояний среди всех последовательностей 
	 *    фиксированной длины
	 * @param start
	 *    начальная позиция для вставки
	 * @param length
	 *    длина последовательности
	 */
	protected void insertStates(byte[] array, int idx, int start, int length) {
		for (int i = 0; i < length; i++) {
			array[start + length - 1 - i] = (byte)(idx % nHiddenStates);
			idx /= nHiddenStates;
		}
	}
	
	/**
	 * Восстанавливает поля объекта, которые не записываются в поток, 
	 * на основе сохраненных полей.
	 * 
	 * @param stream
	 *    поток для считывания объекта
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream stream) 
			throws IOException, ClassNotFoundException {
		
		stream.defaultReadObject();
		initialize(this.chain);
	}
	
	@Override
	public Object clearClone() {
		ViterbiAlgorithm other = (ViterbiAlgorithm) super.clearClone();
		other.chain = (MarkovChain) other.chain.clearClone();
		other.initialize(other.chain);
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
		return new Memory();
	}
}

package ua.kiev.icyb.bio.alg;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.StatesDescription;
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
		
		@SuppressWarnings("unchecked")
		public final Map<Integer, Integer>[] pointer = new Map[MAX_SEQ_LENGTH];
	}

	/**
	 * Параметры марковской цепи, использующиеся в алгоритме распознавания.
	 */
	protected MarkovChain chain;
	
	
	protected int depLength;
	
	/**
	 * Порядок модели, используемой алгоритмом. 
	 */
	protected int order;
	
	/**
	 * Создает новый алгоритм распознавания с заданными параметрами вероятностной модели.
	 * 
	 * @param depLength
	 *    длина зависимой цепочки состояний, используемая в алгоритме
	 * @param order
	 *    порядок марковской цепи, используемый в алгоритме
	 */
	public ViterbiAlgorithm(int depLength, int order) {
		this.depLength = depLength;
		this.order = order;
	}
	
	/**
	 * Создает новый алгоритм.
	 */
	protected ViterbiAlgorithm() {
	}
	
	/**
	 * Создает марковскую модель на основе предоставленной обучающей выборки.
	 * Созданная модель используется в алгоритме Витерби для получения сведений 
	 * о начальных и переходных вероятностях.
	 * 
	 * @param states
	 * @return
	 *    созданная модель
	 */
	protected MarkovChain createChain(StatesDescription states) {
		return new MarkovChain(depLength, order, states);
	}
	
	@Override
	public void train(Sequence sequence) {
		if (chain == null) {
			chain = this.createChain(sequence.states());
		}
		chain.train(sequence);
	}
	
	@Override
	public void train(Collection<? extends Sequence> set) {
		if (chain == null) {
			chain = this.createChain(((SequenceSet) set).states());
		}
		chain.train(set);
	}

	@Override
	public void reset() {
		if (chain != null) chain.reset();
	}

	@Override
	public byte[] run(Sequence sequence) {
		return run(sequence.observed, this.chain);
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
		if (chain == null) return null;
		if ((seq.length < chain.order()) || (seq.length > MAX_SEQ_LENGTH)) return null;
		
		final int order = chain.order(), 
			depLength = chain.depLength(),
			nHiddenStates = chain.states().nHidden();
		int nHiddenHeads = 1;
		for (int i = 0; i < chain.depLength(); i++) {
			nHiddenHeads *= nHiddenStates;
		}
		int nHiddenTails = 1;
		for (int i = 0; i < chain.order(); i++) {
			nHiddenTails *= nHiddenStates;
		}

		final FragmentFactory factory = chain.factory();
		Fragment tail = factory.fragment(), head = factory.fragment(), shifted = factory.fragment();
		
		Memory mem = (Memory) getMemory();
		
		double[] curProb = new double[nHiddenTails], nextProb = new double[nHiddenTails]; 
		Map<Integer, Integer>[] pointer = mem.pointer;
		// Обрезать последовательность
		int trimmedLength = ((seq.length - order) / depLength) * depLength + order; 
		
		// Инициализировать промежуточные массивы
		for (int i = 0; i < nHiddenTails; i++) {
			factory.fragment(seq, i, 0, order, tail);
			curProb[i] = Math.log(Math.max(chain.getInitialP(tail), 0));
		}
		
		int ptrIdx = 0; // текущая позиция в массиве указателей
		for (int pos = order; pos <= trimmedLength - depLength; pos += depLength) {
			Arrays.fill(nextProb, Double.NEGATIVE_INFINITY);
			if (pointer[ptrIdx] == null) {
				pointer[ptrIdx] = new HashMap<Integer, Integer>();
			}
			pointer[ptrIdx].clear();
			
			for (int i = 0; i < nHiddenHeads; i++) {
				factory.fragment(seq, i, pos, depLength, head);
				
				for (int j = 0; j < nHiddenTails; j++) {
					if (curProb[j] == Double.NEGATIVE_INFINITY) continue;
					
					factory.fragment(seq, j, pos - order, order, tail);
					double val = Math.log(Math.max(0, chain.getTransP(tail, head))) 
							+ curProb[j];
					if (val == Double.NEGATIVE_INFINITY) continue;
					
					tail.append(head, shifted);
					shifted.suffix(tail.length, shifted);
					int idx = shifted.hidden;
					
					if (nextProb[idx] < val) {
						nextProb[idx] = val;
						pointer[ptrIdx].put(idx, j);
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
			maxPtr = pointer[ptrIdx - 1].get(maxPtr);
			pointer[ptrIdx - 1].clear();
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
	public ViterbiAlgorithm clearClone() {
		ViterbiAlgorithm other = (ViterbiAlgorithm) super.clone();
		if (other.chain != null) {
			other.chain = (MarkovChain) other.chain.clearClone();
		}
		return other;
	}
	
	@Override
	public String repr() {
		String repr = super.repr() + "\n";
		repr += Messages.format("alg.chain", this.depLength, this.order);
		return repr;
	}
	
	@Override
	public String toString() {
		if (chain != null) {
			return String.format("<%s(%s)>", this.getClass().getSimpleName(), chain);
		} else {
			return String.format("<%s(%d, %d)>", this.getClass().getSimpleName(), this.depLength, this.order);
		}
	}
	
	@Override
	protected Object allocateMemory() {
		return new Memory();
	}
}

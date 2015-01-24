package ua.kiev.icyb.bio.alg;

import java.util.Arrays;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Адаптация алгоритма распознавания скрытых последовательностей для распознавания
 * фрагментов генов (экзонов и интронов). Как и в более общем случае, рапознавание
 * производится на основе принципа максимума правдоподобия с использованием модифицированного
 * алгоритма Витерби.
 * 
 * <p>Отличительными особенностями алгоритма по сравнению с алгоритмом, реализованным 
 * в классе {@link ViterbiAlgorithm}, являются:
 * <ul>
 * <li>использование априорных сведений о концевой последовательности скрытых состояний -
 * полагается, что последние скрытые состояния всегда соответствуют последнему экзону;
 * <li>суммарная длина экзонных нуклеотидов может проверяться на кратность трем (т.е. проверяется
 * соответствие целому числу кодонов).
 * </ul> 
 */
public class GeneViterbiAlgorithm extends ViterbiAlgorithm {
	
	private static final long serialVersionUID = 1L;
	
	private boolean validateCds;
	
	/**
	 * Создает новый алгоритм распознавания с заданными параметрами вероятностной модели.
	 * Длина зависимой последовательности состояний полагается равной 1.
	 * 
	 * @param order
	 *    порядок марковской цепи, используемый в алгоритме
	 * @param validateCds
	 *    следует ли вводить ограничение на суммарную длину скрытых состояний,
	 *    соответствующих экзонам, так чтобы она была кратна трем
	 */
	public GeneViterbiAlgorithm(int order, boolean validateCds) {
		super(1, order);
		this.validateCds = validateCds;
	}
	
	@Override
	public byte[] run(Sequence sequence) {
		return run(sequence.observed, validateCds, this.chain);
	}
	
	@Override
	protected byte[] run(byte[] sequence, MarkovChain chain) {
		return run(sequence, validateCds, chain);
	}
	
	/**
	 * Производит распознавание на отдельной строке наблюдаемых состояний.
	 * 
	 * @param seq
	 *      строка наблюдаемых состояний
	 * @param validateCds
	 *    следует ли вводить ограничение на суммарную длину скрытых состояний,
	 *    соответствующих экзонам, так чтобы она была кратна трем
	 * @param chain
	 *    марковская цепь, которая задает начальные и переходные вероятности, используемые в алгоритме
	 *    оптимизации
	 * @return 
	 *    последовательность скрытых состояний, соответствующая наблюдаемой строке;
	 *    {@code null} в случае отказа от распознавания
	 */
	protected byte[] run(byte[] seq, boolean validateCds, MarkovChain chain) {
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
		
		final int codonLength = validateCds ? 3 : 1;
		double curProb[][] = new double[codonLength][nHiddenTails], nextProb[][] = new double[codonLength][nHiddenTails]; 
		short pointer[][][] = new short[codonLength][nHiddenTails][seq.length / depLength];
		// Обрезать последовательность
		int trimmedLength = ((seq.length - order) / depLength) * depLength + order; 

		// Инициализировать промежуточные массивы
		for (int rem = 0; rem < codonLength; rem++)
			Arrays.fill(curProb[rem], Double.NEGATIVE_INFINITY);
		for (int i = 0; i < nHiddenTails; i++) {
			curProb[exonCharCount(i, order) % codonLength][i] = Math.log(chain.getInitialP(
					factory.fragment(seq, i, 0, order)));
		}
		
		int ptrIdx = 0; // текущая позиция в массиве указателей
		for (int pos = order; pos <= trimmedLength - depLength; pos += depLength)
		{
			for (int rem = 0; rem < codonLength; rem++)
				Arrays.fill(nextProb[rem], Double.NEGATIVE_INFINITY);
			
			for (int i = 0; i < nHiddenHeads; i++)
			{
				Fragment newState = factory.fragment(seq, i, pos, depLength);
				int headRem = exonCharCount(i, depLength) % codonLength;
				
				for (short j = 0; j < nHiddenTails; j++) {
					Fragment tailState = factory.fragment(seq, j, pos - order, order);
					double val = Math.log(chain.getTransP(tailState, newState));
					int idx = factory.shift(tailState, newState);
					
					for (int rem = 0; rem < codonLength; rem++)
						if (nextProb[(rem + headRem) % codonLength][idx] < val + curProb[rem][j]) {
							nextProb[(rem + headRem) % codonLength][idx] = val + curProb[rem][j];
							pointer[(rem + headRem) % codonLength][idx][ptrIdx] = j;
					}
				}
			}
			
			for (int rem = 0; rem < codonLength; rem++)
				curProb[rem] = Arrays.copyOf(nextProb[rem], nextProb[rem].length);
			ptrIdx++;
		}
		
		// Обратный шаг алгоритма
		int rem = (- seq.length + trimmedLength) % codonLength;
		if (rem < 0) rem += codonLength;
		double maxProb = Double.NEGATIVE_INFINITY;
		int maxPtr = -1;
		for (int i = 0; i < nHiddenTails; i++)
			if (curProb[rem][i] > maxProb) {
				maxProb = 0;//curProb[rem][i];				
				maxPtr = i;
			}
		if (maxPtr == -1) return null;
		
		byte[] result = new byte[seq.length];
		for (int pos = trimmedLength; pos > order; pos -= depLength) {
			result[pos - 1] = (byte)(maxPtr % nHiddenHeads);
			int headRem = exonCharCount(maxPtr % (1 << depLength), depLength);
			maxPtr = pointer[rem][maxPtr][ptrIdx - 1];
			
			rem = (rem - headRem) % codonLength;
			if (rem < 0) rem += codonLength;
			ptrIdx--;
		}
		insertStates(result, nHiddenStates, maxPtr, 0, order);

		return result;
	}
	
	/**
	 * Подсчитывает число скрытых состояний, соответствующих экзонам, в строке,
	 * заданной порядковым номером ее среди всех последовательностей скрытых состояний
	 * фиксированной длины.
	 * 
	 * @param num
	 *    индекс последовательности среди последовательностей скрытых состояний
	 *    фиксированной длины
	 * @param len
	 *    длина последовательности
	 * @return
	 *    количество экзонных нуклеотидов в строке
	 */
	private static int exonCharCount(int num, int len) {
		int count = 0;
		for (int i = 0; i < len; i++) {
			count += (1 - num % 2);
			num /= 2;
		}
		return count;
	}
	
	@Override
	public String repr() {
		String repr = super.repr() + "\n";
		repr += Messages.format("alg.validate_cds", validateCds);
		return repr;
	}
}

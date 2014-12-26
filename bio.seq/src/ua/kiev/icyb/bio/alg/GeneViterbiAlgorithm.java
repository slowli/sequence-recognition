package ua.kiev.icyb.bio.alg;

import java.util.Arrays;

import ua.kiev.icyb.bio.SequenceSet;
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
	 * @param set
	 *    образец выборки, используемый для определения алфавитов наблюдаемых и скрытых
	 *    состояний
	 */
	public GeneViterbiAlgorithm(int order, boolean validateCds, SequenceSet set) {
		super(1, order, set);
		this.validateCds = validateCds;
	}
	
	/**
	 * Создает новый алгоритм с заданной вероятностной моделью.
	 * 
	 * @param chain
	 *    марковская цепь, используемая в алгоритме
	 * @param validateCds
	 *    следует ли вводить ограничение на суммарную длину скрытых состояний,
	 *    соответствующих экзонам, так чтобы она была кратна трем
	 */
	public GeneViterbiAlgorithm(MarkovChain chain, boolean validateCds) {
		super(chain);
		this.validateCds = validateCds;
	}
	
	@Override
	public byte[] run(byte[] sequence) {
		return run(sequence, validateCds);
	}
	
	/**
	 * Производит распознавание на отдельной строке наблюдаемых состояний.
	 * 
	 * @param seq
	 *      строка наблюдаемых состояний
	 * @param validateCds
	 *    следует ли вводить ограничение на суммарную длину скрытых состояний,
	 *    соответствующих экзонам, так чтобы она была кратна трем
	 * @return 
	 *    последовательность скрытых состояний, соответствующая наблюдаемой строке;
	 *    {@code null} в случае отказа от распознавания
	 */
	public byte[] run(byte[] seq, boolean validateCds) {
		if (seq.length < order) {
			return null;
		}
		
		final int CODON_LENGTH = validateCds ? 3 : 1;
		double curProb[][] = new double[CODON_LENGTH][nHiddenTails], nextProb[][] = new double[CODON_LENGTH][nHiddenTails]; 
		short pointer[][][] = new short[CODON_LENGTH][nHiddenTails][seq.length / depLength];
		// Trim the sequence
		int trimmedLength = ((seq.length - order) / depLength) * depLength + order; 

		// Initialize arrays
		for (int rem = 0; rem < CODON_LENGTH; rem++)
			Arrays.fill(curProb[rem], Double.NEGATIVE_INFINITY);
		for (int i = 0; i < nHiddenTails; i++) {
			curProb[exonCharCount(i, order) % CODON_LENGTH][i] = Math.log(chain.getInitialP(
					factory.fragment(seq, i, 0, order)));
		}
		
		int ptrIdx = 0; // position of the current token in the pointer array
		for (int pos = order; pos <= trimmedLength - depLength; pos += depLength)
		{
			for (int rem = 0; rem < CODON_LENGTH; rem++)
				Arrays.fill(nextProb[rem], Double.NEGATIVE_INFINITY);
			
			for (int i = 0; i < nHiddenHeads; i++)
			{
				Fragment newState = factory.fragment(seq, i, pos, depLength);
				int headRem = exonCharCount(i, depLength) % CODON_LENGTH;
				
				for (short j = 0; j < nHiddenTails; j++) {
					Fragment tailState = factory.fragment(seq, j, pos - order, order);
					double val = Math.log(chain.getTransP(tailState, newState));
					int idx = factory.shift(tailState, newState);
					
					for (int rem = 0; rem < CODON_LENGTH; rem++)
						if (nextProb[(rem + headRem) % CODON_LENGTH][idx] < val + curProb[rem][j]) {
							nextProb[(rem + headRem) % CODON_LENGTH][idx] = val + curProb[rem][j];
							pointer[(rem + headRem) % CODON_LENGTH][idx][ptrIdx] = j;
					}
				}
			}
			
			for (int rem = 0; rem < CODON_LENGTH; rem++)
				curProb[rem] = Arrays.copyOf(nextProb[rem], nextProb[rem].length);
			ptrIdx++;
		}
		
		// Reverse propagation
		int rem = (- seq.length + trimmedLength) % CODON_LENGTH;
		if (rem < 0) rem += CODON_LENGTH;
		double maxProb = Double.NEGATIVE_INFINITY;
		int maxPtr = -1; // !!!
		for (int i = 0; i < nHiddenTails; i++)
			if (curProb[rem][i] > maxProb) {
				maxProb = 0;//curProb[rem][i];				
				maxPtr = i;
			}
		if (maxPtr == -1) {
			return null;
		}
		
		byte[] result = new byte[seq.length];
		for (int pos = trimmedLength; pos > order; pos -= depLength)
		{
			result[pos - 1] = (byte)(maxPtr % nHiddenHeads);
			int headRem = exonCharCount(maxPtr % (1 << depLength), depLength);
			maxPtr = pointer[rem][maxPtr][ptrIdx - 1];
			// Calculate new remainder
			rem = (rem - headRem) % CODON_LENGTH;
			if (rem < 0) rem += CODON_LENGTH;
			ptrIdx--;
		}
		insertStates(result, maxPtr, 0, order);

		// Restore trimmed sequence to the full one; assume that tailing chars correspond to exon
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

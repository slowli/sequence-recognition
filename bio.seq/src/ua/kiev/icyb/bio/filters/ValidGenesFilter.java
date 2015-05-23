package ua.kiev.icyb.bio.filters;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;

/**
 * Фильтр для выделения <em>обыкновенных</em> генов.
 * 
 * <p>Под обыкновенным геном подразумевается ген, отвечающий следующим условиям
 * <ul>
 *   <li>начинается с последовательности нуклеотидов <code>ATG</code>;
 *   <li>заканчивается нуклеотидами <code>TAA</code> или <code>TAG</code> или <code>TGA</code>;
 * </ul>
 * 
 * <p>Кроме того, фильтр может проверять условие, касающееся интронов:
 * <ul>
 *   <li>каждый интрон начинается с нуклеотидов <code>GT</code> и заканчивается нуклеотидами <code>AG</code>.
 * </ul>
 */
public class ValidGenesFilter implements SequenceSet.Filter {

	/**
	 * Следует ли проверять интроны генов.
	 */
	private final boolean validateIntrons;
	
	/**
	 * Создает фильтр, выделяющий обыкновенные гены.
	 * 
	 * @param validateIntrons
	 *    следует ли проверять нуклеотиды, начинающие и заканчивающие интроны
	 */
	public ValidGenesFilter(boolean validateIntrons) {
		this.validateIntrons = validateIntrons;
	}
	
	/**
	 * Сравнивает подстроку последовательности с эталоном.
	 * 
	 * @param symbols
	 *    последовательность состояний
	 * @param sample
	 *    эталонная строка состояний
	 * @param start
	 *    предполагаемое начало эталонной строки в последовательности
	 * @return
	 *    {@code true}, если эталон входит в строку состояний, начиная с заданной позиции {@code start}
	 */
	private boolean checkSubstring(byte[] symbols, byte[] sample, int start) {
		for (int pos = 0; pos < sample.length; pos++) {
			if (symbols[start + pos] != sample[pos]) return false;
		}
		return true;
	}
	
	/**
	 * Восстанавливает последовательность состояний по ее текстовому представлению.
	 * 
	 * @param text
	 *    текстовое представление состояний
	 * @param alphabet
	 *    алфавит состояний
	 * @return
	 *   восстановленный массив состояний
	 */
	private byte[] getSymbols(String text, String alphabet) {
		byte[] symbols = new byte[text.length()];
		for (int i = 0; i < text.length(); i++) {
			symbols[i] = (byte) alphabet.indexOf(text.charAt(i));
		}
		return symbols;
	}
	
	@Override
	public boolean eval(Sequence sequence) {
		final byte[] observed = sequence.observed;
		final String oStates = sequence.states().observed();
		
		final byte[] startSeq = getSymbols("ATG", oStates);
		final byte[] termSeq1 = getSymbols("TGA", oStates),
			termSeq2 = getSymbols("TAG", oStates),
			termSeq3 = getSymbols("TAA", oStates);
		
		boolean validStart = checkSubstring(observed, startSeq, 0);
		if (!validStart) return false;
		
		int len = observed.length;
		boolean validEnd = checkSubstring(observed, termSeq1, len - 3) 
				|| checkSubstring(observed, termSeq2, len - 3)
				|| checkSubstring(observed, termSeq3, len - 3);
		if (!validEnd) return false;
		
		if (!validateIntrons) return true;
		
		final byte[] intronStart = getSymbols("GT", oStates),
				intronEnd = getSymbols("AG", oStates);
		
		for (Sequence.Segment segment : sequence.segments()) {
			if (segment.stateChar() == 'i') {
				if (!checkSubstring(observed, intronStart, segment.start)) return false;
				if (!checkSubstring(observed, intronEnd, segment.end - 1)) return false;
			}
		}
		
		return true;
	}
}

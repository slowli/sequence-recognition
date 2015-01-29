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
 *   <li>каждый интрон начинается с нуклеотида <code>G</code> и заканчивается нуклеотидом <code>G</code>.
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
	
	@Override
	public boolean eval(Sequence sequence) {
		final byte[] observed = sequence.observed, hidden = sequence.hidden;
		
		boolean validStart = (observed[0] == 0) && (observed[1] == 3) && (observed[2] == 2);
		if (!validStart) return false;
		
		int len = observed.length;
		boolean validEnd = ((observed[len - 3] == 3) && (observed[len - 2] == 0) && (observed[len - 1] == 0))
				|| ((observed[len - 3] == 3) && (observed[len - 2] == 0) && (observed[len - 1] == 2))
				|| ((observed[len - 3] == 3) && (observed[len - 2] == 2) && (observed[len - 1] == 0));
		if (!validEnd) return false;
		
		if (!validateIntrons) return true;
		
		for (int pos = 1; pos < hidden.length - 1; pos++) {
			boolean intronStart = (hidden[pos - 1] == 0) && (hidden[pos] == 1);
			boolean intronEnd = (hidden[pos - 1] == 1) && (hidden[pos] == 0);
			
			if (intronStart && (observed[pos] != 2)) {
				return false;
			}
			if (intronEnd && (observed[pos - 1] != 2)) {
				return false;
			}
		}
		
		return true;
	}
}

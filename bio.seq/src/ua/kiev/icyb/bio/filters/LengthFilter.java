package ua.kiev.icyb.bio.filters;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;

/**
 * Фильтр, ограничивающий длину строк в выборке.
 */
public class LengthFilter implements SequenceSet.Filter {
	
	/**
	 * Минимальная приемлимая длина строки в выборке.
	 */
	private final int minLength;
	
	/**
	 * Максимальная приемлимая длина строки в выборке.
	 */
	private final int maxLength;
	
	/**
	 * Создает фильтр, ограничивающий длины строк в выборке.
	 * 
	 * @param minLength
	 *    минимальная приемлимая длина строки в выборке
	 * @param maxLength
	 *    максимальная приемлимая длина строки в выборке
	 */
	public LengthFilter(int minLength, int maxLength) {
		this.minLength = minLength;
		this.maxLength = maxLength;
	}
	
	/**
	 * Создает фильтр, ограничивающий максимальную длину строк в выборке.
	 * 
	 * @param maxLength
	 *    максимальная приемлимая длина строки в выборке
	 */
	public LengthFilter(int maxLength) {
		this(0, maxLength);
	}
	
	@Override
	public boolean eval(Sequence sequence) {
		return (sequence.length() >= minLength) && ((sequence.length() <= maxLength) || (maxLength <= 0));
	}

}

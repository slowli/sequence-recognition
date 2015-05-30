package ua.kiev.icyb.bio.filters;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;

/**
 * Фильтр, выбирающий строки случайным образом.
 */
public class RandomFilter implements SequenceSet.Filter {
	
	private double passP;
	
	/**
	 * Создает новый рандомизированный фильтр.
	 * 
	 * @param passP
	 *    вероятность прохождения фильтра произвольной строкой
	 */
	public RandomFilter(double passP) {
		this.passP = passP;
	}
	
	@Override
	public boolean eval(Sequence sequence) {
		return (Math.random() < this.passP);
	}

}

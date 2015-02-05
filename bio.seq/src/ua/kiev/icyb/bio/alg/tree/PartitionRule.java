package ua.kiev.icyb.bio.alg.tree;

import ua.kiev.icyb.bio.SequenceSet;


/**
 * Предикат, определенный на пространстве строк наблюдаемых состояний.
 */
public abstract class PartitionRule {

	/**
	 * Вычисляет значение предиката на заданной строке наблюдаемых состояний.
	 * 
	 * @param seq
	 *    последовательность наблюдаемых состояний
	 * @return
	 *    значение предиката
	 */
	public abstract boolean test(byte[] seq);

	/**
	 * Вычисляет значение предиката на заданном наборе строк наблюдаемых состояний.
	 * 
	 * @param set
	 *    набор последовательностей
	 * @return
	 *    значения предиката для всех строк в наборе
	 */
	public boolean[] test(SequenceSet set) {
		boolean[] selector = new boolean[set.size()];
		for (int i = 0; i < set.size(); i++) {
			selector[i] = this.test(set.observed(i));
		}
		return selector;
	}
	
	/**
	 * Разбивает выборку на две части в соответствии со значениями предиката.
	 * 
	 * @param set
	 *    выборка, которую надо разбить
	 * @return
	 *    две части выборки, первая из которых содержит строки, на которых предикат истиннен,
	 *    а вторая - строки, на которых он ложен
	 */
	public SequenceSet[] split(SequenceSet set) {
		boolean[] selector = this.test(set);
		SequenceSet compliantSubset = set.filter(selector);
		for (int i = 0; i < set.size(); i++) {
			selector[i] = !selector[i];
		}
		SequenceSet otherSubset = set.filter(selector);

		return new SequenceSet[] { compliantSubset, otherSubset };
	}
}

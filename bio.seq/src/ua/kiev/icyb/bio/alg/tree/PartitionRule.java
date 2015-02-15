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
	
	/**
	 * Возвращает предикат, значение которого на произвольной строке противоположно
	 * значению этого предиката.
	 * 
	 * @return
	 *    отрицание этого предиката
	 */
	public PartitionRule not() {
		return new NegationPartitionRule(this);
	}
	
	/**
	 * Возвращает конъюнкцию этого предиката и другого.
	 * 
	 * @param other
	 *    другой предикат
	 * @return
	 *    конъюнкция этого предиката и другого
	 */
	public PartitionRule and(PartitionRule other) {
		return new IntersectionPartitionRule(this, other);
	}
}

/**
 * Предикат, ялвяющийся отрицанием другого предиката.
 */
class NegationPartitionRule extends PartitionRule {
	
	/** Базовый предикат. */
	private final PartitionRule base;
	
	/**
	 * Создает предикат, который является отрицанием заданного предиката.
	 * 
	 * @param base
	 *    базовый предикат
	 */
	public NegationPartitionRule(PartitionRule base) {
		this.base = base;
	}

	@Override
	public boolean test(byte[] seq) {
		return !this.base.test(seq);
	}
	
	@Override
	public String toString() {
		return "!" + base.toString();
	}
}

/**
 * Предикат, являющийся конъюнкцией нескольких предикатов.
 */
class IntersectionPartitionRule extends PartitionRule {
	
	/** Базовые предикаты. */
	private final PartitionRule[] clauses;
	
	/**
	 * Создает конъюнкцию на основе базовых предикатов.
	 * 
	 * @param clauses
	 *    базовые предикаты
	 */
	public IntersectionPartitionRule(PartitionRule... clauses) {
		this.clauses = clauses;
	}
	
	@Override
	public boolean test(byte[] seq) {
		for (int i = 0; i < clauses.length; i++) {
			if (!clauses[i].test(seq)) return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		String str = "";
		for (int i = 0; i < clauses.length; i++) {
			str += clauses[i].toString();
			if (i < clauses.length - 1) {
				str += " & ";
			}
		}
		return str;
	}
}

/**
 * Тривиальный предикат, равный {@code true} на произвольной строке наблюдаемых состояний.
 */
class TrivialPartitionRule extends PartitionRule {

	@Override
	public boolean test(byte[] seq) {
		return true;
	}
	
	@Override
	public PartitionRule and(PartitionRule other) {
		return other;
	}
	
	@Override
	public String toString() {
		return "true";
	}
}
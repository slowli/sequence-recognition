package ua.kiev.icyb.bio.alg.tree;

import java.io.Serializable;

import ua.kiev.icyb.bio.SequenceSet;


/**
 * Предикат, определенный на пространстве строк наблюдаемых состояний.
 */
public interface PartitionRule extends Serializable {

	/**
	 * Вычисляет значение предиката на заданной строке наблюдаемых состояний.
	 * 
	 * @param seq
	 *        последовательность наблюдаемых состояний
	 * @return
	 *        значение предиката
	 */
	public abstract boolean test(byte[] seq);

	/**
	 * Вычисляет значение предиката на заданном наборе строк наблюдаемых состояний.
	 * 
	 * @param set
	 *        набор последовательностей
	 * @return
	 *        значения предиката для всех строк в наборе
	 */
	public abstract boolean[] test(SequenceSet set);
}

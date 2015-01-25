package ua.kiev.icyb.bio;

/**
 * Интерфейс для обработки событий, связанных с распознаванием скрытых последовательностей
 * для набора наблюдаемых строк.
 * 
 * @see SeqAlgorithm#runSet(SequenceSet, JobListener)
 */
public interface JobListener {
	
	/**
	 * Вызывается каждый раз, когда алгоритм восстановил последовательность 
	 * скрытых состояний для какой-то строки наблюдаемых состояний из выборки.
	 * 
	 * @param sequence
	 *    результат работы алгоритма
	 */
	public abstract void seqCompleted(Sequence sequence);
	
	/**
	 * Вызывается после окончания работы со всеми последовательностями в выборке.
	 */
	public abstract void finished();
}

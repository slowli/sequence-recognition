package ua.kiev.icyb.bio;

/**
 * Коллекция запусков некоторого алгоритма распознавания на различных выборках.
 */
public interface RunCollection extends Launchable {
	
	/**
	 * Возвращает выборку для определенного запуска алгоритма.
	 * 
	 * @param index
	 *    номер выборки
	 * @return
	 *    выборка для запуска алгоритма с порядковым номером <code>index</code> (с отсчетом от нуля)
	 */
	SequenceSet getSet(int index);
}

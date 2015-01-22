package ua.kiev.icyb.bio.alg;

import java.io.Serializable;

/**
 * Представление короткой последовательности полных состояний с помощью целых чисел.
 * 
 * <p>Строка полных состояний, т.е. пара строк наблюдаемых и соответствующих им
 * скрытых состояний одинаковой длины, представляются в рамках класса тремя числами:
 * <ol>
 * <li>длина строки;
 * <li>порядковый номер (с отсчетом от нуля) наблюдаемой цепочки состояний среди всех
 * наблюдаемых строк фиксированной длины;
 * <li>порядковый номер (с отсчетом от нуля) скрытой цепочки состояний среди всех
 * скрытых строк фиксированной длины.
 * </ol>
 * 
 * <p>Класс переопределяет стандартные функции {@link #hashCode()} и {@link #equals(Object)}
 * для сравнения фрагментов между собой. Два фрагмента считаются равными тогда и только тогда,
 * когда равны соответствующие им цепочки полных состояний, или, что то же самое, равны три
 * введенные характеристики.
 * 
 * <p>
 * <b>Пример.</b> Для задачи распознавания фрагментов генов строка <code>"ACg"</code> (нуклеотид аденин,
 * относящийся к экзону, нуклеотид цитозин, относящийся к экзону, а также нуклеотид гуанин,
 * относящийся к интрону) характеризуется тройкой чисел:
 * <ol>
 * <li>длина строки <code>3</code>;
 * <li>порядковый номер наблюдаемой цепочки состояний <code>012<sub>4</sub> = 6</code>;
 * <li>порядковый номер скрытой цепочки состояний <code>001<sub>2</sub> = 1</code>.
 * </ol>
 */
public final class Fragment implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Конвертирует целочисленное представление последовательности состояний обратно
	 * в строку.
	 * 
	 * @param state
	 *    порядковый номер строки состояний среди всех строк фиксированной длины
	 * @param base
	 *    размер алфавита состояний
	 * @param length
	 *    длина последовательности
	 * @return
	 *    восстановленная цепочка состояний
	 */
	public static byte[] sequence(int state, int base, int length) {
		byte[] seq = new byte[length];
		for (int i = 0; i < length; i++) {
			seq[length - i - 1] = (byte) (state % base);
			state /= base;
		}
		return seq;
	}

	/**
	 * Порядковый номер наблюдаемой цепочки состояний среди всех
	 * наблюдаемых строк фиксированной длины.
	 */
	public int observed;
	/**
	 * Порядковый номер скрытой цепочки состояний среди всех
	 * скрытых строк фиксированной длины.
	 */
	public int hidden;
	/**
	 * Длина цепочки.
	 */
	public int length;

	/**
	 * Создает фрагмент с заданными целочисленными параметрами.
	 * 
	 * @param observed
	 *    порядковый номер наблюдаемой цепочки состояний
	 * @param hidden
	 *    порядковый номер скрытой цепочки состояний
	 * @param length
	 *    длина цепочки
	 */
	public Fragment(int observed, int hidden, int length) {
		this.observed = observed;
		this.hidden = hidden;
		this.length = length;
	}
	
	/**
	 * Копирующий конструктор.
	 * 
	 * @param other
	 *    фрагмент, который надо скопировать
	 */
	public Fragment(Fragment other) {
		this.observed = other.observed;
		this.hidden = other.hidden;
		this.length = other.length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + hidden;
		result = prime * result + length;
		result = prime * result + observed;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		Fragment other = (Fragment) obj;
		if (hidden != other.hidden)
			return false;
		if (length != other.length)
			return false;
		if (observed != other.observed)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return observed + " " + hidden;
	}
}
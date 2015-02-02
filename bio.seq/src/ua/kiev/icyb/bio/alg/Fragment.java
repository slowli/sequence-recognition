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
	 * Фабрика, с помощью которой был создан этот фрагмент.
	 */
	FragmentFactory factory;

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
	Fragment(FragmentFactory factory, int observed, int hidden, int length) {
		this.observed = observed;
		this.hidden = hidden;
		this.length = length;
		this.factory = factory;
	}
	
	/**
	 * Копирующий конструктор.
	 * 
	 * @param other
	 *    фрагмент, который надо скопировать
	 */
	private Fragment(Fragment other) {
		this.observed = other.observed;
		this.hidden = other.hidden;
		this.length = other.length;
		this.factory = other.factory;
	}

	@Override
	public Fragment clone() {
		return new Fragment(this);
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
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		Fragment other = (Fragment) obj;
		if (hidden != other.hidden) return false;
		if (length != other.length) return false;
		if (observed != other.observed) return false;
		
		return true;
	}

	/**
	 * Объединяет другой фрагмент с этим фрагментом.
	 * 
	 * @param other
	 *    фрагмент, добавляемый вслед за данным
	 * @return
	 *    результат объединения фрагментов
	 */
	public Fragment append(Fragment other) {
		return factory.compose(this, other);
	}
	
	/**
	 * Объединяет другой фрагмент с этим фрагментом.
	 * 
	 * @param other
	 *    фрагмент, добавляемый вслед за данным
	 * @param output
	 *    фрагмент, в который следует записать результат
	 */
	public void append(Fragment other, Fragment output) {
		factory.compose(this, other, output);
	}
	
	/**
	 * Возвращает префикс этого фрагмента заданной длины.
	 * 
	 * @param length
	 *    длина префикса
	 * @return
	 *    префикс фрагмента
	 */
	public Fragment prefix(int length) {
		return factory.prefix(this, length);
	}
	
	/**
	 * Возвращает префикс этого фрагмента заданной длины.
	 * 
	 * @param length
	 *    длина префикса
	 * @param output
	 *    фрагмент, в который следует записать результат
	 */
	public void prefix(int length, Fragment output) {
		factory.prefix(this, length, output);
	}
	
	/**
	 * Возвращает суффикс этого фрагмента заданной длины.
	 * 
	 * @param length
	 *    длина суффикса
	 * @return
	 *    суффикс фрагмента
	 */
	public Fragment suffix(int length) {
		return factory.suffix(this, length);
	}
	
	/**
	 * Возвращает суффикс этого фрагмента заданной длины.
	 * 
	 * @param length
	 *    длина суффикса
	 * @param output
	 *    фрагмент, в который следует записать результат
	 */
	public void suffix(int length, Fragment output) {
		factory.suffix(this, length, output);
	}
	
	/**
	 * Вычисляет позицию фрагмента в упорядоченном множестве фрагментов строк полных
	 * состояний той же длины.
	 * 
	 * @return 
	 *    индекс (с отсчетом от нуля) фрагмента в упорядоченном множестве фрагментов
	 *    фиксированной длины
	 */
	public int index() {
		return factory.getTotalIndex(this);
	}
	
	@Override
	public String toString() {
		return factory.toString(this);
	}
}
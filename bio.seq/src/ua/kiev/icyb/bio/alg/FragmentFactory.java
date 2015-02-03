package ua.kiev.icyb.bio.alg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;

/**
 * Фабрика для создания и операций над фрагментами строк полных состояний.
 * 
 * @see Fragment
 */
public class FragmentFactory implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	
	/** Алфавит наблюдаемых состояний. */
	private final String observedStates;
	/** Алфавит скрытых состояний. */
	private final String hiddenStates;
	/** Алфавит полных состояний (может быть равен {@code null}). */
	private final String completeStates;
	/** Количество наблюдаемых состояний. */
	private int oSize;
	/** Количество скрытых состояний. */
	private int hSize;

	/*
	 * Степени oSize, hSize и (oSize * hSize) (для ускорения вычислений).
	 */
	private int[] obsPower;
	private int[] hPower;
	private int[] cPower;
	
	/**
	 * Создает фабрику с заданной структурой полных состояний.
	 * 
	 * @param observedStates
	 *    строка, каждый символ которой определяет одно из наблюдаемых состояний
	 * @param hiddenStates
	 *    строка, каждый символ которой определяет одно из скрытых состояний
	 * @param completeStates
	 *    строка, каждый символ которой определяет одно из полных состояний (может равняться {@code null})
	 * @param maxLength
	 *    максимальная длина фрагментов полных состояний, которые будут
	 *    создаваться этой фабрикой
	 */
	public FragmentFactory(String observedStates, String hiddenStates, String completeStates, int maxLength) {
		if ((completeStates != null) 
				&& (completeStates.length() != observedStates.length() * hiddenStates.length())) {
			
			throw new IllegalArgumentException("Wrong number of complete states");
		}
		
		this.observedStates = observedStates;
		this.hiddenStates = hiddenStates;
		this.completeStates = completeStates;
		this.oSize = observedStates.length();
		this.hSize = hiddenStates.length();

		maxLength++; // Степени длин алфавитов начинаются с нулевой
		obsPower = new int[maxLength];
		obsPower[0] = 1;
		for (int i = 1; i < maxLength; i++)
			obsPower[i] = obsPower[i - 1] * oSize;
		hPower = new int[maxLength];
		hPower[0] = 1;
		for (int i = 1; i < maxLength; i++)
			hPower[i] = hPower[i - 1] * hSize;
		cPower = new int[maxLength];
		cPower[0] = 1;
		for (int i = 1; i < maxLength; i++)
			cPower[i] = cPower[i - 1] * oSize * hSize;
	}
	
	public FragmentFactory(SequenceSet set, int maxLength) {
		this(set.observedStates(), set.hiddenStates(), set.completeStates(), maxLength);
	}
	
	/**
	 * Создает новый пустой фрагмент.
	 * 
	 * @return
	 *    созданный фрагмент
	 */
	public Fragment fragment() {
		return new Fragment(this, 0, 0, 0);
	}
	
	/**
	 * Создает фрагмент с заданными параметрами.
	 * 
	 * @param observed
	 *    порядковый номер наблюдаемой цепочки состояний среди всех строк наблюдаемых состояний
	 *    фиксированной длины
	 * @param hidden
	 *    порядковый номер скрытой цепочки состояний среди всех строк скрытых состояний
	 *    фиксированной длины
	 * @param length
	 *    длина фрагмента
	 *    
	 * @return
	 *    созданный фрагмент
	 */
	public Fragment fragment(int observed, int hidden, int length) {
		return new Fragment(this, observed, hidden, length);
	}

	/**
	 * Создает список, состоящий из всех фрагментов фиксированной длины.
	 * 
	 * @param length
	 *    длина фрагментов
	 * @return
	 *    список фрагментов заданной длины
	 */
	public List<Fragment> allFragments(int length) {
		List<Fragment> fragments = new ArrayList<Fragment>();
		
		for (int i = 0; i < cPower[length]; i++) {
			fragments.add(this.fragment(i % obsPower[length], i / obsPower[length], length));
		}
		
		return fragments;
	}
	
	/**
	 * Создает фрагмент строки полных состояний на основе заданных последовательностей
	 * наблюдаемых и скрытых состояний, а также положения фрагмента.
	 * 
	 * @param observed
	 *    последовательность наблюдаемых состояний
	 * @param hidden
	 *    последовательность скрытых состояний
	 * @param start
	 *    положение начального символа фрагмента с отсчетом от нуля
	 * @param length
	 *    длина фрагмента
	 * 
	 * @return
	 *    фрагмент строки полных состояний
	 */
	public Fragment fragment(final byte[] observed, final byte[] hidden, int start, int length) {
		int obsIndex = 0;
		for (int i = 0; i < length; i++)
			obsIndex += observed[start + i] * obsPower[length - i - 1];
		int hIndex = 0;
		for (int i = 0; i < length; i++)
			hIndex += hidden[start + i] * hPower[length - i - 1];

		return this.fragment(obsIndex, hIndex, length);
	}
	
	/**
	 * Создает фрагмент строки полных состояний на основе заданных последовательностей
	 * наблюдаемых и скрытых состояний, а также положения фрагмента.
	 * 
	 * @param observed
	 *    последовательность наблюдаемых состояний
	 * @param hidden
	 *    последовательность скрытых состояний
	 * @param start
	 *    положение начального символа фрагмента с отсчетом от нуля
	 * @param length
	 *    длина фрагмента
	 * @param output
	 *    фрагмент, в который следует записать результат
	 */
	public void fragment(final byte[] observed, final byte[] hidden, int start, int length, Fragment output) {
		int obsIndex = 0;
		for (int i = 0; i < length; i++)
			obsIndex += observed[start + i] * obsPower[length - i - 1];
		int hIndex = 0;
		for (int i = 0; i < length; i++)
			hIndex += hidden[start + i] * hPower[length - i - 1];

		output.observed = obsIndex;
		output.hidden = hIndex;
		output.length = length;
	}

	/**
	 * Создает фрагмент строки полных состояний на основе заданной последовательности
	 * наблюдаемых состояний, индекса строки скрытых состояний, а также положения фрагмента.
	 * 
	 * @param observed
	 *    последовательность наблюдаемых состояний
	 * @param hIndex
	 *    порядковый номер скрытой цепочки состояний среди всех строк скрытых состояний
	 *    фиксированной длины
	 * @param start
	 *    положение начального символа фрагмента с отсчетом от нуля
	 * @param length
	 *    длина фрагмента
	 * 
	 * @return
	 *    фрагмент строки полных состояний
	 */
	public Fragment fragment(final byte[] observed, int hIndex, int start, int length) {
		int obsIndex = 0;
		for (int i = 0; i < length; i++) {
			obsIndex += observed[start + i] * obsPower[length - i - 1];
		}

		return this.fragment(obsIndex, hIndex, length);
	}
	
	/**
	 * Создает фрагмент строки полных состояний на основе заданной последовательности
	 * наблюдаемых состояний, индекса строки скрытых состояний, а также положения фрагмента.
	 * 
	 * @param observed
	 *    последовательность наблюдаемых состояний
	 * @param hIndex
	 *    порядковый номер скрытой цепочки состояний среди всех строк скрытых состояний
	 *    фиксированной длины
	 * @param start
	 *    положение начального символа фрагмента с отсчетом от нуля
	 * @param length
	 *    длина фрагмента
	 * @param output
	 *    фрагмент, в который следует записать результат
	 */
	public void fragment(final byte[] observed, int hIndex, int start, int length, Fragment output) {
		int obsIndex = 0;
		for (int i = 0; i < length; i++) {
			obsIndex += observed[start + i] * obsPower[length - i - 1];
		}

		output.observed = obsIndex;
		output.hidden = hIndex;
		output.length = length;
	}

	/**
	 * Выполняет конкатенацию двух фрагментов.
	 * 
	 * @param x
	 *    начальный фрагмент
	 * @param y
	 *    конечный фрагмент
	 * @return
	 *    фрагмент, соответствующий строке {@code xy}
	 */
	Fragment compose(Fragment x, Fragment y) {
		int obsIndex = x.observed * obsPower[y.length] + y.observed;
		int hIndex = x.hidden * hPower[y.length] + y.hidden;
		
		return this.fragment(obsIndex, hIndex, x.length + y.length);
	}
	
	void compose(Fragment x, Fragment y, Fragment output) {
		int observed = x.observed * obsPower[y.length] + y.observed;
		int hidden = x.hidden * hPower[y.length] + y.hidden;
		
		output.length = x.length + y.length;
		output.observed = observed;
		output.hidden = hidden;
	}

	/**
	 * Выделяет префикс фрагмента строки полных состояний.
	 * 
	 * @param fragment
	 *        базовый фрагмент строки полных состояний
	 * @param length
	 *        длина префикса
	 *        
	 * @return префикс фрагмента заданной длины
	 */
	Fragment prefix(Fragment fragment, int length) {
		if (fragment.length == length)
			return fragment;

		int obsIndex = fragment.observed / obsPower[fragment.length - length];
		int hIndex = fragment.hidden / hPower[fragment.length - length];
		return this.fragment(obsIndex, hIndex, length);
	}
	
	void prefix(Fragment fragment, int length, Fragment output) {
		int obsIndex = fragment.observed / obsPower[fragment.length - length];
		int hIndex = fragment.hidden / hPower[fragment.length - length];
		
		output.length = length;
		output.observed = obsIndex;
		output.hidden = hIndex;
	}

	/**
	 * Выделяет суффикс фрагмента строки полных состояний.
	 * 
	 * @param fragment
	 *        базовый фрагмент строки полных состояний
	 * @param length
	 *        длина суффикса
	 *        
	 * @return суффикс фрагмента заданной длины
	 */
	Fragment suffix(Fragment fragment, int length) {
		if (fragment.length == length)
			return fragment;

		int obsIndex = fragment.observed % obsPower[length];
		int hIndex = fragment.hidden % hPower[length];
		return this.fragment(obsIndex, hIndex, length);
	}
	
	void suffix(Fragment fragment, int length, Fragment output) {
		int obsIndex = fragment.observed % obsPower[length];
		int hIndex = fragment.hidden % hPower[length];
		
		output.length = length;
		output.observed = obsIndex;
		output.hidden = hIndex;
	}
	
	/**
	 * Вычисляет позицию фрагмента в упорядоченном множестве фрагментов строк полных
	 * состояний той же длины.
	 * 
	 * @param fragment
	 *    фрагмент строки полных состояний
	 * @return 
	 *    индекс (с отсчетом от нуля) фрагмента в упорядоченном множестве фрагментов
	 *    фиксированной длины
	 */
	int getTotalIndex(Fragment fragment) {
		return fragment.observed + fragment.hidden * obsPower[fragment.length];
	}

	/**
	 * Включает фрагмент в последовательность полных состояний. 
	 * 
	 * @param sequence
	 *    последовательность, в которую встраивается фрагмент
	 * @param fragment
	 *    фрагмент, который надо встроить в последовательность
	 * @param start
	 *    индекс (с отсчетом от нуля) начала фрагмента
	 */
	void embed(Sequence sequence, Fragment fragment, int start) {
		int oIndex = fragment.observed, hIndex = fragment.hidden;
		
		for (int pos = fragment.length - 1; pos >= 0; pos--) {
			sequence.observed[start + pos] = (byte) (oIndex % oSize);
			sequence.hidden[start + pos] = (byte) (hIndex % hSize);
			
			oIndex /= oSize;
			hIndex /= hSize;
		}
	}
	
	/**
	 * Создает строковое представление для фрагмента строки полных состояний.
	 * Строковое представление для отдельного полного состояния включает в себя
	 * два символа, соответствующие наблюдаемому и скрытому состояниям, которые
	 * оно содержит. Для цепочки полных состояний представление является цепочкой
	 * представлений для отдельных полных состояний.
	 * 
	 *  <p>
	 * <b>Пример.</b> Для задачи распознавания фрагментов генов строка <code>"ACg"</code> 
	 * (нуклеотид аденин, относящийся к экзону, нуклеотид цитозин, относящийся к экзону, 
	 * нуклеотид гуанин, относящийся к интрону) представляется в виде <code>"AxCxGi"</code>.
	 * 
	 * @param fragment
	 *        фрагмент
	 * @return строковое представление фрагмента
	 */
	String toString(Fragment fragment) {
		String result = "";
		int obsIndex = fragment.observed, hIndex = fragment.hidden;
		
		for (int i = 0; i < fragment.length; i++) {
			String symbol;
			
			if (completeStates == null) {
				symbol = "" + observedStates.charAt(obsIndex % oSize) + hiddenStates.charAt(hIndex % hSize);
			} else {
				symbol = "" + completeStates.charAt((obsIndex % oSize) + (hIndex % hSize) * oSize);
			}
			
			result = symbol + result;
			obsIndex /= oSize;
			hIndex /= hSize;
		}
		
		return result;
	}
}

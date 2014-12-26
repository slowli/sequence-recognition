package ua.kiev.icyb.bio.alg;

/**
 * Фабрика для создания и операций над фрагментами строк полных состояний.
 * 
 * @see Fragment
 */
public class FragmentFactory {
	
	/** Алфавит наблюдаемых состояний. */
	private final String alphabet;
	/** Алфавит скрытых состояний. */
	private final String hAlphabet;
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
	 * @param alphabet
	 *        строка, каждый символ которой определяет одно из наблюдаемых состояний
	 * @param hAlphabet
	 *        строка, каждый символ которой определяет одно из скрытых состояний
	 * @param maxLength
	 *        максимальная длина фрагментов полных состояний, которые будут
	 *        создаваться этой фабрикой
	 */
	public FragmentFactory(String alphabet, String hAlphabet, int maxLength) {
		this.alphabet = alphabet.toUpperCase();
		this.hAlphabet = hAlphabet.toLowerCase();
		this.oSize = alphabet.length();
		this.hSize = hAlphabet.length();

		maxLength++; // Powers go from zero, hence the increment
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

	/**
	 * Создает фрагмент строки полных состояний на основе заданных последовательностей
	 * наблюдаемых и скрытых состояний, а также положения фрагмента.
	 * 
	 * @param observed
	 *        последовательность наблюдаемых состояний
	 * @param hidden
	 *        последовательность скрытых состояний
	 * @param start
	 *        положение начального символа фрагмента с отсчетом от нуля
	 * @param length
	 *        длина фрагмента
	 * 
	 * @return
	 *        фрагмент строки полных состояний
	 */
	public Fragment fragment(final byte[] observed, final byte[] hidden, int start, int length) {
		int obsIndex = 0;
		for (int i = 0; i < length; i++)
			obsIndex += observed[start + i] * obsPower[length - i - 1];
		int hIndex = 0;
		for (int i = 0; i < length; i++)
			hIndex += hidden[start + i] * hPower[length - i - 1];

		return new Fragment(obsIndex, hIndex, length);
	}

	/**
	 * Создает фрагмент строки полных состояний на основе заданной последовательности
	 * наблюдаемых состояний, индекса строки скрытых состояний, а также положения фрагмента.
	 * 
	 * @param observed
	 *        последовательность наблюдаемых состояний
	 * @param hIndex
	 *        порядковый номер скрытой цепочки состояний среди всех строк скрытых состояний
	 *        фиксированной длины
	 * @param start
	 *        положение начального символа фрагмента с отсчетом от нуля
	 * @param length
	 *        длина фрагмента
	 * 
	 * @return
	 *        фрагмент строки полных состояний
	 */
	public Fragment fragment(final byte[] observed, int hIndex, int start, int length) {
		int obsIndex = 0;
		for (int i = 0; i < length; i++) {
			obsIndex += observed[start + i] * obsPower[length - i - 1];
		}

		return new Fragment(obsIndex, hIndex, length);
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
	public Fragment compose(Fragment x, Fragment y) {
		int obsIndex = x.observed * obsPower[y.length] + y.observed;
		int hIndex = x.hidden * hPower[y.length] + y.hidden;
		return new Fragment(obsIndex, hIndex, x.length + y.length);
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
	public Fragment prefix(Fragment fragment, int length) {
		if (fragment.length == length)
			return fragment;

		int obsIndex = fragment.observed / obsPower[fragment.length - length];
		int hIndex = fragment.hidden / hPower[fragment.length - length];
		return new Fragment(obsIndex, hIndex, length);
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
	public Fragment suffix(Fragment fragment, int length) {
		if (fragment.length == length)
			return fragment;

		int obsIndex = fragment.observed % obsPower[length];
		int hIndex = fragment.hidden % hPower[length];
		return new Fragment(obsIndex, hIndex, length);
	}

	/**
	 * Разделяет фрагмент строки полных состояний на три части: префикс,
	 * суффикс и середина. Префикс и суффикс могут пересекаться между собой;
	 * в этом случае, средняя часть фрагмента будет равной {@code null}.
	 * 
	 * @param fragment
	 *    базовый фрагмент строки полных состояний
	 * @param left
	 *    длина префикса
	 * @param right
	 *    длина суффикса
	 * @return
	 *    массив, состоящий из трех фрагментов - префикса, суффикса и серединной части 
	 *    исходного фрагмента
	 */
	public Fragment[] split(Fragment fragment, int left, int right) {
		Fragment rState = suffix(fragment, right);
		Fragment lState = prefix(fragment, left);

		Fragment mState = null;
		if (fragment.length > left + right) {
			int mLength = fragment.length - left - right;
			int obsIndex = (fragment.observed / obsPower[right]) % obsPower[mLength];
			int hIndex = (fragment.hidden / hPower[right]) % hPower[mLength];
			mState = new Fragment(obsIndex, hIndex, mLength);
		}

		return new Fragment[] { lState, mState, rState };
	}

	/**
	 * Вычисляет позицию фрагмента в упорядоченном множестве фрагментов строк полных
	 * состояний той же длины.
	 * 
	 * @param fragment
	 *        фрагмент строки полных состояний
	 * @return индекс (с отсчетом от нуля) фрагмента в упорядоченном множестве фрагментов
	 *    фиксированной длины
	 */
	public int getTotalIndex(Fragment fragment) {
		return fragment.observed + fragment.hidden * obsPower[fragment.length];
	}

	/**
	 * Определяет индекс последовательности скрытых состояний, полученной в результате
	 * сдвига рамки чтения вправо. В результате сдвига получается фрагмент с той же длиной,
	 * что и текущая хвостовая цепочка состояний; новый фрагмент включает в себя всю головную
	 * последовательность состояний и суффикс текущей хвостовой цепочки.
	 * 
	 * @param tail
	 *        фрагмент, определяющий текущую хвостовую последовательность состояний
	 * @param head
	 *        фрагмент, определяющий текущую головную последовательность состояний
	 * @return
	 *    индекс (с отсчетом от нуля) скрытой цепочки состояний в фрагменте, полученном
	 *    в результате сдвига, среди всех цепочек скрытых состояний той же длины
	 */
	public int shift(Fragment tail, Fragment head) {
		if (head.length == 1) {
			return (tail.hidden % hPower[tail.length - 1]) * hSize + head.hidden;
		} else {
			// TODO implement
			return 0;
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
	public String toString(Fragment fragment) {
		String result = "";
		int obsIndex = fragment.observed, hIndex = fragment.hidden;
		for (int i = 0; i < fragment.length; i++) {
			String symbol;
			symbol = "" + alphabet.charAt(obsIndex % oSize) + hAlphabet.charAt(hIndex % hSize);
			result = symbol + result;
			obsIndex /= oSize;
			hIndex /= hSize;
		}
		return result;
	}
}

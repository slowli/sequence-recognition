package ua.kiev.icyb.bio.alg.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ua.kiev.icyb.bio.SequenceSet;


/**
 * Множество из цепочек состояний фиксированной длины.
 * Цепочки представляются в виде целых чисел, соответствующих их индексам (с отсчетом от нуля)
 * в упорядоченном в алфавитном порядке множестве всех цепочек фиксированной длины.
 * 
 * <p><b>Пример.</b>
 * <pre>
 * FragmentSet set = new FragmentSet("ACGT", 2);
 * set.add(1);
 * System.out.println(set); // Выводит AC
 * set.add(6);
 * System.out.println(set); // Выводит AC,CG
 * </pre>
 */
public class FragmentSet extends HashSet<Integer> {
	
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Генерирует множество, содержащее все цепочки состояний определенной
	 * длины.
	 * 
	 * @param alphabet 
	 *    алфавит цепочек состояний
	 * @param length 
	 *    длина генерируемых цепочек
	 * @return
	 *    множество цепочек состояний
	 */
	public static FragmentSet getSequences(String alphabet, int length) {
		FragmentSet bases = new FragmentSet(alphabet, length);
		
		int power = 1;
		for (int i = 0; i < length; i++)
			power *= alphabet.length();
		
		for (int x = 0; x < power; x++) {
			bases.add(x);
		}
		
		return bases;
	}

	/**
	 * Удаляет из коллекции множеств цепочек близкие множества. Расстояние
	 * между множествами вычисляется по Хэммингу. После выполнения метода
	 * для любых двух элементов <code>x</code> и <code>y</code> коллекции <code>set</code> 
	 * справедливо неравенство
	 * <pre>
	 * x.distance(y) > distance
	 * </pre>
	 * 
	 * @see #distance(FragmentSet)
	 * 
	 * @param sets
	 *    коллекция множеств, из которой необходимо удалить близкие множества 
	 * @param distance
	 *    граничное расстояние между множествами
	 */
	public static void trim(Collection<FragmentSet> sets, int distance) {
		List<FragmentSet> newSets = new ArrayList<FragmentSet>(sets);
		for (int i = 0; i < newSets.size(); i++) {
			FragmentSet set = newSets.get(i);
			
			for (int j = newSets.size() - 1; j > i; j--)
				if (newSets.get(j).distance(set) <= distance)
					newSets.remove(j);
		}
		sets.clear();
		sets.addAll(newSets);
	}

	/**
	 * Алфавит состояний, используемый в этом множестве. Алфавит состоит из 
	 * уникальных символов, каждый из которых соответствует отдельному состоянию.
	 */
	private final String alphabet;
	
	/**
	 * Длина фрагментов, содержащихся во множестве.
	 */
	private final int fragmentLength;
	
	/**
	 * Создает пустое множество, которое может содержать фрагменты состояний указанной длины.
	 * 
	 * @param alphabet
	 *    используемый алфавит состояний
	 * @param length
	 *    длина фрагментов, которые может содержать множество
	 */
	public FragmentSet(String alphabet, int length) {
		super();
		this.alphabet = alphabet;
		this.fragmentLength = length;
	}

	/**
	 * Создает множество фрагментов с заданными элементами.
	 * 
	 * @param alphabet
	 *    используемый алфавит состояний
	 * @param fragments
	 *    коллекция фрагментов
	 * 
	 * @throws IllegalArgumentException
	 *    если выполнено хотя бы одно из условий:
	 *    <ul>
	 *    <li>коллекция фрагментов пуста;
	 *    <li>существуют фрагменты различной длины;
	 *    <li>в какой-либо из фрагментов входит символ, отстутствующий в алфавите.
	 *    </ul>
	 */
	public FragmentSet(String alphabet, Collection<String> fragments) {
		super();
	
		if (fragments.isEmpty()) {
			throw new IllegalArgumentException("At least 1 fragment needed to initialize set");
		}
		
		this.alphabet = alphabet;
		int len = -1;
		for (String fragmentStr : fragments) {
			if ((len >= 0) && (len != fragmentStr.length())) {
				throw new IllegalArgumentException("All fragments should have same length");
			}
			len = fragmentStr.length();
			
			int val = encode(fragmentStr);
			if (val < 0) throw new IllegalArgumentException("Invalid fragment: " + fragmentStr);
			this.add(val);
		}
	
		this.fragmentLength = len;
	}

	public FragmentSet(String alphabet, String fragment) {
		this(alphabet, Collections.singleton(fragment));
	}

	/**
	 * Копирующий конструктор.
	 * 
	 * @param other
	 *    множество фргментов
	 */
	public FragmentSet(FragmentSet other) {
		super(other);
		this.alphabet = other.alphabet;
		this.fragmentLength = other.fragmentLength;
	}

	/**
	 * Переводит фрагмент из текстового вида в целое число.
	 * 
	 * @param str
	 *    фрагмент
	 * @return
	 *    целочисленное представление фрагмента или {@code -1}, если в фрагменте есть символ, 
	 *    отсутствующий в алфавите состояний {@link #getStates()} 
	 */
	private int encode(String str) {
		int index = 0;
		for (int i = 0; i < str.length(); i++) {
			int pos = alphabet.indexOf(str.charAt(i));
			if (pos < 0) return -1;
			
			index = index * alphabet.length() + pos;
		}
		
		return index;
	}

	/**
	 * Переводит отдельный элемент множества в текстовый вид.
	 * 
	 * @param index
	 *    элемент, который надо перевести
	 * @return
	 *    текстовое представление элемента
	 */
	private String decode(int index) {
		String repr = "";
		int val = index;
		for (int i = 0; i < fragmentLength; i++) {
			repr = alphabet.charAt(val % alphabet.length()) + repr;
			val /= alphabet.length();
		}
		return repr;
	}

	/**
	 * Вычисляет <em>хэш</em> множества, позволяющий идентифицировать его среди всех
	 * множеств фрагментов с фиксированной длиной.
	 * 
	 * @return
	 *    целочисленный хэш 
	 */
	protected long getHash() {
		long hash = 0L;
		for (Integer elem: this) {
			hash += (1L << elem);
		}
		return hash;
	}

	/**
	 * Возвращает алфавит состояний, используемый в этом множестве. Алфавит состоит из 
	 * уникальных символов, каждый из которых соответствует отдельному состоянию.
	 * 
	 * @return
	 *    алфавит состояний
	 */
	public String getStates() {
		return alphabet;
	}

	/**
	 * Возвращает длину фрагментов, содержащихся во множестве.
	 * 
	 * @return
	 *    длина фрагментов
	 */
	public int getFragmentLength() {
		return fragmentLength;
	}
	
	/**
	 * Вычисляет суммарную концентрацию цепочек из этого множества в заданной
	 * строке состояний.
	 * 
	 * @param seq
	 *    строка состояний
	 * @return
	 *    концентрация цепочек из множества (вещественное число 
	 *    от <code>0.0</code> до <code>1.0</code>) 
	 */
	public double content(byte[] seq) {
		int patternLength = getFragmentLength();
		
		int count = 0;
		for (int pos = 0; pos < seq.length - patternLength + 1; pos++) {
			// Вычислить индекс подстроки, начинающейся с позиции pos
			int hash = 0, power = 1;
			for (int i = patternLength - 1; i >= 0; i--) {
				hash += seq[pos + i] * power;
				power *= getStates().length();
			}
			if (this.contains(hash))
				count++;
		}
		
		return 1.0 * count/(seq.length - patternLength + 1);
	}
	
	/**
	 * Подсчитывает медианную концентрацию цепочек из этого множества
	 * в строках из определенной выборки.
	 * 
	 * @param set
	 *    выборка строк наблюдаемых состояний
	 * @return 
	 *    медианная концентрация (вещественное число 
	 *    от <code>0.0</code> до <code>1.0</code>)
	 */
	public double medianContent(SequenceSet set) {
		double[] vars = new double[set.size()];
		for (int i = 0; i < set.size(); i++)
			vars[i] = content(set.observed(i));
		Arrays.sort(vars);
		return vars[vars.length / 2];
	}
	
	/**
	 * Возвращает список всех подмножеств заданного множества цепочек состояний.
	 * 
	 * @return
	 *    список из всех подмножеств множества
	 */
	public Collection<FragmentSet> subsets() {
		Integer[] bases = this.toArray(new Integer[0]);
		Set<FragmentSet> result = new HashSet<FragmentSet>(); 
		
		final int max = (1 << bases.length) - 1;
		for (int x = 1; x <= max - 1; x++) {
			int val = x;
			FragmentSet partialSet = new FragmentSet(this.getStates(), this.getFragmentLength());
			for (int i = 0; i < bases.length; i++) {
				if (val % 2 == 1) {
					partialSet.add(bases[i]);
				}
				val /= 2;
			}
			
			FragmentSet compl = partialSet.complmentary();
			if (!result.contains(compl)) {
				result.add(partialSet);
			} else if (partialSet.size() < compl.size()) {
				result.remove(compl);
				result.add(partialSet);
			}
		}
		
		return result;
	}
	
	/**
	 * Вычисляет расстояние Хэмминга между двумя множествами цепочек.
	 * Расстояние Хэмминга равно числу элементов в симметрической разнице между
	 * двумя множествами:
	 * <pre>
	 * FragmentSet x, y;
	 * FragmentSet diff = new FragmentSet(x).removeAll(y);
	 * diff.addAll(new FragmentSet(y).removeAll(x));
	 * int distance = diff.size();
	 * </pre>
	 * 
	 * @param other
	 *    множество цепочек, расстояние для которого надо определить
	 * @return
	 *    расстояние Хэмминга между этой цепочкой и другой
	 */
	public int distance(FragmentSet other) {
		long intersection = this.getHash() ^ other.getHash();
		int count = 0;
		while (intersection > 0) {
			count += intersection % 2;
			intersection /= 2;
		}
		return count;
	}
	
	/**
	 * Возвращает дополнение к этому множеству относительно множества
	 * содержащего все цепочки состояний фиксированной длины из {@link #getStates()}.
	 * 
	 * @return
	 *    дополнение к этому множеству
	 */
	public FragmentSet complmentary() {
		FragmentSet complementary = new FragmentSet(getStates(), getFragmentLength());
		int max = 1;
		for (int i = 0; i < getFragmentLength(); i++) {
			max *= getStates().length();
		}
		
		for (int i = 0; i < max; i++) {
			if (!this.contains(i)) {
				complementary.add(i);
			}
		}
		
		return complementary;
	}
		
	@Override
	public String toString() {
		String result = "";
		for (int elem: this)
			result += (decode(elem) + ",");
		if (result.length() > 0)
			result = result.substring(0, result.length() - 1);
		return result;
	}
}
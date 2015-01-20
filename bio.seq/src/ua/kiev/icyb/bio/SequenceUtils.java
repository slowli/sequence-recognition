package ua.kiev.icyb.bio;

import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.res.Messages;

/**
 * Утилиты для обработки коллекций последовательностей.
 */
public class SequenceUtils {
	
	/**
	 * Создает символьное отображение по его текстовому представлению.
	 * Текстовое представление должно иметь вид
	 * <pre>
	 * &lt;<i>src</i>&gt;:&lt;<i>dest</i>&gt;
	 * </pre>
	 * где {@code src} и {@code dest} — строки одинаковой длины. <code>i</code>-й символ
	 * строки {@code src} отображается в <code>i</code>-й символ строки {@code dest}.
	 * 
	 * <p><b>Пример.</b>
	 * <pre>
	 * Map<Character, Character> map = SequenceUtils.translationMap("abcdef:bccaff");
	 * System.out.println(map);
	 * </pre>
	 * выведет строку
	 * <pre>
	 * {f=f, d=a, e=f, b=c, c=c, a=b}
	 * </pre>
	 * 
	 * @param map
	 *    текстовое представление отображения
	 * @return
	 *    отображение
	 * @throws IllegalArgumentException
	 *    если входная строка не является корректным текстовым представлением отображения
	 */
	public static Map<Character, Character> translationMap(String map) {
		String[] parts = map.split(":", 2);
		if (parts.length != 2) {
			throw new IllegalArgumentException(Messages.format("set.tr.e_map", map));
		}
		if (parts[0].length() != parts[1].length()) {
			throw new IllegalArgumentException(Messages.format("set.tr.e_map", map));
		}
		
		Map<Character, Character> tr = new HashMap<Character, Character>();
		for (int i = 0; i < parts[0].length(); i++) {
			tr.put(parts[0].charAt(i), parts[1].charAt(i));
		}
		return tr;
	}
	
	/**
	 * Преобразует алфавит состояний в соответствии с отображением.
	 * 
	 * @param alphabet
	 *    исходный алфавит
	 * @param map
	 *    отображение символов алфавита
	 * @return
	 *    преобразованный алфавит
	 */
	private static String translateAlphabet(String alphabet, Map<Character, Character> map) {
		String translated = "";
		for (int i = 0; i < alphabet.length(); i++) {
			char ch = alphabet.charAt(i);
			char transCh = map.containsKey(ch) ? map.get(ch) : ch;
			if (!translated.contains("" + transCh)) {
				translated += transCh;
			}
		}
		return translated;
	}
	
	/**
	 * Создает отображение для байтовых последовательностей состояний.
	 * 
	 * @param alphabet
	 *    алфавит состояний, подвергающийся преобразованию
	 * @param map
	 *    отображение алфавита
	 * @return
	 *    массив, соответствующий байтовому отображению. <code>i</code>-й элемент
	 *    массива равен номеру состояния, в который отображается <code>i</code>-й
	 *    символ исходного алфавита состояний
	 */
	private static byte[] stateMap(String alphabet, Map<Character, Character> map) {
		byte[] byteMap = new byte[alphabet.length()];
		String translated = "";
		for (int i = 0; i < alphabet.length(); i++) {
			char ch = alphabet.charAt(i);
			char transCh = map.containsKey(ch) ? map.get(ch) : ch;
			if (!translated.contains("" + transCh)) {
				translated += transCh;
			}
			
			byteMap[i] = (byte)translated.indexOf(transCh);
		}
		return byteMap;
	}
	
	/**
	 * Преобразует последовательность наблюдаемых или скрытых состояний
	 * в соответствии с заданным байтовым отображением. 
	 * 
	 * @param sequence
	 *    строка состояний, которую необходимо преобразовать
	 * @param map
	 *    отображение состояний
	 * @return
	 *    преобразованная строка состояний
	 */
	private static byte[] translate(byte[] sequence, byte[] map) {
		byte[] translated = new byte[sequence.length];
		for (int i = 0; i < sequence.length; i++) {
			translated[i] = map[sequence[i]];
		}
		return translated;
	}
	
	/**
	 * Производит отображение наблюдаемых и/или скрытых состояний для заданной выборки.
	 * 
	 * <p>Например, для преобразования выборки белков, полученной из файлов DSSP (т.е. содержащей
	 * восемь скрытых состояний {@code "-GHIEBTS"}), к формату, соответствующему стандартной
	 * задаче распознавания (с тремя скрытыми состояниями {@code "-HS"}), следует выполнить код
	 * <pre>
	 * SequenceSet proteins = ...;
	 * Map&lt;Character, Character&gt; map = SequenceUtils.translationMap("-TSGHIEB:---HHHSS");
	 * SequenceSet translated = SequenceUtils.translateStates(set, null, map);
	 * </pre>
	 * 
	 * @param set
	 *    выборка, для которой производится отображение
	 * @param observedMap
	 *    отображение наблюдаемых состояний выборки; {@code null}, чтобы не отображать
	 *    наблюдаемые состояния
	 * @param hiddenMap
	 *    отображение скрытых состояний выборки; {@code null}, чтобы не отображать
	 *    скрытые состояния
	 * @return
	 *    преобразованная выборка
	 */
	public static SequenceSet translateStates(SequenceSet set, Map<Character, Character> observedMap, 
			Map<Character, Character> hiddenMap) {
		
		if (observedMap == null) observedMap = new HashMap<Character, Character>();
		if (hiddenMap == null) hiddenMap = new HashMap<Character, Character>();
		
		String trObservedStates = translateAlphabet(set.observedStates(), observedMap);
		String trHiddenStates = translateAlphabet(set.hiddenStates(), hiddenMap);
		
		byte[] observedStateMap = stateMap(set.observedStates(), observedMap);
		byte[] hiddenStateMap = stateMap(set.hiddenStates(), hiddenMap);
		
		GenericSequenceSet trSet = new GenericSequenceSet(trObservedStates, trHiddenStates, null);
		for (int i = 0; i < set.length(); i++) {
			byte[] observed = translate(set.observed(i), observedStateMap);
			byte[] hidden = translate(set.hidden(i), hiddenStateMap);
			trSet.doAdd(observed, hidden, set.id(i));
		}
		
		return trSet;
	}
}

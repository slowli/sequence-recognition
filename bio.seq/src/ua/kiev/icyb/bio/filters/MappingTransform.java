package ua.kiev.icyb.bio.filters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.StatesDescription;
import ua.kiev.icyb.bio.Transform;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Преобразование строк на основе заданных отображений множеств наблюдаемых
 * и скрытых состояний.
 * 
 * <p><b>Пример.</b> Преобразование из формата белков в файлах DSSP (7 скрытых состояний)
 * к стандартному набору из трех скрытых состояний осуществляется с помощью преобразования
 * <pre>
 * Map<Character, Character> map = MappingTransform.map("-TSGHIEB:---HHHSS");
 * Transform transform = new MappingTransform(MappingTransform.TRIVIAL_MAP, map);
 * </pre>
 */
public class MappingTransform implements Transform {

	/**
	 * Тривиальное отображение состояний.
	 */
	public static final Map<Character, Character> TRIVIAL_MAP = Collections.emptyMap();
	
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
	public static Map<Character, Character> map(String map) {
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
	
	private final Map<Character, Character> observedMap;
	
	private final Map<Character, Character> hiddenMap;
	
	/**
	 * Создает преобразование.
	 * 
	 * @param observedMap
	 *    отображение наблюдаемых состояний
	 * @param hiddenMap
	 *    отображение скрытых состояний
	 */
	public MappingTransform(Map<Character, Character> observedMap, Map<Character, Character> hiddenMap) {
		this.observedMap = observedMap;
		this.hiddenMap = hiddenMap;
	}
	
	@Override
	public StatesDescription states(StatesDescription original) {
		return StatesDescription.create(
				translateAlphabet(original.observed(), this.observedMap),
				translateAlphabet(original.hidden(), this.hiddenMap));
	}

	@Override
	public Sequence sequence(Sequence original) {
		final byte[] oMap = stateMap(original.states().observed(), this.observedMap);
		final byte[] hMap = stateMap(original.states().hidden(), this.hiddenMap);
		
		return new Sequence(original.id, 
				translate(original.observed, oMap),
				translate(original.hidden, hMap));
	}

	@Override
	public Sequence inverse(Sequence transformed) {
		throw new UnsupportedOperationException("Transform is irreversible");
	}

	@Override
	public String repr() {
		return Messages.getString("transform.map");
	}
}

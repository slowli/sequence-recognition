package ua.kiev.icyb.bio.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.GenericSequenceSet;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.io.res.Messages;

/**
 * Класс для преобразования файлов в формате DSSP, описывающими пространственную структуру 
 * конкретного белка.
 */
public class DSSPReader {
	
	/**
	 * Регулярное выражение для извлечения идентификатора белка. Первая группа соответствует имени.
	 */
	private static final Pattern ID_REGEX = Pattern.compile("^HEADER.*\\s(\\w+)\\s*.$");
	
	/**
	 * Регулярное выражение для извлечения имени белка. Первая группа соответствует имени.
	 */
	private static final Pattern NAME_REGEX = Pattern.compile("MOLECULE:\\s*(.*[^;\\s]);?\\s*\\.$");
	
	/**
	 * Алфавит аминокислот.
	 */
	private static final String AMINO_ACIDS = "ACDEFGHIKLMNPQRSTVWY";
	/**
	 * Алфавит вторичных структур.
	 */
	private static final String STRUCTURES = " GHIEBTS";
	
	/**
	 * Алфавит наблюдаемых состояний в формируемой выборке — аминокислоты плюс состояние для неизвестных
	 * аминокислот. Неизвестные аминокислоты обозначаются {@code '?'}.
	 */
	private static final String OBSERVED_STATES = AMINO_ACIDS + "?";
	/**
	 * Алфавит скрытых состояний в формируемой выборке.
	 */
	private static final String HIDDEN_STATES = STRUCTURES.replace(' ', '-');
	
	/**
	 * Регулярное выражение для извлечения информации об отдельных аминокислотах 
	 * и соответствующих вторичных структурах. Аминокислота хранится в первой группе,
	 * стркутура — во второй.
	 */
	private static final Pattern LINE_REGEX = Pattern.compile("^[\\w\\s]{13}(.).{2}(.)");
	
	/**
	 * Имена белков, обработанных этим объектом.
	 */
	private final Set<String> proteinNames = new HashSet<String>();
	/**
	 * Префиксы белков, обработанных этим объектом.
	 */
	private final Set<String> proteinPrefixes = new HashSet<String>();
	
	/**
	 * Формировать ли выборку из белков, уникальных по имени.
	 */
	public boolean uniqueNames = true;
	
	/**
	 * Формировать ли выборку из белков с уникальным началом аминокислотной
	 * последовательности.
	 */
	public boolean uniquePrefix = false;
	
	/**
	 * Длина начала аминокислотной цепочки белка, проверяемая на уникальность при
	 * при включенном значении поля {@link #uniquePrefix}. 
	 */
	public int prefixLength = 10;
	
	/**
	 * Включать ли разрывы аминокислотной последовательности белка как неизвестные аминокислоты.
	 */
	public boolean includeBreaks = false;
	
	/**
	 * Выборка, состоящая из прочитанных белков.
	 */
	private final GenericSequenceSet proteins = new GenericSequenceSet(
			OBSERVED_STATES, HIDDEN_STATES, null);
	
	public DSSPReader() {
	}

	/**
	 * Считывает белок из указанного файла и добавляет его в выборку.
	 * При определенных условиях белок может не добавляться (например, если
	 * свойство {@link #uniqueNames} установлено в {@code true} и белок с тем же
	 * именем уже есть в выборке).
	 * 
	 * @param filename
	 *    имя DSSP-файла
	 * @throws IOException
	 *    если при чтении файла произошла ошибка ввода/вывода
	 */
	public void read(String filename) throws IOException {
		Env.debug(1, Messages.format("dssp.file", filename));
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line, proteinId = null;
		Matcher matcher;
		boolean header = true;
		
		StringBuilder aminoAcids = new StringBuilder(), structures = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			matcher = ID_REGEX.matcher(line);
			if (matcher.find()) {
				proteinId = matcher.group(1);
			}
			
			matcher = NAME_REGEX.matcher(line);
			if (matcher.find()) {
				String name = matcher.group(1);
				
				if (proteinNames.contains(name)) {
					reader.close();
					return;
				}
				if (this.uniqueNames) {
					proteinNames.add(name);
				}
			}
			
			if (!line.endsWith(".")) {
				header = false;
			}
			if (!header) {
				matcher = LINE_REGEX.matcher(line);
				if (matcher.find()) {
					aminoAcids.append(matcher.group(1));
					structures.append(matcher.group(2));
				}
			}
		}
		reader.close();
		
		String prefix = (aminoAcids.length() > this.prefixLength) 
				? aminoAcids.substring(0, this.prefixLength)
				: aminoAcids.toString();
		if (proteinPrefixes.contains(prefix)) {
			return;
		}
		if (this.uniquePrefix) {
			proteinPrefixes.add(prefix);
		}

		addSequence(aminoAcids.toString(), structures.toString(), proteinId);
	}
	
	/**
	 * Добавляет белок в выборку.
	 * 
	 * @param aminoAcids
	 *    строка аминокислот белка
	 * @param structures
	 *    строка обозначений вторичных структур, соответствующих аминокислотам
	 * @param id
	 *    идентификатор белка
	 */
	private void addSequence(String aminoAcids, String structures, String id) {
		int length = aminoAcids.length();
		if (!this.includeBreaks) {
			for (int i = 0; i < aminoAcids.length(); i++) {
				if (aminoAcids.charAt(i) == '!') {
					length--;
				}
			}
		}
		
		byte[] observed = new byte[length];
		byte[] hidden = new byte[length];
		
		for (int i = 0; i < observed.length; i++) {
			char aa = aminoAcids.charAt(i);
			if (Character.isLowerCase(aa)) {
				// Цистеин с дисульфидной связью
				aa = 'C';
			} else if (!this.includeBreaks && (aa == '!')) {
				continue;
			}
			
			int pos = AMINO_ACIDS.indexOf(aa);
			if (pos < 0) {
				pos = AMINO_ACIDS.length();
			}
			observed[i] = (byte) pos;
			
			pos = STRUCTURES.indexOf(structures.charAt(i));
			if (pos < 0) {
				throw new IllegalStateException(Messages.format("dssp.e_struct", structures.charAt(i)));
			}
			hidden[i] = (byte) pos;
		}
		proteins.add(observed, hidden, id);
	}
	
	/**
	 * Возвращает текущую выборку белков. 
	 * 
	 * @return
	 *		выборка последовательностей, соответствующих белкам
	 */
	public SequenceSet getSet() {
		return proteins;
	}
}

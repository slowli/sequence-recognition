package ua.kiev.icyb.bio.alg.tree;

import java.io.Serializable;

import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Бинарное дерево предикатов, которое может использоваться для построения
 * областей компетентности для композиций алгоритмов распознавания.
 */
public class PartitionRuleTree implements Serializable, Representable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Максимальное число правил в дереве.
	 */
	private static final int MAX_RULES = 20;
	
	/**
	 * Массив правил в порядке построения дерева.
	 */
	private final PartitionRule[] rules = new PartitionRule[MAX_RULES];
	/**
	 * Массив, <code>i</code>-й элемент которого равен номеру части выборки, которую разбивает
	 * <code>i</code>-е правило.
	 */
	private final int[] parts = new int[MAX_RULES];
	
	/**
	 * Число правил в этом дереве.
	 */
	private int nRules = 0;
	
	/**
	 * Возвращает число частей, на которые это дерево делит пространство
	 * строк наблюдаемых состояний. Это число на единицу больше количество правил
	 * в дереве.
	 * 
	 * @return
	 *    число частей разбиения
	 */
	public int size() {
		return nRules + 1;
	}
	
	/**
	 * Создает пустое дерево предикатов.
	 */
	public PartitionRuleTree() {
	}
	
	/**
	 * Добавляет в дерево новое решающее правило.
	 * 
	 * @param rule 
	 *    добавляемое правило
	 * @param part
	 *    индекс (с отсчетом от нуля) части выборки, которое правило разбивает
	 */
	public void add(PartitionRule rule, int part) {
		assert(part <= nRules);
		parts[nRules] = part;
		rules[nRules] = rule;
		nRules++;
	}
	
	/**
	 * Возвращает часть разбиения, порождаемого этим деревом, которая содержит
	 * заданную строку состояний.
	 * 
	 * @param seq
	 *    строка наблюдаемых состояний
	 * @return
	 *    индекс (с отсчетом от нуля) части разбиения, содержащей строку
	 */
	public int getPart(byte[] seq) {
		int curPart = 0;
		for (int r = 0; r < nRules; r++)
			if ((parts[r] == curPart) && (rules[r].test(seq)))
				curPart = r + 1;
		
		return curPart;
	}
	
	/**
	 * Разбивает выборку на части согласно этому дереву разбиения.
	 * 
	 * @param set
	 *    выборка для разбиения
	 * @return
	 *    индексы (с отсчетом от нуля) частей разбиения, содержащих каждую наблюдаемую
	 *    строку из выборки
	 */
	public int[] split(SequenceSet set) {
		int[] result = new int[set.length()];
		for (int i = 0; i < result.length; i++)
			result[i] = getPart(set.observed(i));
		return result;
	}
	
	/**
	 * Обрезает дерево.
	 * 
	 * @param newSize
	 *    новый размер дерева
	 */
	public void trim(int newSize) {
		assert(newSize >= 0);
		for (int i = newSize; i < nRules; i++) {
			rules[i] = null; // Free memory
		}
		nRules = Math.min(nRules, newSize);
		nRules = Math.max(nRules, 0);
	}
	
	public String repr() {
		String str = "";
		for (int i = 0; i < nRules; i++)
			str += Messages.format("tree.repr_rule", i + 1, parts[i] + 1, rules[i]) + "\n";
		if (nRules == 0) {
			str += Messages.getString("tree.no_rules");
		}
		return str;
	}
}

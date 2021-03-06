package ua.kiev.icyb.bio.alg.tree;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.filters.LabelFilter;
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
	 * Создает пустое дерево предикатов.
	 */
	public PartitionRuleTree() {
	}

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
	 * Возвращает правило, входящее в это дерево.
	 * 
	 * @param index
	 *    индекс правила (с отсчетом от нуля)
	 * @return
	 *    правило с заданным индексом
	 */
	public PartitionRule rule(int index) {
		if (index >= size()) {
			throw new IndexOutOfBoundsException("Invalid index: " + index);
		}
		return this.rules[index];
	}
	
	/**
	 * Возовращает корневую вершину этого дерева разбиения.
	 * 
	 * @return
	 *    корень дерева разбиения
	 */
	public Partition root() {
		Partition root = new Partition();
		
		for (int i = 0; i < this.size() - 1; i++) {
			for (Partition part : root.leaves()) {
				if (part.index() == this.parts[i]) {
					part.split(this.rule(i));
					break;
				}
			}
		}
		
		return root;
	}
	
	/**
	 * Возвращает терминальные вершины дерева разбиения.
	 * 
	 * @return
	 *    список терминальных вершин. Порядок перечисления вершин соответствует их индексам.
	 */
	public List<Partition> leaves() {
		List<Partition> leaves = this.root().leaves();
		Collections.sort(leaves, Partition.INDEX_SORTER);
		return leaves;
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
		if (part > nRules) {
			throw new IndexOutOfBoundsException("Invalid number of part to split: " + part);
		}
		
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
	public SequenceSet[] split(SequenceSet set) {
		Map<String, Integer> labels = this.getLabels(set);
		
		SequenceSet[] subsets = new SequenceSet[this.size()];
		for (int i = 0; i < subsets.length; i++) {
			subsets[i] = set.filter(new LabelFilter(labels, i));
		}
		return subsets;
	}
	
	/**
	 * Возвращает метки для строк выборки согласно этому дереву разбиения.
	 * 
	 * @param set
	 *    выборка для разбиения
	 * @return
	 *    метки для всех строк выборки, соответствующие номеру части разбиения, в которую
	 *    попадает конкретная строка
	 */
	public Map<String, Integer> getLabels(SequenceSet set) {
		Map<String, Integer> labels = new HashMap<String, Integer>();
		for (Sequence sequence : set) {
			labels.put(sequence.id, getPart(sequence.observed));
		}
		
		return labels;
	}
	
	/**
	 * Обрезает дерево, удаляя из него правила, начиная с последнего.
	 * 
	 * @param newSize
	 *    новый размер дерева, то есть количество правил в нем
	 */
	public void trim(int newSize) {
		if (newSize <= 0) {
			throw new IllegalArgumentException("Tree size must be positive: " + newSize);
		}
		
		for (int i = newSize; i < nRules; i++) {
			rules[i] = null; // Освободить память
		}
		this.nRules = Math.min(nRules, newSize);
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

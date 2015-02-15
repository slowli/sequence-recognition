package ua.kiev.icyb.bio.alg.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ua.kiev.icyb.bio.Representable;

/**
 * Составляющая разбиения пространства строк наблюдаемых состояний.
 * Составляющая является вершиной (возможно, внутренней) дерева предикатов {@link PartitionRuleTree}.
 */
public class Partition implements Representable {
	
	/**
	 * Сортировщик компонент разбиения по их индексу.
	 */
	public static final Comparator<Partition> INDEX_SORTER = new Comparator<Partition>() {

		@Override
		public int compare(Partition a, Partition b) {
			return Integer.compare(a.index(), b.index());
		}
	};
	
	private final PartitionRule rule;
	
	private final int index;
	
	private final Partition parent;
	
	private Partition leftChild;
	
	private Partition rightChild;
	
	/**
	 * Создает компоненту разбиения, которая охватывает все пространство 
	 * строк наблюдаемых состояний.
	 */
	Partition() {
		this.rule = new TrivialPartitionRule();
		this.parent = null;
		this.index = 0;
	}
	
	/**
	 * Создает компоненту разбиения с заданными параметрами.
	 * 
	 * @param parent
	 *    родительская компонента разбиения
	 * @param rule
	 *    правило, согласно которому разбивается робительская компонента
	 * @param index
	 *    индекс этой компоненты
	 */
	private Partition(Partition parent, PartitionRule splittingRule, int index) {
		this.parent = parent;
		this.rule = parent.rule().and(splittingRule);
		this.index = index;
	}
	
	/**
	 * Возвращает предикат, описывающий эту компоненту разбиения.
	 * 
	 * @return
	 *    предикат, истинный на строках, принадлежащих этой компоненте, и ложный на всех
	 *    остальных строках наблюдаемых состояний
	 */
	public PartitionRule rule() {
		return this.rule;
	}
	
	/**
	 * Возвращает родительскую компоненту разбиения.
	 * 
	 * @return
	 *    родитель этой компоненты или {@code null}, если компонента является корнем дерева
	 *    разбиения
	 */
	public Partition parent() {
		return this.parent;
	}
	
	/**
	 * Возвращает корень дерева разбиения, в которое входит эта компонента
	 * 
	 * @return
	 *    корень дерева разбиения
	 */
	public Partition root() {
		Partition root = this;
		while (root.parent() != null) {
			root = root.parent();
		}
		return root;
	}
	
	/**
	 * Возвращает левую дочернюю вершину этой компоненты разбиения.
	 * 
	 * @return
	 *    левая дочерняя вершина, или {@code null}, если эта компонента является листом дерева разбиения
	 */
	public Partition leftChild() {
		return this.leftChild;
	}
	
	/**
	 * Возвращает правую дочернюю вершину для этой компоненты разбиения.
	 * 
	 * @return
	 *    правая дочерняя вершина, или {@code null}, если эта компонента является листом дерева разбиения
	 */
	public Partition rightChild() {
		return this.rightChild;
	}
	
	/**
	 * Возвращает индекс этой компоненты разбиения в дереве.
	 * 
	 * @return
	 *    индекс (с отсчетом от нуля) компоненты разбиения
	 */
	public int index() {
		return this.index;
	}
	
	/**
	 * Подсчитывает количество листьев в поддереве дерева разбиения с корнем в этой
	 * компоненте. Если компонента сама является листом, возвращается {@code 1}.
	 * 
	 * @return
	 *    число листьев
	 */
	public int numberOfLeaves() {
		if (this.isLeaf()) return 1;
		
		int nLeaves = 0;
		if (this.leftChild() != null) {
			nLeaves += this.leftChild().numberOfLeaves();
		}
		if (this.rightChild() != null) {
			nLeaves += this.rightChild().numberOfLeaves();
		}
		
		return nLeaves;
	}
	
	/**
	 * Возвращает список листьев в поддереве дерева разбиения с корнем в этой
	 * компоненте.
	 * 
	 * @return
	 *    список компонент разбиения
	 */
	public List<Partition> leaves() {
		if (this.isLeaf()) return Collections.singletonList(this);
		
		List<Partition> leaves = new ArrayList<Partition>();
		if (this.leftChild() != null) {
			leaves.addAll(this.leftChild().leaves());
		}
		if (this.rightChild() != null) {
			leaves.addAll(this.rightChild().leaves());
		}
		
		return leaves;
	}
	
	/**
	 * Проверяет, является ли эта компонента разбиения листом в дереве разбиения.
	 * 
	 * @return
	 *    {@code true}, если компонента является листом дерева разбиения
	 */
	public boolean isLeaf() {
		return (this.leftChild() == null) && (this.rightChild() == null);
	}
	
	/**
	 * Разбивает область на две части и добавляет их как дочерние вершины этой
	 * компоненты разбиения.
	 * 
	 * @param splittingRule
	 *    предикат, согласно которому проводится разбиение компоненты
	 */
	void split(PartitionRule splittingRule) {
		this.leftChild = new Partition(this, splittingRule.not(), this.index);
		this.rightChild = new Partition(this, splittingRule, this.root().numberOfLeaves());
	}
	
	@Override
	public String toString() {
		return index() + ":" + rule().toString();
	}

	/**
	 * Рекурсивно создает текстовое представление поддерева дерева разбиения с корнем в этой
	 * компоненте.
	 * 
	 * @param prefix
	 *    префик, позволяющий регулировать отступ для дочерних вершин компоненты 
	 * @return
	 *    строковое представление поддерева
	 */
	private String printTree(String prefix) {
		String str = prefix + this.index() + ": " + rule().toString() + "\n";
		if (this.leftChild() != null) {
			str += this.leftChild().printTree(prefix + "  ");
		}
		if (this.rightChild() != null) {
			str += this.rightChild().printTree(prefix + "  ");
		}
		
		return str;
	}
	
	@Override
	public String repr() {
		return this.printTree("");
	}
}

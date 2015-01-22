package ua.kiev.icyb.bio.alg.tree;

import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.Sequence;

/**
 * Подкласс композиций алгоритмов распознавания, в которых области компетентности
 * определяются на основе бинарного дерева предикатов.
 * 
 * @see PartitionRuleTree
 */
public class TreeSwitchAlgorithm extends SwitchAlgorithm {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Дерево предикатов, используемое алгоритмом.
	 */
	private final PartitionRuleTree tree;
	
	/**
	 * Создает алгоритм на основе заданного дерева предикатов.
	 * 
	 * @param tree
	 *    дерево предикатов
	 * @param algs
	 *    составляющие алгоритмы распознавания
	 */
	public TreeSwitchAlgorithm(PartitionRuleTree tree, SeqAlgorithm[] algs) {
		super(algs);
		this.tree = tree;
	}
	
	@Override
	public int index(Sequence sequence) {
		return tree.getPart(sequence.observed);
	}
}

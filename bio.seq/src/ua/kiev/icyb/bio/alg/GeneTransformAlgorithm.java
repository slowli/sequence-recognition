package ua.kiev.icyb.bio.alg;

import ua.kiev.icyb.bio.Transform;
import ua.kiev.icyb.bio.filters.PeriodicTransform;
import ua.kiev.icyb.bio.filters.TerminalTransform;
import ua.kiev.icyb.bio.filters.TransformComposition;

/**
 * Алгоритм для распознавания фрагментов генов, использующий преобразование
 * входных последовательностей.
 */
public class GeneTransformAlgorithm extends TransformAlgorithm {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Преобразование последовательностей, применяемое алгоритмом.
	 */
	private static final Transform GENE_TRANSFORM = new TransformComposition(
			new PeriodicTransform(), new TerminalTransform());
	
	/**
	 * Создает новый алгоритм с использованием базового алгоритма Витерби. 
	 * 
	 * @param order
	 *    порядок скрытых марковских моделей, используемых в алгоритме
	 */
	public GeneTransformAlgorithm(int order) {
		super(new ViterbiAlgorithm(1, order), GENE_TRANSFORM);
	}
}

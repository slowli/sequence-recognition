package ua.kiev.icyb.bio.alg;

import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.StatesDescription;
import ua.kiev.icyb.bio.Transform;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Алгоритм распознавания, использующий определенное пребразование последовательностей выборки
 * для модификации работы базового алгоритма.
 * 
 * <p>При обучении поданные на вход строки преобразуются в соответствии с заданной трансформацией {@code t}.
 * При распознавании происходит следующее:
 * <ol>
 * <li>входная последовательность преобразуется {@code t};
 * <li>полученная строка подается на вход базового алгоритма;
 * <li>результат работы базового алгоритма преобразуется обратной трансформацией <code>t<sup>-1</sup></code>.
 * </ol>
 */
public class TransformAlgorithm extends AbstractSeqAlgorithm {

	private static final long serialVersionUID = 1L;

	private SeqAlgorithm base;
	
	private Transform transform;
	
	/**
	 * Создает алгоритм распознавания.
	 * 
	 * @param base
	 *    базовый алгоритм
	 * @param transform
	 *    преобразование, применяемое к строкам выборки при обучении и распознавании
	 */
	public TransformAlgorithm(SeqAlgorithm base, Transform transform) {
		this.base = base;
		this.transform = transform;
	}
	
	@Override
	public byte[] run(Sequence sequence) {
		sequence = this.transform.sequence(sequence);
		byte[] hidden = this.base.run(sequence);
		
		if (hidden == null) return null;
		
		Sequence result = new Sequence(sequence.id, 
				sequence.observed, 
				hidden);
		return this.transform.inverse(result).hidden;
	}

	@Override
	public void reset() {
		this.base.reset();
	}

	@Override
	public void train(Sequence sequence) {
		StatesDescription states = this.transform.states(sequence.states());
		this.base.train(this.transform.sequence(sequence).setStates(states));
	}
	
	@Override
	public TransformAlgorithm clone() {
		TransformAlgorithm other = (TransformAlgorithm) super.clone();
		other.base = this.base.clone();
		return other;
	}
	
	@Override
	public TransformAlgorithm clearClone() {
		TransformAlgorithm other = (TransformAlgorithm) super.clone();
		other.base = this.base.clearClone();
		return other;
	}
	
	@Override
	public String repr() {
		String repr = super.repr() + "\n";
		repr += Messages.format("alg.base", this.base.repr()) + "\n";
		repr += Messages.format("alg.transform", this.transform.repr());
		return repr;
	}
}

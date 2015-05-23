package ua.kiev.icyb.bio.filters;

import java.io.Serializable;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.StatesDescription;
import ua.kiev.icyb.bio.Transform;

/**
 * Композиция из нескольких преобразований.
 */
public class TransformComposition implements Transform, Serializable {

	private static final long serialVersionUID = 1L;
	
	
	private final Transform[] transforms;
	
	/**
	 * Создает композицию. Преобразования применяются в указанном порядке;
	 * при поиске обратной последовательности - в обратном порядке. 
	 * 
	 * @param transforms
	 *    последовательность преобразований
	 */
	public TransformComposition(Transform... transforms) {
		this.transforms = transforms;
	}
	
	@Override
	public StatesDescription states(StatesDescription original) {
		StatesDescription states = original;
		for (Transform t : this.transforms) {
			states = t.states(states);
		}
		return states;
	}

	@Override
	public Sequence sequence(Sequence original) {
		Sequence seq = original;
		for (Transform t : this.transforms) {
			seq = t.sequence(seq);
		}
		return seq;
	}

	@Override
	public Sequence inverse(Sequence transformed) {
		Sequence seq = transformed;
		for (int i = this.transforms.length - 1; i >= 0; i--) {
			seq = this.transforms[i].inverse(seq);
		}
		return seq;
	}
}

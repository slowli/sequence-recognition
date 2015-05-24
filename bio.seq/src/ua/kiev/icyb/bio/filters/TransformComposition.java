package ua.kiev.icyb.bio.filters;

import java.io.Serializable;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.StatesDescription;
import ua.kiev.icyb.bio.Transform;
import ua.kiev.icyb.bio.res.Messages;

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

	@Override
	public String repr() {
		String repr = Messages.getString("transform.comp") + "\n";
		String list = "";
		for (int i = 0; i < transforms.length; i++) {
			if (i > 0) list += "\n";
			list += Messages.format("transform.comp.part", i + 1, transforms[i].repr());
		}
		repr += Messages.format("transform.comp.parts", list);
		return repr;
	}
}

package ua.kiev.icyb.bio.filters;

import java.io.Serializable;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.StatesDescription;
import ua.kiev.icyb.bio.Transform;

/**
 * Преобразование, позволяющее (совместно с {@link TerminalTransform}) ограничить
 * генерируемые марковской цепью последовательности "корректными" генами.
 * Под корректными генами подразумеваются последовательности, для которых
 * суммарное количество нуклеотидов в экзонах кратно трем.
 * 
 * <p>Корректность генов достигается путем расширения множества скрытых состояний до шести:
 * три состояния соответствуют экзонам (x, y, z) и три - интронам (i, j, k). 
 * В пределах экзонов состояния чередуются, а в интронах - запоминаются.
 * 
 * <p><b>Пример.</b> Пусть есть последовательность
 * <pre>
 * observed: ATGCAGTTAGCC...
 * hidden:   xxxxiiiiiixx...
 * </pre> 
 * После преобразования получится последовательность
 * <pre>
 * observed: ATGCAGTTAGCC...
 * hidden:   xyzxjjjjjjyz...
 * </pre>
 * 
 * Таким образом, при обучении на преобразованных последовательностях из скрытого состояния
 * x можно перейти только в y или j; из j - в j или y, и так далее. Любой правильный ген заканчивается
 * скрытым состоянием z.
 */
public class PeriodicTransform implements Transform, Serializable {

	private static final long serialVersionUID = 1L;
	

	@Override
	public StatesDescription states(StatesDescription original) {
		if (!original.hidden().equals("xi"))
			throw new IllegalArgumentException("Transform may be used for genes only");
		return StatesDescription.create(original.observed(), "xyzijk");
	}

	@Override
	public Sequence sequence(Sequence original) {
		byte[] hidden = new byte[original.length()];
		int mod = 0;
		for (int i = 0; i < original.length(); i++) {
			hidden[i] = (byte) (original.hidden[i] * 3 + mod);
			if (original.hidden[i] == 0) {
				mod = (mod + 1) % 3;
			}
		}
		
		return new Sequence(original.id, original.observed, hidden)
				.setStates(this.states(original.states()));
	}

	@Override
	public Sequence inverse(Sequence transformed) {
		byte[] hidden = new byte[transformed.length()];
		for (int i = 0; i < transformed.length(); i++) {
			hidden[i] = (byte) (transformed.hidden[i] / 3);
		}
		
		return new Sequence(transformed.observed, hidden);
	}

}

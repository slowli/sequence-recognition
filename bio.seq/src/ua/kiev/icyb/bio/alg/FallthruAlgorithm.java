package ua.kiev.icyb.bio.alg;

import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Алгоритм распознавания, использующий вероятностную модель с
 * аппроксимацией отсутствующих начальных и переходных вероятностей.
 */
public class FallthruAlgorithm extends ViterbiAlgorithm {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Параметры аппроксимации неизвестных вероятностей.
	 */
	private final Approximation approx;

	/**
	 * Создает новый алгоритм распознавания, использующий вероятностную
	 * модель с аппроксимацией.
	 * 
	 * @param approx
	 *    параметры аппроксимации
	 * @param set
	 *    образец выборки, используемый для определения алфавитов наблюдаемых и скрытых
	 *    состояний
	 */
	public FallthruAlgorithm(Approximation approx, SequenceSet set) {
		super(new FallthruChain(approx, 
				set.observedStates(), set.hiddenStates()));
		this.approx = approx;
	}
	
	@Override
	public String repr() {
		String repr = super.repr() + "\n";
		repr += Messages.format("alg.approx", approx.strategy, approx.minOrder);
		return repr;
	}
}

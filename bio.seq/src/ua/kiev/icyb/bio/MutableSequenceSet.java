package ua.kiev.icyb.bio;

import ua.kiev.icyb.bio.SimpleSequenceSet;

/**
 * Выборка, в которую можно добавлять строки.
 */
public class MutableSequenceSet extends SimpleSequenceSet {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Создает новую пустую выборку.
	 * 
	 * @param observedStates
	 *    алфавит наблюдаемых состояний
	 * @param hiddenStates
	 *    алфавит скрытых состояний
	 * @param completeStates
	 *    алфавит полных состояний (может быть равен {@code null})
	 */
	public MutableSequenceSet(String observedStates, String hiddenStates, String completeStates) {
		super(observedStates, hiddenStates, completeStates);
	}

	/**
	 * Добавляет в коллекцию пару из наблюдаемой и соответстующей скрытой 
	 * последовательности состояний.
	 * 
	 * @param observed
	 *    строка наблюдаемых состояний
	 * @param hidden
	 *    строка скрытых состояний
	 */
	public void add(byte[] observed, byte[] hidden, String id) {
		this.doAdd(new Sequence(null, -1, id, observed, hidden));
	}
	
	/**
	 * Добавляет в коллекцию пару из наблюдаемой и соответстующей скрытой 
	 * последовательности состояний.
	 * 
	 * @param sequence
	 *    добавляемый объект
	 */
	public void add(Sequence sequence) {
		this.doAdd(sequence);
	}
}

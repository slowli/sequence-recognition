package ua.kiev.icyb.bio.alg;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Имплементация интерфейса {@link SequenceSet}, хранящая исключительно строки
 * скрытых состояний.
 * 
 * Используется в алгоритмах распознавания скрытых последовательностей для хранения
 * результатов работы.
 */
public final class EstimatesSet implements SequenceSet {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Выборка, на которой будет работать алгоритм распознавания.
	 */
	private final SequenceSet baseSet;
	
	/** Последовательности скрытых состояний. */
	private final byte[][] hidden;

	/**
	 * Создает контейнер для сохранения результатов работы алгоритма распознавания
	 * скрытых последовательностей. 
	 * 
	 * @param set
	 *    выборка, на которой будет работать алгоритм распознавания
	 */
	public EstimatesSet(SequenceSet set) {
		baseSet = set;
		hidden = new byte[set.length()][];
	}

	/**
	 * Сохраняет последовательность скрытых состояний в этом наборк.
	 * 
	 * @param index
	 *        индекс (с отсчетом от нуля) наблюдаемой последовательности,
	 *        для которой найдены соответствующие скрытые состояния
	 * @param array
	 *        последовательность скрытых состояний для сохранения
	 */
	public void put(int index, byte[] array) {
		hidden[index] = array;
	}

	@Override
	public int length() {
		return hidden.length;
	}

	@Override
	public String observedStates() {
		return baseSet.observedStates();
	}

	@Override
	public String hiddenStates() {
		return baseSet.hiddenStates();
	}
	
	@Override
	public String completeStates() {
		return baseSet.completeStates();
	}

	@Override
	public byte[] observed(int index) {
		return baseSet.observed(index);
	}

	@Override
	public byte[] hidden(int index) {
		return hidden[index];
	}
	
	@Override
	public String id(int index) {
		return baseSet.id(index);
	}

	@Override
	public void saveToFile(String fileName) throws IOException {
		// TODO ???
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
		writer.write(hidden.length + "\n");
		for (int i = 0; i < hidden.length; i++) {
			writer.write(i + ":");
			for (int pos = 0; pos < hidden[i].length; pos++)
				writer.write('0' + hidden[i][pos]);
			writer.write("\n");
		}
		writer.close();
	}

	/**
	 * <strong>Операция не реализована.</strong>
	 * <p>{@inheritDoc}
	 * 
	 * @throws UnsupportedOperationException всегда
	 */
	@Override
	public SequenceSet filter(boolean[] selector) {
		throw new UnsupportedOperationException();
	}

	/**
	 * <strong>Операция не реализована.</strong>
	 * <p>{@inheritDoc}
	 * 
	 * @throws UnsupportedOperationException всегда
	 */
	@Override
	public SequenceSet filter(Filter filter) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String repr() {
		return Messages.format("dataset.est", baseSet.repr());
	}
}
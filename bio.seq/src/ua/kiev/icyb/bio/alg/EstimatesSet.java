package ua.kiev.icyb.bio.alg;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.SimpleSequenceSet;

/**
 * Имплементация интерфейса {@link SequenceSet}, хранящая исключительно строки
 * скрытых состояний.
 * 
 * Используется в алгоритмах распознавания скрытых последовательностей для хранения
 * результатов работы.
 */
class EstimatesSet extends SimpleSequenceSet {
	
	private static final long serialVersionUID = 1L;
	
	public final SequenceSet baseSet;
	
	private final byte[][] hidden; 
	
	/**
	 * Создает контейнер для сохранения результатов работы алгоритма распознавания
	 * скрытых последовательностей. 
	 * 
	 * @param set
	 *    выборка, на которой будет работать алгоритм распознавания
	 */
	public EstimatesSet(SequenceSet set) {
		super(set.observedStates(), set.hiddenStates(), set.completeStates());
		this.baseSet = set;
		
		this.hidden = new byte[set.size()][];
		for (int i = 0; i < set.size(); i++) {
			this.hidden[i] = new byte[set.observed(i).length];
			this.doAdd(new Sequence(set.id(i), set.observed(i), null));
		}
	}

	/**
	 * Сохраняет последовательность скрытых состояний в этом наборк.
	 * 
	 * @param index
	 *    индекс (с отсчетом от нуля) наблюдаемой последовательности,
	 *    для которой найдены соответствующие скрытые состояния
	 * @param array
	 *    последовательность скрытых состояний для сохранения
	 */
	public void put(int index, byte[] array) {
		hidden[index] = array;
	}
	
	@Override
	public byte[] hidden(int index) {
		return hidden[index];
	}
	
	@Override
	public void saveToFile(String fileName) throws IOException {
		// TODO writeObject?
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
		writer.write(hidden.length + "\n");
		for (int i = 0; i < hidden.length; i++) {
			writer.write(this.id(i) + ": ");
			for (int pos = 0; pos < hidden[i].length; pos++)
				writer.write(hiddenStates().charAt(hidden[i][pos]));
			writer.write("\n");
		}
		writer.close();
	}
}
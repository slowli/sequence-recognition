package ua.kiev.icyb.bio;

/**
 * Прецедент — пара из наблюдаемой и скрытой строк состояний. 
 */
public class Sequence {
	
	/**
	 * Последовательность наблюдаемых состояний.
	 */
	public final byte[] observed;
	
	/**
	 * Последовательность скрытых состояний.
	 */
	public final byte[] hidden;
	
	/**
	 * Идентификатор прецедента в выборке. Для задач биоинформатики идентификатор связан с идентификаторами
	 * генов и белков в глобальных базах данных NCBI и CMBI.
	 */
	public final String id;
	
	/**
	 * Номер прецедента в содержащей его выборке.
	 */
	public final int index;
	
	/**
	 * Выборка, содержащая прецедент.
	 */
	public final SequenceSet set;
	
	/**
	 * Создает прецедент с заданными параметрами.
	 * 
	 * @param set
	 * @param index
	 * @param id
	 * @param observed
	 * @param hidden
	 */
	public Sequence(SequenceSet set, int index, String id, byte[] observed, byte[] hidden) {
		this.set = set;
		this.index = index;
		this.id = id;
		this.observed = observed;
		this.hidden = hidden;
	}
	
	/**
	 * Длина строк, состоавляющих прецедент.
	 * 
	 * @return
	 */
	public int length() {
		return observed.length;
	}
}

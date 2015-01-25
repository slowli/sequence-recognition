package ua.kiev.icyb.bio;

import ua.kiev.icyb.bio.res.Messages;

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
	Sequence(SequenceSet set, int index, String id, byte[] observed, byte[] hidden) {
		if ((hidden != null) && (observed.length != hidden.length)) {
			throw new IllegalArgumentException(Messages.getString("dataset.e_length"));
		}
		
		this.set = set;
		this.index = index;
		this.id = id;
		this.observed = observed;
		this.hidden = hidden;
	}
	
	/**
	 * Создает прецедент с заданными параметрами.
	 * 
	 * @param id
	 * @param observed
	 * @param hidden
	 */
	public Sequence(String id, byte[] observed, byte[] hidden) {
		this.set = null;
		this.index = -1;
		this.id = id;
		this.observed = observed;
		this.hidden = hidden;
	}
	
	/**
	 * Длина строк, составляющих прецедент.
	 * 
	 * @return
	 *    длина строк
	 */
	public int length() {
		return observed.length;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj.getClass() != this.getClass()) return false;
		
		Sequence other = (Sequence) obj;
		return other.id.equals(this.id);
	}
	
	@Override
	public String toString() {
		String args = "ID='" + id + "',";
		
		if (set == null) {
			args += "[" + length() + " symbols]";
		} else {
			String oStates = set.observedStates(), hStates = set.hiddenStates(), 
					cStates = set.completeStates();
			StringBuilder builder = (cStates != null)
					? new StringBuilder(length()) : new StringBuilder(2 * length());
			
			for (int i = 0; i < length(); i++) {
				if (cStates != null) {
					builder.append("" + cStates.charAt(hidden[i] * oStates.length() + observed[i]));
				} else { 
					builder.append("" + oStates.charAt(observed[i])); 
					builder.append("" + hStates.charAt(hidden[i]));
				}
			}
			
			args += "'" + builder.toString() + "'";
		}
		
		return "seq(" + args + ")";
	}
}

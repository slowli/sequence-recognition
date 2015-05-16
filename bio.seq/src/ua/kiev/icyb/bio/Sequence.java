package ua.kiev.icyb.bio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ua.kiev.icyb.bio.res.Messages;

/**
 * Прецедент — пара из наблюдаемой и скрытой строк состояний. 
 * 
 * <p>Для ускорения вычислений и уменьшения объема памяти, потребляемой алгоритмами,
 * каждый прецедент обладает идентификатором. Два прецедента равны тогда и только тогда,
 * когда их идентификаторы совпадают (фактическое содержимое прецедентов при этом может различаться).
 */
public class Sequence {
	
	/**
	 * Сегмент последовательности — наибольший возможный отрезок строки скрытых состояний,
	 * включающий состояния одного типа.
	 */
	public class Segment {
		
		/**
		 * Позиция первого состояния сегмента (с отсчетом от нуля).
		 */
		public final int start;
		
		/**
		 * Позиция последнего состояния сегмента (с отсчетом от нуля).
		 */
		public final int end;
		
		/**
		 * Скрытое состояние, соответствующее сегменту.
		 */
		public final byte state;
		
		/**
		 * Возвражает длину сегмента.
		 * 
		 * @return
		 *    длина сегмента
		 */
		public int length() {
			return this.end - this.start + 1;
		}
		
		private Segment(byte state, int start, int end) {
			this.start = start;
			this.end = end;
			this.state = state;
		}
		
		/**
		 * Возвращает скрытое состояние для этого сегмента.
		 * 
		 * @return
		 *    символьное представление скрытого состояния или {@code '\0'}, 
		 *    если символьное представление не определено  
		 */
		public char stateChar() {
			return (Sequence.this.set == null) ? '\0' : Sequence.this.set.hiddenStates().charAt(this.state);
		}
		
		public String toString() {
			return String.format("segment(%d:%d-%d)", this.state, this.start, this.end);
		}
	}
	
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
	 *   идентификатор прецедента
	 * @param observed
	 *    строка наблюдаемых состояний
	 * @param hidden
	 *    строка скрытых состояний
	 */
	public Sequence(String id, byte[] observed, byte[] hidden) {
		this.set = null;
		this.index = -1;
		this.id = id;
		this.observed = observed;
		this.hidden = hidden;
	}
	
	/**
	 * Создает прецедент с заданными строками наблюдаемых и скрытых состояний.
	 * Идентификатор прецедента генерируется автоматически случайным образом.
	 * 
	 * @param observed
	 *    строка наблюдаемых состояний
	 * @param hidden
	 *    строка скрытых состояний
	 *    
	 * @see SequenceUtils#newID()
	 */
	public Sequence(byte[] observed, byte[] hidden) {
		this(SequenceUtils.newID(), observed, hidden);
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
	
	private transient List<Segment> segments;
	
	/**
	 * Вовращает последовательность сегментов, из которых состоит этот прецедент.
	 * 
	 * @return
	 *    список сегментов в порядке их появления в строке скрытых состояний
	 */
	public List<Segment> segments() {
		if ((this.segments == null) && (this.hidden != null)) {
			this.segments = new ArrayList<Segment>();
			
			int start = 0;
			for (int pos = 1; pos <= this.length(); pos++) {
				if ((pos == this.length()) || (hidden[pos] != hidden[pos - 1])) {
					this.segments.add(new Segment(hidden[pos - 1], start, pos - 1));
					start = pos;
				}
			}
		}
		
		return this.segments;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj.getClass() != this.getClass()) return false;
		
		Sequence other = (Sequence) obj;
		if ((this.id != null) && (other.id != null)) {
			return other.id.equals(this.id);
		} else {
			return Arrays.equals(this.observed, other.observed) && Arrays.equals(this.hidden, other.hidden); 
		}
	}
	
	@Override
	public int hashCode() {
		if (this.id != null) return this.id.hashCode();
		
		int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.hidden);
		result = prime * result + Arrays.hashCode(this.observed);
		return result;
	}
	
	@Override
	public String toString() {
		String args = (id == null) ? "" : "ID='" + id + "',";
		
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

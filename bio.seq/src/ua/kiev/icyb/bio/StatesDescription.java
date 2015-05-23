package ua.kiev.icyb.bio;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация состояний, из которых состоят строки определенной выборки.
 * Конфигурации являются неизменяемыми объектами.
 */
public class StatesDescription implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/** Символы для разделения частей конфигурации. */
	private static final String DELIM = "\n";
	
	/**
	 * Кэш конфигураций.
	 */
	private static final Map<String, StatesDescription> cache = new HashMap<String, StatesDescription>();
	
	/**
	 * Создает конфигурацию на основе наблюдаемых, скрытых и полных состояний.
	 * 
	 * @param observed
	 *    символы, обозначающие наблюдаемые состояния
	 * @param hidden
	 *    символы, обозначающие скрытые состояния
	 * @param complete
	 *    символы, обозначающие полные состояния (может быть {@code null})
	 *    
	 * @throws IllegalArgumentException
	 *    если полные состояния имеют явные обозначения ({@code complete != null}) и число обозначений
	 *    не совпадает с числом полных состояний
	 */
	public static StatesDescription create(String observed, String hidden, String complete) {
		String key = observed + DELIM + hidden;
		if (complete != null) key += DELIM + complete; 
		
		StatesDescription states = cache.get(key);
		if (states == null) {
			states = new StatesDescription(observed, hidden, complete);
			cache.put(key, states);
		}
		return states;
	}
	
	/**
	 * Создает конфигурацию на основе наблюдаемых и скрытых состояний.
	 * Полные состояния не имеют обозначений. 
	 * 
	 * @param observed
	 *    символы, обозначающие наблюдаемые состояния
	 * @param hidden
	 *    символы, обозначающие скрытые состояния
	 */
	public static StatesDescription create(String observed, String hidden) {
		return create(observed, hidden, null);
	}
	
	private final String observedStates;
	private final String hiddenStates;
	private final String completeStates;

	/**
	 * Создает конфигурацию на основе наблюдаемых, скрытых и полных состояний.
	 * 
	 * @param observed
	 *    символы, обозначающие наблюдаемые состояния
	 * @param hidden
	 *    символы, обозначающие скрытые состояния
	 * @param complete
	 *    символы, обозначающие полные состояния (может быть {@code null})
	 *    
	 * @throws IllegalArgumentException
	 *    если полные состояния имеют явные обозначения ({@code complete != null}) и число обозначений
	 *    не совпадает с числом полных состояний
	 */
	private StatesDescription(String observed, String hidden, String complete) {
		this.observedStates = observed;
		this.hiddenStates = hidden;
		
		if ((complete != null) && (complete.length() != this.nComplete())) {
			throw new IllegalArgumentException("Invalid number of complete states");
		}
		this.completeStates = complete;
	}
	
	/**
	 * Возвращает алфавит наблюдаемых состояний.
	 * 
	 * @return
	 */
	public String observed() {
		return this.observedStates;
	}
	
	/**
	 * Возвращает символ, соответствующий определенному наблюдаемому состоянию.
	 * 
	 * @param index
	 *    индекс состояния (с отсчетом от нуля)
	 * @return
	 */
	public char observed(int index) {
		return this.observedStates.charAt(index);
	}
	
	/**
	 * Возвращает количество наблюдаемых состояний.
	 * 
	 * @return
	 */
	public int nObserved() {
		return this.observedStates.length();
	}
	
	/**
	 * Возвращает алфавит скрытых состояний.
	 * 
	 * @return
	 */
	public String hidden() {
		return this.hiddenStates;
	}
	
	/**
	 * Возвращает символ, соответствующий определенному скрытому состоянию.
	 * 
	 * @param index
	 *    индекс состояния (с отсчетом от нуля)
	 * @return
	 */
	public char hidden(int index) {
		return this.hiddenStates.charAt(index);
	}
	
	/**
	 * Возвращает количество скрытых состояний.
	 * 
	 * @return
	 */
	public int nHidden() {
		return this.hiddenStates.length();
	}
	
	/**
	 * Возвращает алфавит полных состояний.
	 * 
	 * @return
	 *    алфавит или {@code null}, если для полных состояний нет обозначений
	 */
	public String complete() {
		return this.completeStates;
	}
	
	/**
	 * Возвращает символ, соответствующий определенному полному состоянию.
	 * Если для полных состояний нет обозначений, возвращается символ {@code '\0'}.
	 * 
	 * @param index
	 *    индекс состояния (с отсчетом от нуля)
	 * @return
	 */
	public char complete(int index) {
		if (this.completeStates == null) return '\0';
		return this.completeStates.charAt(index);
	}
	
	/**
	 * Возвращает количество полных состояний.
	 * 
	 * @return
	 */
	public int nComplete() {
		return this.nObserved() * this.nHidden();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((completeStates == null) ? 0 : completeStates.hashCode());
		result = prime * result + hiddenStates.hashCode();
		result = prime * result + observedStates.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		StatesDescription other = (StatesDescription) obj;
		if (completeStates == null) {
			if (other.completeStates != null) return false;
		} else if (!completeStates.equals(other.completeStates)) {
			return false;
		}
		if (!hiddenStates.equals(other.hiddenStates))
			return false;
		if (!observedStates.equals(other.observedStates))
			return false;
		return true;
	}
}
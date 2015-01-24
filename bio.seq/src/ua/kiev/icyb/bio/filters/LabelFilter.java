package ua.kiev.icyb.bio.filters;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;

/**
 * Выбирает из выборки строки с заданными метками.
 */
public class LabelFilter implements SequenceSet.Filter {
	
	/**
	 * Используемая таблица меток.
	 */
	private final Map<String, Integer> labels = new HashMap<String, Integer>();
	
	/**
	 * Выбриаемые метки.
	 */
	private final Set<Integer> indices = new HashSet<Integer>();
	
	/**
	 * Создает новый фильтр.
	 * 
	 * @param labels
	 *    данные о метках - хэш-таблица, содержащая соответствия между идентификаторами строк
	 *    из выборки и метками
	 * @param index
	 *    метка, строки с которой нужно выбрать
	 */
	public LabelFilter(Map<String, Integer> labels, int index) {
		this(labels, Collections.singleton(index));
	}
	
	/**
	 * Создает новый фильтр.
	 * 
	 * @param labels
	 *    данные о метках - хэш-таблица, содержащая соответствия между идентификаторами строк
	 *    из выборки и метками
	 * @param index
	 *    метки, строки с которыми нужно выбрать
	 */
	public LabelFilter(Map<String, Integer> labels, Collection<Integer> indices) {
		this.labels.putAll(labels);
		this.indices.addAll(indices);
	}

	@Override
	public boolean eval(Sequence sequence) {
		Integer label = this.labels.get(sequence.id);
		if (label == null) {
			throw new RuntimeException("Unknown sequence id: " + sequence.id);
		}
		
		return indices.contains(label);
	}
}

package ua.kiev.icyb.bio.alg;


import java.io.Serializable;
import java.util.Collection;

import ua.kiev.icyb.bio.Trainable;

/**
 * Параметрическое вероятностное распределение на некотором множестве объектов.
 * 
 * <p>Для работы с рапределением следует вначале <em>обучить</em> его
 * путем вызовов метода {@link #train(Object, double)} для определенной выборки.
 * После этого метод {@link #estimate(Object)} возвращает значение логарифмического правдоподобия
 * в заданной точке с использованием параметров распределения, которые максимизируют
 * совместное правдоподобие для прецедентов из обучающей выборки <code>D</code>.
 * 
 * @param <T>
 *    класс объектов, на котором задано это распределение
 */
public interface Distribution<T> extends Trainable<T>, Serializable {
	
	/**
	 * Добавляет объект к пулу прецедентов с определенным весом.
	 * 
	 * @param sample
	 *    объект
	 * @param weight
	 *    неотрицательный вес прецедента
	 */
	void train(T sample, double weight);
	
	/**
	 * Добавляет в прецеденты множество объектов с заданными весами элементов.
	 * 
	 * @param samples
	 *    набор объектов
	 * @param weights
	 *    веса объектов в том порядке, в котором они возвращаются итератором набора
	 */
	void train(Collection<? extends T> samples, double[] weights);
	
	@Override
	Distribution<T> clone();
	
	@Override
	Distribution<T> clearClone();
	
	/**
	 * Вычисляет функцию логарифмического правдоподобия этого распределения в заданной точке.
	 * 
	 * @param point
	 *    объект, для которого вычисляется правдоподобие
	 * @return
	 *    логарифимическое правдоподобие в заданной точке
	 */
	double estimate(T point);
	
	/**
	 * Вычисляет совместное логарифмическое правдоподобие для коллекции объектов.
	 * 
	 * @param points
	 *    набор объектов
	 * @return
	 *    логарифм совместного правдоподобия для объектов
	 */
	double estimate(Collection<? extends T> points);
	
	/**
	 * Генерирует случайный объект в соответствии с текущим распределением.
	 * 
	 * @return
	 *    сгенерированный объект
	 * 
	 * @throws UnsupportedOperationException
	 *    если создание объектов не поддерживается
	 */
	T generate();
}

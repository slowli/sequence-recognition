package ua.kiev.icyb.bio.alg.mixture;

import java.util.Arrays;

import ua.kiev.icyb.bio.alg.AbstractDistribution;
import ua.kiev.icyb.bio.alg.Distribution;

/**
 * Линейная смесь вероятностных распределений. Как и составляющие, смесь сама по себе
 * является распределением. 
 * 
 * @param <T>
 *    пространство объектов, на котором задана смесь.
 */
public class Mixture<T> extends AbstractDistribution<T> {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Веса компонент смеси.
	 */
	private double[] weights;
	
	/**
	 * Модели, входящие в смесь.
	 */
	private Distribution<T>[] models;
	
	/**
	 * Создает пустую смесь.
	 */
	public Mixture() {
		this.weights = new double[0];
		
		@SuppressWarnings("unchecked")
		Distribution<T>[] v = new Distribution[0];
		this.models = v;
	}
	
	/**
	 * Создает смесь на основе заданных распределений. Веса всех распределений
	 * равны между собой.
	 * 
	 * @param models
	 *    массив распределений, входящих в смесь
	 */
	public Mixture(Distribution<T>[] models) {
		this.weights = new double[models.length];
		for (int i = 0; i < models.length; i++) {
			this.weights[i] = 1.0 / models.length;
		}
		
		this.models = models.clone();
	}
	
	/**
	 * Возвращает количество распределений, входящих в смесь. 
	 * 
	 * @return
	 *    число распределений в смеси
	 */
	public int size() {
		return this.weights.length;
	}
	
	/**
	 * Возвращает вес определенной компоненты смеси.
	 * 
	 * @param index
	 *    индекс компоненты (с отсчетом от нуля)
	 * @return
	 *    вес компоненты - неотрицательное число, не превышающее единицу
	 */
	public double weight(int index) {
		return this.weights[index];
	}
	
	/**
	 * Возвращает вектор весов компонент смеси.
	 * 
	 * @return
	 *    веса компонент
	 */
	public double[] weights() {
		return this.weights.clone();
	}
	
	/**
	 * Устанавливает веса компонент смеси. Веса должны быть неотрицательными. Нормализация
	 * весов производится автоматически.
	 * 
	 * @param weights
	 *    массив с весами компонент
	 * @throws IllegalArgumentException
	 *    в следующих случаях:
	 *    <ul>
	 *    <li>вектор весов имеет неправильный размер;
	 *    <li>хотя бы один из весов отрицателен;
	 *    <li>все веса равны нулю.
	 *    </ul>
	 */
	public void setWeights(double[] weights) {
		if (weights.length != size()) {
			throw new IllegalArgumentException("Wrong number of weights");
		}
		
		double sum = 0.0;
		for (int i = 0; i < weights.length; i++) {
			if (weights[i] < 0) {
				throw new IllegalArgumentException("Negative weight: " + weights[i]);
			}
			sum += weights[i];
		}
		
		if (sum == 0.0) {
			throw new IllegalArgumentException("At least one weight must be positive");
		}
		
		for (int i = 0; i < weights.length; i++) {
			this.weights[i] = weights[i] / sum;
		}
	}
	
	/**
	 * Возвращает компоненту смеси распределений.
	 * 
	 * @param index
	 *    порядковый номер компоненты (с отсчетом от нуля)
	 * @return
	 *    вероятностное распределение, являющееся компонентой смеси
	 */
	public Distribution<T> model(int index) {
		return this.models[index];
	}
	
	/**
	 * Удаляет одну из компонент из смеси. Веса остальных
	 * моделей пропорционально увеличиваются так, чтобы в сумме они по-прежнему
	 * составляли единицу.
	 * 
	 * @param index
	 *    индекс (с отсчетом от нуля) компоненты, которую надо удалить
	 */
	public void delete(int index) {
		for (int i = index + 1; i < size(); i++) {
			models[i - 1] = models[i];
			weights[i - 1] = weights[i];
		}
		models = Arrays.copyOf(models, models.length - 1);
		weights = Arrays.copyOf(weights, weights.length - 1);
		
		double sum = 0;
		for (int i = 0; i < size(); i++)
			sum += weights[i];
		for (int i = 0; i < size(); i++)
			weights[i] /= sum;
	}
	
	/**
	 * Добавляет новую модель в смесь. Веса остальных марковских цепей в композиции
	 * пропорционально уменьшаются так, чтобы в сумме все веса по-прежнему
	 * составляли единицу.
	 * 
	 * @param model
	 *    модель, которая добавляется в смесь
	 * @param weight
	 *    вес новой модели
	 *    
	 * @throws IllegalArgumentException
	 *    если заданный вес отрицателен или больше единицы
	 */
	public void add(Distribution<T> model, double weight) {
		if (weight < 0.0) {
			throw new IllegalArgumentException("Negative weight: " + weight);
		}
		if (weight > 1.0) {
			throw new IllegalArgumentException("Weight exceeds 1.0: " + weight);
		}
		
		int oldSize = size();
		models = Arrays.copyOf(models, oldSize + 1);
		models[oldSize] = model;
		
		weights = Arrays.copyOf(weights, oldSize + 1);
		for (int i = 0; i < size() - 1; i++) {
			weights[i] *= (1 - weight);
		}
		weights[size() - 1] = weight;
	}
	
	/**
	 * Вычисляет апостериорные вероятности компонент смеси для заданного объекта.
	 * 
	 * @param sample
	 *    объект, для которого ищутся вероятности
	 * @return
	 *    вектор апостериорных вероятностей компонент. Сумма элементов вектора равна единице.
	 */
	public double[] posteriors(T sample) {
		double[] p = new double[this.size()];
		double maxP = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < p.length; i++) {
			p[i] = models[i].estimate(sample) + Math.log(this.weights[i]);
			if (p[i] > maxP) maxP = p[i];
		}
		
		double sum = 0.0;
		for (int i = 0; i < p.length; i++) {
			p[i] -= maxP;
			p[i] = Math.exp(p[i]);
			sum += p[i];
		}
		
		for (int i = 0; i < p.length; i++) {
			p[i] /= sum;
		}
		
		return p;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>Операция не реализована; для обучения параметров смеси распределений следует
	 * использовать EM-алгоритм.
	 */
	@Override
	public void train(T sample, double weight) {
		throw new UnsupportedOperationException("Use EM algorithm");
	}
	
	@Override
	public double estimate(T point) {
		double[] p = new double[this.size()];
		double maxP = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < p.length; i++) {
			p[i] = models[i].estimate(point) + Math.log(this.weights[i]);
			if (p[i] > maxP) maxP = p[i];
		}
		
		double sum = 0.0;
		for (int i = 0; i < size(); i++) {
			sum += Math.exp(p[i] - maxP);
		}
		return maxP + Math.log(sum);
	}
	
	@Override
	public Mixture<T> clearClone() {
		Mixture<T> other = (Mixture<T>) super.clone();
		
		other.weights = this.weights.clone();
		other.models = this.models.clone();
		for (int i = 0; i < this.size(); i++) {
			other.models[i] = this.models[i].clearClone();
		}
		return other;
	}
	
	@Override
	public Mixture<T> clone() {
		Mixture<T> other = (Mixture<T>) super.clone();
		
		other.weights = this.weights.clone();
		other.models = this.models.clone();
		for (int i = 0; i < this.size(); i++) {
			other.models[i] = this.models[i].clone();
		}
		
		return other;
	}

	@Override
	public void reset() {
		for (Distribution<T> model : this.models) {
			model.reset();
		}
	}
}

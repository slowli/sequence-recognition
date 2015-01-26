package ua.kiev.icyb.bio.alg;

import java.util.Collection;

/**
 * Реализация параметрического вероятностного распределения.
 *
 * @param <T>
 *    класс объектов, на котором задано это распределение
 */
public abstract class AbstractDistribution<T> implements Distribution<T>, Cloneable {

	private static final long serialVersionUID = 1L;
	

	@Override
	public void train(T sample) {
		this.train(sample, 1.0);	
	}

	@Override
	public abstract void train(T sample, double weight);

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Реализация по умолчанию обучает распределение на всех объектах, перечисляемых итератором коллекции,
	 * при помощи метода {@link #train(Object)}.
	 */
	@Override
	public void train(Collection<? extends T> samples) {
		for (T sample : samples) {
			this.train(sample);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>Реализация по умолчанию обучает распределение на всех объектах, перечисляемых итератором коллекции,
	 * при помощи метода {@link #train(Object, double)}.
	 */
	@Override
	public void train(Collection<? extends T> samples, double[] weights) {
		if (samples.size() != weights.length) {
			throw new IllegalArgumentException("Dimensions don't agree");
		}
		
		int i = 0;
		for (T sample : samples) {
			this.train(sample, weights[i++]);
		}
	}

	@Override
	public abstract void reset();

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Реализация по умолчанию возвращает объект, клонированный с помощью стандартного механизма Java.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Distribution<T> clone() {
		try {
			return (Distribution<T>) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>Реализация по умолчанию возвращает объект, клонированный с помощью метода {@link #clone()},
	 * и затем сброшенный с помощью метода {@link #reset()}.
	 */
	@Override
	public Distribution<T> clearClone() {
		Distribution<T> clone;
		clone = (Distribution<T>) this.clone();
		clone.reset();
		return clone;
	}

	@Override
	public abstract double estimate(T point);
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>Реализация по умолчанию подсчитывает логарифм правдоподобия на выборке как сумму лог. правдоподобия
	 * на отдельных объектах. 
	 */
	@Override
	public double estimate(Collection<? extends T> points) {
		double logP = 0.0;
		for (T point : points) {
			logP += this.estimate(point);
		}
		
		return logP;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Реализация по умолчанию всегда возбуждает исключительную ситуацию {@link UnsupportedOperationException}.
	 */
	@Override
	public T generate() {
		throw new UnsupportedOperationException();
	}
}

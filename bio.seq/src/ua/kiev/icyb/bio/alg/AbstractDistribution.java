package ua.kiev.icyb.bio.alg;

import java.util.Collection;

public abstract class AbstractDistribution<T> implements Distribution<T>, Cloneable {

	private static final long serialVersionUID = 1L;
	

	@Override
	public void train(T sample) {
		this.train(sample, 1.0);	
	}

	@Override
	public abstract void train(T sample, double weight);

	@Override
	public void train(Collection<? extends T> samples) {
		for (T sample : samples) {
			this.train(sample);
		}
	}
	
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

	@SuppressWarnings("unchecked")
	@Override
	public Distribution<T> clone() {
		try {
			return (Distribution<T>) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	@Override
	public Distribution<T> clearClone() {
		Distribution<T> clone;
		clone = (Distribution<T>) this.clone();
		clone.reset();
		return clone;
	}

	@Override
	public abstract double estimate(T point);
	
	public double estimate(Collection<? extends T> points) {
		double logP = 0.0;
		for (T point : points) {
			logP += this.estimate(point);
		}
		
		return logP;
	}

	@Override
	public T generate() {
		throw new UnsupportedOperationException();
	}
}

package ua.kiev.icyb.bio.filters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.mixture.MarkovMixture;

/**
 * Фильтр, принимающий решение на основе смеси марковских моделей.
 * 
 * Фильтр выделяет из множества те строки, для которых апостериорная вероятность для
 * заданных компонент смеси максимальна и (опционально) превосходит заданный порог.
 */
public class MixtureFilter implements SequenceSet.Filter {
	
	/** Смесь марковских моделей, используемая фильтром. */
	private MarkovMixture mixture;
	
	/** Индексы компонент смеси, которые выделяются фильтром. */
	private final Set<Integer> indices = new HashSet<Integer>();
	
	private double confidence;
	
	
	public MixtureFilter(MarkovMixture mixture, Set<Integer> indices, double confidence) {
		init(mixture, indices, confidence);
	}
	
	public MixtureFilter(MarkovMixture mixture, int index, double confidence) {
		init(mixture, Collections.singleton(index), confidence);
	}
	
	private void init(MarkovMixture mixture, Set<Integer> indices, double confidence) {
		for (int index : indices) {
			if ((index < 0) || (index >= mixture.size())) {
				throw new IllegalArgumentException();
			}
		}
		
		this.mixture = mixture;
		this.confidence = confidence;
		this.indices.addAll(indices);
	}
	
	@Override
	public boolean eval(Sequence sequence) {
		double[] aposterioriP = new double[this.mixture.size()];
		double maxP = Double.NEGATIVE_INFINITY;
		int maxIndex = -1;
		
		for (int k = 0; k < this.mixture.size(); k++) {
			aposterioriP[k] = this.mixture.model(k).estimate(sequence)
					+ Math.log(this.mixture.weight(k));
			if (aposterioriP[k] > maxP) {
				maxP = aposterioriP[k];
				maxIndex = k;
			}
		}
		
		double sum = 0;
		for (int k = 0; k < this.mixture.size(); k++) {
			aposterioriP[k] = Math.exp(aposterioriP[k] - maxP);
			sum += aposterioriP[k]; 
		}
		for (int k = 0; k < this.mixture.size(); k++) {
			aposterioriP[k] /= sum;
		}
		
		return this.indices.contains(maxIndex) 
				&& (aposterioriP[maxIndex] >= this.confidence);
	}
}

package ua.kiev.icyb.bio.alg.tree;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.alg.Organism;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Оболочка для множества цепочек состояний, позволяющее использовать его
 * в генетическом алгоритме оптимизации. 
 */
public class FragmentSetWrapper extends FragmentSet implements Organism {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Генерирует случайный набор цепочек состояний.
	 * 
	 * @param entropy
	 *    объект, использующийся для вычисления функционала качества цепочки
	 * @param seqLength
	 *    длина цепочек в наборе
	 * @return
	 *    случайное множество цепочек состояний
	 */
	public static FragmentSetWrapper random(RuleEntropy entropy, int seqLength) {
		int nSequences = 1;
		final int alphabetLength = entropy.getSet().observedStates().length();
		for (int i = 0; i < seqLength; i++)
			nSequences *= alphabetLength;
		long hash = (long)Math.floor(Math.random() * (1 << nSequences));
		return new FragmentSetWrapper(entropy, seqLength, hash);
	}
	
	/**
	 * Объект, использующийся для вычисления функционала качества цепочки.
	 */
	private final RuleEntropy entropy;
	
	/** Number of possible sequences that the set may contain. */
	private transient int nSequences = 0;
	
	public FragmentSetWrapper(FragmentSetWrapper other) {
		super(other);
		this.entropy = other.entropy;
		this.nSequences = other.nSequences;
	}

	private FragmentSetWrapper(RuleEntropy entropy, int seqLength) {
		super(entropy.getSet().observedStates(), seqLength);
		this.entropy = entropy;
	}
	
	/**
	 * Возвращает максимальный размер множества, составленного из цепочек
	 * той же длины, что и элементы этого множества. 
	 * 
	 * @return
	 *    максимальный размер множества
	 */
	private int nSequences() {
		if (this.nSequences == 0) {
			this.nSequences = 1;
			for (int i = 0; i < getFragmentLength(); i++)
				this.nSequences *= getStates().length();
		}
		return this.nSequences;
	}
	
	private FragmentSetWrapper(RuleEntropy entropy, int seqLength, long hash) {
		this(entropy, seqLength);
		while (hash == 0) {
			// We don't want an empty set
			hash = (long)Math.floor(Math.random() * (1 << nSequences())); 
		}
		
		for (int i = 0; i < nSequences(); i++)
			if ((hash & (1 << i)) > 0) {
				this.add(i);
			}
	}

	@Override
	public Organism mutate(double p) {
		long hash = getHash();
		for (int i = 0; i < nSequences(); i++)
			if (Math.random() < p) {
				if ((hash & (1 << i)) > 0)
					hash &= ~(1 << i); // remove the sequence from the set
				else
					hash ^= (1 << i); // add sequence to the set
			}
			
		return new FragmentSetWrapper(entropy, getFragmentLength(), hash);
	}

	@Override
	public Organism crossover(Organism other) {
		FragmentSetWrapper otherWrapper = (FragmentSetWrapper)other;
		long hash = getHash(), otherHash = otherWrapper.getHash();
		
		for (int i = 0; i < nSequences(); i++)
			if (Math.random() < 0.5) {
				hash &= ~(1 << i);
				hash ^= otherHash & (1 << i);
			}
		return new FragmentSetWrapper(entropy, getFragmentLength(), hash);
	}

	@Override
	public double fitness() {
		ContentPartitionRule rule = new ContentPartitionRule(this, 0.0);
		rule.setThreshold(this.median(entropy.getSet()));
		double fitness = entropy.fitness(rule);
		Env.debug(2, Messages.format("misc.fitness", rule, fitness));
		return fitness;
	}

}

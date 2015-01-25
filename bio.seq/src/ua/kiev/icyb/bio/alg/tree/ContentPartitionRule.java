package ua.kiev.icyb.bio.alg.tree;

import ua.kiev.icyb.bio.SequenceSet;

/**
 * Предикат, значение которого зависит от совместной концентрации
 * в строке состояний отдельных наблюдаемых состояний или их коротких последовательностей.
 * Значение предиката истинно на строках, для которых суммарная концентрация заданных
 * цепочек состояний больше некоторого порога. В противном случае значение предиката ложно.
 * 
 * <p><b>Пример.</b>
 * <pre>
 * FragmentSet states = new FragmentSet("ACGT", 1);
 * states.add(0);
 * states.add(2); // states ~ {A, G}
 * PartitionRule rule = new ContentPartitionRule(states, 0.5);
 * // предикат, сравнивающий суммарную концентрацию состояний A и G с 0,5
 * 
 * byte[] str = new byte[] { 0, 1, 2, 3, 0 }; // str ~ ACGTA 
 * assert(rule.test(str)); // концентрация A и G равна 3/5 > 0,5
 * str = new byte[] { 1, 1, 1, 0, 2}; // str ~ CCCAG
 * assert(!rule.test(str)); // концентрация A и G равна 2/5 < 0,5
 * </pre> 
 */
public class ContentPartitionRule implements PartitionRule {
	
	private static final long serialVersionUID = 1L;
	
	/** Пороговое значение концентрации. */
	private double threshold;
	
	/** 
	 * Множество цепочек наблюдаемых состояний, которые используются для вычисления
	 * концентрации. 
	 */
	private final FragmentSet bases;

	/**
	 * Создает новый предикат на основе концентрации.
	 * 
	 * @param bases
	 *        множество цепочек наблюдаемых состояний, которые используются для вычисления
	 *        концентрации
	 * @param threshold
	 *        пороговое значение концентрации
	 */
	public ContentPartitionRule(FragmentSet bases, double threshold) {
		this.threshold = threshold;
		this.bases = new FragmentSet(bases);
	}

	@Override
	public boolean test(byte[] seq) {
		return (bases.calc(seq) > threshold);
	}

	@Override
	public boolean[] test(SequenceSet set) {
		boolean[] selector = new boolean[set.size()];

		for (int i = 0; i < set.size(); i++) {
			selector[i] = test(set.observed(i));
		}

		return selector;
	}

	/**
	 * Устанавливает значение пороговой концентрации.
	 * 
	 * @param threshold
	 *    пороговая концентрация для этого предиката
	 */
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	@Override
	public String toString() {
		return String.format("n(%s) > %.2f%%", bases, threshold * 100);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bases.hashCode();
		long temp;
		temp = Double.doubleToLongBits(threshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		ContentPartitionRule other = (ContentPartitionRule) obj;
		if (!bases.equals(other.bases))
			return false;
		if (Double.doubleToLongBits(threshold) != Double.doubleToLongBits(other.threshold))
			return false;
		return true;
	}
}

package ua.kiev.icyb.bio.alg;


/**
 * Эмпирическое вероятностное распределение с усреднением. Вероятность в конкретной точке
 * рассчитывается на основе (взвешенной) доли прецедентов из обучающей выборки, попадающих
 * в определенную окрестность этой точки.
 */
public class EmpiricalDistribution extends AbstractDistribution<Integer> {

	private static final long serialVersionUID = 1L;
	
	/**
	 * априорная вероятность для величин, превышающих <code>{@link #bins}.length</code>
	 */
	private final double tailP;
	/**
	 * Длина скользящего окна усреднения.
	 */
	private final int window;
	
	/**
	 * Суммарный вес всех прецедентов.
	 */
	private double weightSum = 0;
	/**
	 * Распределение прецедентов. {@code i}-й элемент массива равен сумме весов прецедентов,
	 * имеющих значение {@code i}.
	 */
	private double[] bins;
	
	/**
	 * Создает эмпирическое распределение с заданными параметрами.
	 * 
	 * @param max
	 *    максимальная величина запоминаемых прецедентов. Прецеденты с большей
	 *    величиной игнорируются; функция правдоподобия для таких величин
	 *    вычисляется, исходя из априорных соображений
	 * @param window
	 *    длина скользящего окна усреднения
	 * @param tailP
	 *    априорная вероятность для величин, превышающих <code>max</code>
	 */
	public EmpiricalDistribution(int max, int window, double tailP) {
		this.window = window;
		this.tailP = tailP;
		bins = new double[max + 1];
	}
	
	@Override
	public void train(Integer value, double weight) {
		if (value < bins.length) {
			bins[value] += weight;
			weightSum += weight;
		}
	}
	
	@Override
	public void reset() {
		weightSum = 0;
		for (int i = 0; i < bins.length; i++) {
			bins[i] = 0;
		}
	}

	@Override
	public double estimate(Integer value) {
		double mean = 0.0;
		int nSamples = 0;
		for (int i = -window/2; i <= window/2; i++)
			if ((value + i >= 0) && (value + i < bins.length)) {
				mean += bins[value + i];
				nSamples++;
			}
		
		if (nSamples > 0) {
			mean /= nSamples;
			return Math.max(Math.log(mean) - Math.log(weightSum), Math.log(tailP));
		} else {
			return Math.log(tailP);
		}
	}
	
	@Override
	public Integer generate() {
		Integer[] outcomes = new Integer[this.bins.length];
		for (int i = 0; i < outcomes.length; i++) {
			outcomes[i] = i;
		}
		
		double[] probabilities = new double[this.bins.length];
		double sum = 0.0;
		for (int i = 0; i < probabilities.length; i++) {
			probabilities[i] = estimate(i);
			sum += probabilities[i];
		}
		for (int i = 0; i < probabilities.length; i++) {
			probabilities[i] /= sum;
		}
		
		return DistributionUtils.choose(outcomes, probabilities);
	}
	
	@Override
	public EmpiricalDistribution clone() {
		EmpiricalDistribution other = (EmpiricalDistribution) super.clone();
		other.bins = this.bins.clone();
		return other;
	}

	@Override
	public String toString() {
		return String.format("Empirical(w=%d)", window);
	}
}

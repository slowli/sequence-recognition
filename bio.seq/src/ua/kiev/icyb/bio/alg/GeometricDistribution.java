package ua.kiev.icyb.bio.alg;


/**
 * Геометрическое вероятностное распределение (<a href="http://ru.wikipedia.org/wiki/Геометрическое_распределение">википедия</a>).
 * Значение функции правдоподобия для натурального числа <code>x</code> равно
 * <pre>
 * p * (1 - p)^(x - 1),
 * </pre>
 * где <code>p</code> — параметр распределения (вероятность успеха при одиночном испытании). 
 */
public class GeometricDistribution extends AbstractDistribution<Integer> {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Взвешенная сумма всех прецедентов.
	 */
	private double valueSum = 0;
	/**
	 * Суммарный вес всех прецедентов.
	 */
	private double weightSum = 0;
	
	/**
	 * Возвращает вычисленное значение параметра распределения — вероятности успеха <code>p</code>.
	 * 
	 * @return
	 *    вероятность успеха
	 */
	public double successP() {
		return weightSum / valueSum;
	}

	@Override
	public void reset() {
		valueSum = 0;
		weightSum = 0;
	}

	@Override
	public String toString() {
		return String.format("Geom(%s)", successP());
	}

	@Override
	public void train(Integer sample, double weight) {
		valueSum += weight * sample;
		weightSum += weight;		
	}

	@Override
	public double estimate(Integer point) {
		final double p = successP();
		return (point - 1) * Math.log(1 - p) + Math.log(p);
	}
}

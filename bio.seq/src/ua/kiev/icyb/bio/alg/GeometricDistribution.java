package ua.kiev.icyb.bio.alg;


/**
 * Геометрическое вероятностное распределение (<a href="http://ru.wikipedia.org/wiki/Геометрическое_распределение">википедия</a>).
 * Значение функции правдоподобия для натурального числа <code>x</code> равно
 * <pre>
 * p * (1 - p)^(x - 1),
 * </pre>
 * где <code>p</code> — параметр распределения (вероятность успеха при одиночном испытании). 
 */
public class GeometricDistribution implements Distribution {

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
	public void digest(int value, double weight) {
		valueSum += weight * value;
		weightSum += weight;
	}

	@Override
	public void reset() {
		valueSum = 0;
		weightSum = 0;
	}
	
	@Override
	public double estimate(int value) {
		final double p = successP();
		return (value - 1) * Math.log(1 - p) + Math.log(p);
	}

	@Override
	public String toString() {
		return String.format("Geom(%s)", successP());
	}
}

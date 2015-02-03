package ua.kiev.icyb.bio.alg;

/**
 * Инструменты для работы с вероятностными распределениями.
 */
public class DistributionUtils {

	/**
	 * Возвращает один из объектов в соответствии с вероятностным распрделением на конечном
	 * множестве.
	 * 
	 * @param objects
	 *    множество объектов, на котором задано распределение
	 * @param probabilities
	 *    вероятности для всех объектов; сумма элементов массива должна равняться единице
	 * @return
	 *    один из объектов
	 */
	public static <T> T choose(T[] objects, double[] probabilities) {
		if (objects.length != probabilities.length) {
			throw new IllegalArgumentException("Incompatible array sizes");
		}
		
		double[] cummulativeP = new double[objects.length];
		cummulativeP[0] = probabilities[0];
		for (int i = 1; i < objects.length; i++) {
			cummulativeP[i] = cummulativeP[i - 1] + probabilities[i]; 
		}
		
		double r = Math.random();
		int idx;
		for (idx = 0; (idx < objects.length) && (cummulativeP[idx] < r); idx++) ;
		if (idx >= objects.length) idx--;
		
		return objects[idx];
	}
}

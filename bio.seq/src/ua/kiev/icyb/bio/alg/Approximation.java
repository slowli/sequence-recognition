package ua.kiev.icyb.bio.alg;

import java.io.Serializable;

/**
 * Параметры аппроксимации неизвестных начальных и переходных вероятностей
 * в марковских цепях.
 * 
 * @see FallthruChain
 */
public class Approximation implements Serializable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Методы аппроксимации неизвестных вероятностей.
	 */
	public static enum Strategy {
		/**
		 * Неизвестные вероятности вычисляются путем усреднения соответствующих
		 * начальных или переходных вероятностей в марковских цепях меньшего порядка.
		 */
		MEAN,
		/**
		 * Неизвестные вероятности вычисляются как первая ненулевая соответствующая вероятность
		 * в марковских цепях меньшего порядка. 
		 */
		FIRST,
		/**
		 * Неизестные вероятности принимают априорные значения.
		 */
		FIXED
	}
	
	/** Базовый порядок марковской цепи. */
	public int order;
	/** Минимальный используемый порядок цепи. */
	public int minOrder;
	/** Метод аппроксимации неизвестных начальных и переходных вероятностей. */
	public Strategy strategy;
	/**
	 * Априорное значение для неизвестных начальных вероятностей. Применяется исключительно
	 * при методе аппроксимации {@link Strategy#FIXED}.  
	 */
	public double initThreshold = 1e-4;
	/**
	 * Априорное значение для неизвестных переходных вероятностей. Применяется исключительно
	 * при методе аппроксимации {@link Strategy#FIXED}.  
	 */
	public double transThreshold = 1e-2;
	
	/**
	 * Создает новый набор параметров аппроксимации. 
	 * 
	 * @param order
	 *    базовый порядок марковской цепи
	 * @param minOrder
	 *    минимальный используемый порядок цепи
	 * @param strategy
	 *    метод аппроксимации неизвестных начальных и переходных вероятностей
	 */
	public Approximation(int order, int minOrder, Strategy strategy) {
		this.order = order;
		this.minOrder = minOrder;
		this.strategy = strategy;
	}
}

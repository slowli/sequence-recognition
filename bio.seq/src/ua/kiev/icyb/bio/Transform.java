package ua.kiev.icyb.bio;

/**
 * Преобразование выборки.
 */
public interface Transform extends Representable {

	/**
	 * Преобразует описание состояний выборки.
	 * 
	 * @param original
	 *    первоначальные состояния выборки
	 * @return
	 *    преобразованные состояния
	 */
	StatesDescription states(StatesDescription original);
	
	/**
	 * Преобразует отдельный прецедент из выборки.
	 * 
	 * @param original
	 *    последовательность состояний, которую необходимо преобразовать
	 * @return
	 *    преобразованная строка состояний
	 */
	Sequence sequence(Sequence original);
	
	/**
	 * Выполняет обратное преобразование для последовательности из выборки.
	 * 
	 * @param transformed
	 *    преобразованная последовательность, для которой надо найти исходную
	 * @return
	 */
	Sequence inverse(Sequence transformed);
}

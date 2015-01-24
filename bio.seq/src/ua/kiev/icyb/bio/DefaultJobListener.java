package ua.kiev.icyb.bio;

import ua.kiev.icyb.bio.res.Messages;


/**
 * Используемая по умолчанию версия обработчика событий {@link JobListener}.
 * 
 * <p>Выводит точку после обработки фиксированного количества последовательностей. 
 * После вывода определенного числа точек
 * обработчик выводит число обработанных последовательностей и переходит на новую строку.
 * При отказе от классификации печатается вопросительный знак.
 * После окончания работы выводится общее количство последовательностей и время их обработки.
 */
class DefaultJobListener implements JobListener {
	
	private static boolean printedKey = false;
	
	/**
	 * Число обрабатываемых последовательностей между двумя последовательными
	 * выводами точки.
	 */
	private int sequencesPerDot;
	/**
	 * Число точек в ряду.
	 */
	private int dotsPerLine;	
	/**
	 * Текущее количество обработанных последовательностей.
	 */
	private int nProcessed = 0;
	
	/**
	 * Окружение, в котором работает обработчик событий.
	 */
	public Env env;
	
	/**
	 * Создает обработчик событий с параметрами по умолчанию.
	 */
	public DefaultJobListener() {
		this(20, 50);
	}
	/**
	 * Создает обработчик событий с заданной скоростью вывода информации.
	 *  
	 * @param seqPerDot
	 *    число обрабатываемых последовательностей между двумя последовательными
	 *    выводами точки
	 * @param dotsPerLine
	 *    число точек в ряду
	 */	
	public DefaultJobListener(int seqPerDot, int dotsPerLine) {
		this.sequencesPerDot = seqPerDot;
		this.dotsPerLine = dotsPerLine;
	}
	
	@Override
	public synchronized void seqCompleted(int index, byte[] hidden) {
		if (nProcessed == 0) {
			if (!printedKey) {
				env.debug(1, Messages.format("test.key", sequencesPerDot));
				printedKey = true;
			}
		}
		nProcessed++;
		
		if (nProcessed % sequencesPerDot == 0)
			env.debugInline(1, ".");
		if (hidden == null)
			env.debugInline(1, "?");
		
		if (nProcessed % (sequencesPerDot * dotsPerLine) == 0)
			env.debug(1, "" + nProcessed);
	}

	@Override
	public void finished() {
		if (nProcessed % (sequencesPerDot * dotsPerLine) >= sequencesPerDot) {
			env.debug(1, "");
		}
	}
}

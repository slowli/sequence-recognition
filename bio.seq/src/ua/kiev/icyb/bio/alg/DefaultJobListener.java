package ua.kiev.icyb.bio.alg;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.JobListener;
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
public class DefaultJobListener implements JobListener {
	/**
	 * Имя свойства конфигурации, содержащего число обрабатываемых последовательностей,
	 * соответствующих выводу одной точки.
	 */
	public static final String SEQ_PER_DOT_PROPERTY = "joblistener.seqPerDot";
	
	/**
	 * Имя свойства конфигурации, содержащего число выводимых точек, которые приходятся
	 * на одну строку.
	 */
	public static final String DOTS_PER_LINE_PROPERTY = "joblistener.dotsPerLine";
	
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
	 * Создает обработчик событий с параметрами по умолчанию.
	 */
	public DefaultJobListener() {
		this(Env.intProperty(SEQ_PER_DOT_PROPERTY), 
				Env.intProperty(DOTS_PER_LINE_PROPERTY));
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
				Env.debug(1, Messages.format("test.key", sequencesPerDot));
				printedKey = true;
			}
		}
		nProcessed++;
		
		if (nProcessed % sequencesPerDot == 0)
			Env.debugInline(1, ".");
		if (hidden == null)
			Env.debugInline(1, "?");
		
		if (nProcessed % (sequencesPerDot * dotsPerLine) == 0)
			Env.debug(1, "" + nProcessed);
	}

	@Override
	public void finished() {
		if (nProcessed % (sequencesPerDot * dotsPerLine) >= sequencesPerDot) {
			Env.debug(1, "");
		}
	}
}

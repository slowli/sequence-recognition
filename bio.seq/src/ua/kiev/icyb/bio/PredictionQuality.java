package ua.kiev.icyb.bio;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ua.kiev.icyb.bio.res.Messages;

/**
 * Класс, представляющий различные критерии качества распознавания скрытых последовательностей.
 * Большинство критериев взяты из теории обработки сигналов.
 */
public class PredictionQuality implements Serializable, Representable {
	
	private static final long serialVersionUID = 1L;
	
	
	private final String hiddenStates;
	
	private int[] truePos, trueNeg, falsePos, falseNeg;
	private int[] trueRegion, actRegion, prRegion;
	
	private int nSeq = 0;
	private int nDeniedSeq = 0;
	
	private PredictionQuality[] samples = null;
	
	/**
	 * Создает контейнер для метрик качества на основе набора прецедентов и
	 * результата работы алгоритма распознавания.
	 * 
	 * @param reference
	 *    референсный набор прецедентов, для которых известны как наблюдаемые,
	 *    так и скрытые цепочки состояний
	 * @param est
	 *    набор строк состояний, полученных в результате работы алгоритма распознавания
	 */
	public PredictionQuality(SequenceSet reference, SequenceSet est) {
		this(reference);
		assert(reference.size() == est.size());
		calculateStats(reference, est);
	}
	
	/**
	 * Создает контейнер для метрик качества на основе набора прецедентов. 
	 * Последовательности скрытых состояний, получаемые алгоритмом распознавания,
	 * добавляются позже.
	 * 
	 * @param reference
	 *    референсный набор прецедентов, для которых известны как наблюдаемые,
	 *    так и скрытые цепочки состояний
	 */
	public PredictionQuality(SequenceSet reference) {
		this.hiddenStates = reference.hiddenStates();
		
		int nStates = this.hiddenStates.length();
		truePos = new int[nStates];
		trueNeg = new int[nStates];
		falsePos = new int[nStates];
		falseNeg = new int[nStates];
		trueRegion = new int[nStates];
		actRegion = new int[nStates];
		prRegion = new int[nStates];
	}
	
	/**
	 * Создает контейнер для метрик качества, усредняющий показатели качества
	 * для набора других контейнеров. 
	 * 
	 * @param samples
	 *        контейнеры метрик качества, по которым производится усреднение
	 */
	public PredictionQuality(PredictionQuality... samples) {
		this.hiddenStates = samples[0].hiddenStates;
		this.samples = samples.clone();
	}
	
	/**
	 * Вычисляет среднее значение величин, возвращаемых методом, вызвавшим этот метод,
	 * для усредняемых контейнеров метрик качества.
	 * 
	 * @param state 
	 *        индекс (с отсчетом от нуля) скрытого состояния, для которого вычисляется
	 *        метрика
	 * @return
	 *        усредненное значение метрики качества
	 */
	private double mean(int state) {
		// Получить имя вызывающего метода
		Throwable t = new Throwable();
		t.fillInStackTrace();
		String methodName = t.getStackTrace()[1].getMethodName();
		
		try {
			Method method = this.getClass().getDeclaredMethod(methodName, int.class);
			double sum = 0.0;
			int nSamples = 0;
			for (PredictionQuality q : samples)
				if (q.nSeq > 0) {
					sum += (Double)method.invoke(q, state);
					nSamples++;
				}
			if (nSamples > 0)
				sum /= nSamples;
			return sum;
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		} catch (NoSuchMethodException e) {
		}
		
		return 0.0;
	}
	
	private void calculateStats(SequenceSet ref, SequenceSet est) {
		for (int i = 0; i < ref.size(); i++) {
			addSequence(ref.hidden(i), est.hidden(i));
		}
	}
	
	/**
	 * Добавляет результат распознавания наблюдаемой последовательности.
	 * 
	 * @param refSeq
	 *        действительная последовательность скрытых состояний  
	 * @param estSeq
	 *        определенная алгоритмом последовательность скрытых состояний, или {@code null}
	 *        в случае отказа от распознавания
	 */
	public void addSequence(byte[] refSeq, byte[] estSeq) {
		nSeq++;
		if (estSeq == null) {
			// отказ от распознавания
			nDeniedSeq++;
			return;
		}
		if (refSeq.length != estSeq.length) {
			throw new IllegalArgumentException("Incomatible lengths of reference and predicted strings of states");
		}
		
		for (int state = 0; state < hiddenStates.length(); state++) {
			// Вычислить статистику по отдельным состояниям
			for (int pos = 0; pos < refSeq.length; pos++) {
				if ((refSeq[pos] == state) && (estSeq[pos] == state))
					truePos[state]++;
				else if ((refSeq[pos] != state) && (estSeq[pos] != state))
					trueNeg[state]++;
				else if ((refSeq[pos] != state) && (estSeq[pos] == state))
					falsePos[state]++;
				else
					falseNeg[state]++;
			}
			
			// Вычислить статистику по сегментам
			for (int pos = 0; pos < refSeq.length - 1; pos++) {
				if ((refSeq[pos] == state) && (refSeq[pos + 1] != state))
					actRegion[state]++;
				if ((estSeq[pos] == state) && (estSeq[pos + 1] != state))
					prRegion[state]++;
			}
			if (refSeq[refSeq.length - 1] == state) actRegion[state]++;
			if (estSeq[refSeq.length - 1] == state) prRegion[state]++;
			
			int actStart = -1, estStart = -2;
			for (int pos = 0; pos < refSeq.length; pos++) {
				if ((refSeq[pos] == state) && ((pos == 0) || (refSeq[pos - 1] != state)))
					actStart = pos;
				if ((estSeq[pos] == state) && ((pos == 0) || (estSeq[pos - 1] != state)))
					estStart = pos;
				
				if (	// конец сегмента в действительной строке состояний
						(refSeq[pos] == state) && ((pos == refSeq.length - 1) || (refSeq[pos + 1] != state))							
						&& // конец сегмента в предсказанной строке
						(estSeq[pos] == state) && ((pos == refSeq.length - 1) || (estSeq[pos + 1] != state))
						&& // начала сегментов совпадают
						(actStart == estStart))
					
					trueRegion[state]++;
			}
		}
	}
	
	/**
	 * Метрика качества — симольная специфичность для конкретного типа скрытых состояний.
	 * Определяется как доля правильно распознанных алгоритмом состояний этого типа к общему
	 * количеству символов, которые алгоритм относит к этому типу.
	 * 
	 * <p>Значения метрики качества находятся в диапазоне <code>[0, 1]</code>. Метрика,
	 * близкая к единице, свидетельствует о хорошем качестве алгоритма распознавания.
	 * 
	 * @param state
	 *    индекс (с отсчетом от нуля) скрытого состояния в алфавите скрытых состояний
	 * @return
	 *    символьная специфичность по заданному состоянию
	 */
	public double symbolSpec(int state) {
		if (samples != null)
			return mean(state);
		
		return 1.0 * truePos[state] / (truePos[state] + falsePos[state]);
	}
	
	/**
	 * Метрика качества — симольная чувствительность для конкретного типа скрытых состояний.
	 * Определяется как доля правильно распознанных алгоритмом состояний этого типа к общему
	 * количеству символов этого типа в референсной выборке.
	 * 
	 * <p>Значения метрики качества находятся в диапазоне <code>[0, 1]</code>. Метрика,
	 * близкая к единице, свидетельствует о хорошем качестве алгоритма распознавания.
	 * 
	 * @param state
	 *    индекс (с отсчетом от нуля) скрытого состояния в алфавите скрытых состояний
	 * @return
	 *    символьная чувствительность по заданному состоянию
	 */
	public double symbolSens(int state) {
		if (samples != null)
			return mean(state);
		
		return 1.0 * truePos[state] / (truePos[state] + falseNeg[state]);
	}
	
	/**
	 * Метрика качества — средняя условная вероятность (англ. <em>average conditional probability</em>).
	 * Является средним арифметическим для символьной чувствительности и специфичности для
	 * некоторого типа скрытых состояний, а также еще двух метрик — чувствительности и
	 * специфичности для всех остальных скрытых состояний.
	 * 
	 * <p>Значения метрики качества находятся в диапазоне <code>[0, 1]</code>. Метрика,
	 * близкая к единице, свидетельствует о хорошем качестве алгоритма распознавания.
	 * 
	 * @param state
	 *    индекс (с отсчетом от нуля) скрытого состояния в алфавите скрытых состояний
	 * @return
	 *    средняя условная вероятность по заданному состоянию
	 */
	public double symbolACP(int state) {
		if (samples != null)
			return mean(state);
		
		return 0.25 * (symbolSpec(state) + symbolSens(state)
				+ 1.0 * trueNeg[state] / (trueNeg[state] + falseNeg[state])
				+ 1.0 * trueNeg[state] / (trueNeg[state] + falsePos[state]));
	}
	
	/**
	 * Метрика качества — коэффициент корреляции. Равен корреляции между двумя событиями:
	 * (1) появлением в строке скрытых состояний опреленного состояния и (2) отнесением
	 * алгоритмом распознавания скрытого состояния к этому классу.
	 * 
	 * <p>Значения метрики качества находятся в диапазоне <code>[-1, 1]</code>. Метрика,
	 * близкая к единице, свидетельствует о хорошем качестве алгоритма распознавания.
	 * 
	 * @param state
	 *    индекс (с отсчетом от нуля) скрытого состояния в алфавите скрытых состояний
	 * @return
	 *    значение коэффициента корреляции по заданному состоянию
	 */
	public double symbolCC(int state) {
		if (samples != null)
			return mean(state);
		
		double a = 1.0 * truePos[state] * trueNeg[state] - 1.0 * falsePos[state] * falseNeg[state];
		double b = Math.sqrt(1.0 * (truePos[state] + falseNeg[state])
				* (truePos[state] + falsePos[state])
				* (trueNeg[state] + falseNeg[state])
				* (trueNeg[state] + falsePos[state]));
		return a / b;
	}
	
	/**
	 * Метрика качества — фрагментная специфичность. Определяется как доля
	 * корректно распознанных алгоритмом <em>фрагментов</em> определенного типа (участки
	 * строки скрытых состояний, которые состоят из состояний одного типа и которые нельзя
	 * расширить путем добавления соседних состояний) среди всех фрагментов этого типа,
	 * выделенных алгоритмом.
	 * 
	 * <p>Значения метрики качества находятся в диапазоне <code>[0, 1]</code>. Метрика,
	 * близкая к единице, свидетельствует о хорошем качестве алгоритма распознавания.
	 * 
	 * @param state
	 *    индекс (с отсчетом от нуля) скрытого состояния в алфавите скрытых состояний
	 * @return
	 *    значение фрагментной специфичности по заданному состоянию
	 */
	public double regionSpec(int state) {
		if (samples != null)
			return mean(state);
		
		return 1.0 * trueRegion[state] / prRegion[state];
	}

	/**
	 * Метрика качества — фрагментная чувствительность. Определяется как доля
	 * корректно распознанных алгоритмом <em>фрагментов</em> определенного типа (участки
	 * строки скрытых состояний, которые состоят из состояний одного типа и которые нельзя
	 * расширить путем добавления соседних состояний) среди всех фрагментов этого типа
	 * в референсной выборке.
	 * 
	 * <p>Значения метрики качества находятся в диапазоне <code>[0, 1]</code>. Метрика,
	 * близкая к единице, свидетельствует о хорошем качестве алгоритма распознавания.
	 * 
	 * @param state
	 *    индекс (с отсчетом от нуля) скрытого состояния в алфавите скрытых состояний
	 * @return
	 *    значение фрагментной чувствительности по заданному состоянию
	 */
	public double regionSens(int state) {
		if (samples != null)
			return mean(state);
		
		return 1.0 * trueRegion[state] / actRegion[state];
	}
	
	private double symbolPrecision(int dummy) {
		if (samples != null)
			return mean(0);
		
		final int nSymbols = truePos[0] + trueNeg[0] + falsePos[0] + falseNeg[0];
		int correct = 0;
		for (int state = 0; state < hiddenStates.length(); state++)
			correct += truePos[state];
		return 1.0 * correct / nSymbols;
	}
	
	/**
	 * Метрика качества — символьная точность алгоритма распознавания. Равна доле
	 * корректно распознанных алгоритмом скрытых состояний к общей длине всех строк
	 * из референсной выборки.
	 * 
	 * @return
	 *    символьная точность алгоритма распознавания
	 */
	public double symbolPrecision() {
		return symbolPrecision(0);
	}
	
	/**
	 * Возвращает текстовое представление точности распознавания по определенному
	 * классу скрытых состояний.
	 * 
	 * @param state
	 *    индекс (с отсчетом от нуля) скрытого состояния в алфавите скрытых состояний
	 * @return
	 *    текстовое представление основных метрик точности
	 */
	public String repr(int state) {
		String repr = String.format("SSp = %.2f%%, SSn = %.2f%%, ACP = %.2f%%, CC = %.2f%%; RSp = %.2f%%, RSn = %.2f%%",
				symbolSpec(state) * 100, symbolSens(state) * 100,
				symbolACP(state) * 100, symbolCC(state) * 100,
				regionSpec(state) * 100, regionSens(state) * 100);
		
		if (truePos != null) {
			repr += "\n";
			repr += String.format("TP = %d, TN = %d, FP = %d, FN = %d; TR = %d, AR = %d, PR = %d",
					truePos[state], trueNeg[state], falsePos[state], falseNeg[state],
					trueRegion[state], actRegion[state], prRegion[state]);
		}
		
		return repr;
	}
	
	public String repr() {
		String repr = "";
		if (nDeniedSeq > 0) {
			repr = Messages.format("q.not_recognized", nDeniedSeq) + "\n";
		}
		for (int state = 0; state < hiddenStates.length(); state++) {
			repr += Messages.format("q.state", hiddenStates.charAt(state)) + " ";
			repr += this.repr(state) + "\n";
		}
		return repr;
	}
}

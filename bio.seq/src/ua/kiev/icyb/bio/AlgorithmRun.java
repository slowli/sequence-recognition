package ua.kiev.icyb.bio;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ua.kiev.icyb.bio.alg.DefaultJobListener;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Класс, представляющий отдельный запуск алгоритма распознавания
 * скрытых последовательностей для оценки его качества. 
 * В рамках кросс-валидации объекты класса соответствуют запускам алгоритма 
 * на какой-либо обучающей или контрольной выборке. 
 * 
 * <p>Объекты класса хранят сведения о состоянии запуска, в частности, об 
 * обработанных на данный момент последовательностях, а также о качестве
 * распознавания. Это позволяет останавливать и продолжать оценку качества в любой момент.
 */
public class AlgorithmRun implements Serializable, Representable {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Имя свойства конфигурации, содержащего количество обрабатываемых последовательностей 
	 * между сохранениями качества распознавания в файл.
	 */
	public static final String SEQ_PER_SAVE_PROPERTY = "joblistener.seqPerSave";
	
	/** 
	 * Число обрабатываемых последовательностей между сохранениями 
	 * качества распознавания в файл. 
	 */
	private static final int sequencesPerSave = Env.intProperty(SEQ_PER_SAVE_PROPERTY);
	
	/** Количество обработанных алгоритмом распознавания генов. */
	private int nProcessed = 0;
	/** Индикатор необработанных последовательностей. */
	private boolean[] isUnprocessed;
	/** Коллекция запусков алгоритма, которой принадлежит этот объект. */
	private final RunCollection parent;
	
	private final int runIndex;
	
	/** Качество распознавания. */
	private final PredictionQuality quality;
	/** Набор последовательностей, для которого оценивается качество распознавания. */
	private transient SequenceSet set;
	/** Набор последовательностей, которые не обработаны алгоритмом. */
	private transient SequenceSet unprocessed;
	/** Список индексов необработанных строк в множестве {@link #set}. */
	private transient List<Integer> unprocessedIdx;
	/** Обработчик событий, используемый для контроля процесса распознавания. */
	private transient JobListener jobListener;
	
	/**
	 * Создает объект, отвечающий отдельному запуску алгоритма распознавания.
	 * 
	 * @param parent
	 *    коллекция запусков, в которой будет содержаться объект
	 * @param index
	 *    индекс (с отсчетом от нуля) выборки в коллекции, которой соответствует
	 *    этот запуск
	 */
	public AlgorithmRun(RunCollection parent, int index) {
		this.parent = parent;
		this.runIndex = index;
		initialize();
		
		quality = new PredictionQuality(set);
	}
	
	/**
	 * Возвращает текущее качество распознавания для обработанных алгоритмом строк.
	 * 
	 * @return
	 *    качество распознавания
	 */
	public PredictionQuality getQuality() {
		return quality;
	}
	
	/**
	 * Возвращает множество всех последовательностей, которые должны быть
	 * обработаны алгоритмом распознавания в рамках этого запуска.
	 * 
	 * @return
	 *    набор последовательностей, которые следует распознать
	 */
	public SequenceSet getSet() {
		if (set == null) {
			initialize();
		}
		return set;
	}
	
	/**
	 * Возвращает количество обработанных алгоритмом распознавания строк.
	 * 
	 * @return
	 *    число строк, обработанных на текущий момент алгоритмом
	 */
	public int getProcessedCount() {
		return nProcessed;
	}
	
	/**
	 * Запускает алгоритм распознавания на необработанных последовательностях.
	 * 
	 * @param algorithm
	 *    алгоритм распознавания скрытых последовательностей, качество которого
	 *    оценивается в рамках этого запуска
	 */
	public void run(SeqAlgorithm algorithm) {
		algorithm.runSet(getUnprocessed(), jobListener);
	}
	
	/**
	 * Инициализирует большинство полей объекта, например, после
	 * его десериализации.
	 */
	private void initialize() {
		set = parent.getSet(this.runIndex);

		if (isUnprocessed == null) {
			isUnprocessed = new boolean[set.length()];
			Arrays.fill(isUnprocessed, true);
		}
		unprocessedIdx = new ArrayList<Integer>();
		for (int i = 0; i < isUnprocessed.length; i++) {
			if (isUnprocessed[i]) {
				unprocessedIdx.add(i);
			}
		}
		unprocessed = set.filter(isUnprocessed);
		
		jobListener = new DefaultJobListener() {

			@Override
			public synchronized void seqCompleted(int index, byte[] hidden) {
				quality.addSequence(getUnprocessed().hidden(index), hidden);
				isUnprocessed[unprocessedIdx.get(index)] = false;
				super.seqCompleted(index, hidden);
				
				nProcessed++;
				if (nProcessed % sequencesPerSave == 0) {
					Env.debugInline(2, "S");
					// Не выводить стандартное сообщение о сохранении
					int level = Env.debugLevel();
					Env.setDebugLevel(0); 
					parent.save();
					Env.setDebugLevel(level);
				}
			}

			@Override
			public void finished() {
				super.finished();
				parent.save();
			}
		};
	}
	
	/**
	 * Возвращает набор необработанных на данный момент последовательностей.
	 * 
	 * @return
	 *    необработанные алгоритмом распознавания последовательности
	 */
	private SequenceSet getUnprocessed() {
		if (set == null) {
			initialize();
		}
		return unprocessed;
	}
	
	@Override
	public String repr() {
		String repr = Messages.format("test.repr", getSet().length(), nProcessed);
		if (nProcessed > 0) {
			repr += "\n" + quality.repr();
		}
		return repr;
	}
}

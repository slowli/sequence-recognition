package ua.kiev.icyb.bio;

import java.io.IOException;

import ua.kiev.icyb.bio.alg.ThreadedAlgorithm;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Класс, представляющий коллекцию обучающих и контрольных выборок
 * в процессе кросс-валидации.
 * 
 * <p><code>n</code>-кратная кросс-валидация алгоритма распознавания производится следующим образом:
 * <ol>
 * <li>Выборка прецедентов, для которой известны как наблюдаемые, так и скрытые цепочки
 * состояний, разбивается случайным образом на <code>n</code> непересекающихся частей. 
 * Вхождение любого прецедента во все части равновероятно.
 * <li>Алгоритм распознавания обучается на <em>обучающей выборке</em>, состоящей из 
 * всех частей разбиения, кроме одной. Оставшаяся часть разбиения образует <em>контрольную выборку</em>;
 * на ней производится оценка качества обученного алгоритма. Этот шаг повторяется
 * по очереди для всех частей разбиения.
 * <li>(необязательно) Показатели качества алгоритма также могут измеряться на той же выборке,
 * на которой он был обучен, например, для оценки степени <em>переобучения</em> (чрезмерной
 * специфичности модели, используемой в алгоритме). 
 * </ol>
 */
public class CrossValidation extends AbstractLaunchable implements RunCollection, Representable {
	
	private static final long serialVersionUID = 1L;
	
	/** 
	 * Следует ли пропускать оценку качества алгоритма распознавания на обучающих выборках. 
	 */ 
	public boolean skipTraining = true;
	
	/** 
	 * Индекс (с отсчетом от нуля) части выборки, на которые она разбивается для
	 * кросс-валидации, для каждого из ее элементов. 
	 */
	private final byte[] foldIndex;
	
	/**
	 * Полная выборка прецедентов, используемая при кросс-валидации.
	 */
	private final SequenceSet set;
	/**
	 * Массив объектов, соответствующих отдельным запускам алгоритма распознавания.
	 */
	private final AlgorithmRun[] runs;
	
	/**
	 * Текущее усредненное качество распознавания на обучающих выборках.
	 */
	private PredictionQuality meanTraining;
	/**
	 * Текущее усредненное качество распознавания на контрольных выборках.
	 */
	private PredictionQuality meanControl;
	
	/**
	 * Оцениваемый алгоритм распознавания.
	 */
	private transient SeqAlgorithm algorithm;
	
	/**
	 * Количество распознанных строк между двумя последовательными сохранениями состояния алгоритма.
	 */
	public int sequencesPerSave = 100;
	
	@Override
	public int getSequencesPerSave() {
		return sequencesPerSave;
	}
	
	/**
	 * Создает коллекцию выборок, соответствующий кросс-валидации с заданной
	 * кратностью.
	 * 
	 * @param set
	 *    полная выборка прецедентов, используемая при кросс-валидации 
	 * @param nFolds
	 *    кратность кросс-валидации
	 */
	public CrossValidation(SequenceSet set, int nFolds) {
		this.set = set;
		foldIndex = new byte[set.length()];
		runs = new AlgorithmRun[2 * nFolds];
		
		for (int i = 0; i < set.length(); i++) {
			foldIndex[i] = (byte) Math.floor(Math.random() * nFolds);
		}
		
		// Создать запуски
		for (int f = 0; f < runs.length; f++) {
			runs[f] = new AlgorithmRun(this, f);
		}
		
		// Создать объекты для усредненных значений функционалов качества
		PredictionQuality[] samples = new PredictionQuality[nFolds];
		for (int f = 0; f < nFolds * 2; f += 2) {
			samples[f / 2] = runs[f].getQuality();
		}
		meanTraining = new PredictionQuality(samples);
		for (int f = 1; f < nFolds * 2; f += 2) {
			samples[f / 2] = runs[f].getQuality();
		}
		meanControl = new PredictionQuality(samples);
	}
	
	/**
	 * Возвращает обучающую или контрольную выборку, соответствующую определенному
	 * запуску алгоритма распознавания. <code>i</code>-ю обучающую выборку 
	 * (индекс <code>i</code> считается, начиная с нуля) можно получить с помощью команды
	 * {@code getSet(2*i)}; <code>i</code>-ю контрольную выборку — с помощью команды
	 * {@code getSet(2*i + 1)}.
	 * 
	 * @param index
	 *    индекс выборки
	 * @return
	 *    выборка, на которой оценивается качество алгоритма в процессе его запуска
	 *    с номером <code>index</code>
	 */
	@Override
	public SequenceSet getSet(int index) {
		boolean[] selector = new boolean[set.length()];
		for (int i = 0; i < set.length(); i++) {
			selector[i] = (foldIndex[i] == index/2);
			if (index % 2 == 0) {
				// Обучающая выборка
				selector[i] = !selector[i];
			}
		}
		
		return set.filter(selector);
	}
	
	/**
	 * Возвращает качество алгоритма распознавания скрытых последовательностей, усредненное
	 * по всем обучающим выборкам. В расчет берутся только те выборки, которые на данный
	 * момент обработаны целиком или частично.
	 * 
	 * @return
	 *    текущее усредненное качество распознавания на обучающих выборках
	 */
	public PredictionQuality meanTraining() {
		return meanTraining;
	}
	
	/**
	 * Возвращает качество алгоритма распознавания скрытых последовательностей, усредненное
	 * по всем контрольным выборкам. В расчет берутся только те выборки, которые на данный
	 * момент обработаны целиком или частично.
	 * 
	 * @return
	 *    текущее усредненное качество распознавания на контрольных выборках
	 */
	public PredictionQuality meanControl() {
		return meanControl;
	}
	
	/**
	 * Прикрепляет к этой коллекции выборок алгоритм распознавания.
	 * 
	 * @param algorithm
	 *    алгоритм, для которого будет оцениваться качество распознавания
	 */
	public void attachAlgorithm(SeqAlgorithm algorithm) {
		this.algorithm = algorithm;
	}
	
	/**
	 * Запускает оценку качества алгоритма распознавания скрытых последовательностей
	 * методом кросс-валидации. Алгоритм последовательно запускается для всех контрольных и, если
	 * переключатель {@link #skipTraining} установлен в значение {@code true}, обучающих выборок;
	 * обучающие и контрольные выборки чередуются между собой.
	 * При этом используется параллелизация вычислений по последовательностям с помощью класса
	 * {@link ThreadedAlgorithm}; количество потоков определяется {@linkplain Env#threadCount() окружением}.
	 * В процессе оценки качества в стандартный вывод печатается информация о прогрессе работы, 
	 * объем которой зависит от установленного {@linkplain Env#debugLevel() уровня детальности}. 
	 * 
	 * @throws IllegalStateException 
	 *    если к коллекции выборок не прикреплен алгоритм распознавания
	 */
	public void run(Env env) {
		if (algorithm == null) {
			throw new IllegalStateException(Messages.getString("test.no_alg"));
		}
		super.run(env);
	}
	
	protected void doRun() {
		SeqAlgorithm tAlgorithm = new ThreadedAlgorithm(algorithm, getEnv());
		getEnv().debug(1, reprHeader());
		
		for (int r = 0; r < runs.length; r++) {
			getEnv().debug(1, getRunName(r));
			getEnv().debug(1, runs[r].repr());
			
			if ((r % 2 == 0) && skipTraining) {
				getEnv().debug(1, Messages.getString("test.skip_train") + "\n");
				continue;
			}
			
			if (runs[r].isComplete()) {
				continue;
			}
				
			System.gc();
			final SequenceSet trainingSet = runs[r - (r % 2)].getSet();
			
			tAlgorithm.reset();
			tAlgorithm.train(trainingSet);
			runs[r].run(tAlgorithm);
			
			getEnv().debug(1, Messages.format("test.quality", runs[r].getQuality().repr()));
		}
	}
	
	@Override
	public void save() {
		if (!getEnv().interruptedByUser()) {
			getEnv().debugInline(2, "S");
		}
	}
	
	private String reprHeader() {
		int nProcessed = 0;
		for (AlgorithmRun run : runs) {
			nProcessed += run.getProcessedCount();
		}
		
		String repr = Messages.format("test.cv_repr", 
				runs.length / 2, set.length(), nProcessed) + "\n";
		repr += Messages.format("misc.dataset", set.repr()) + "\n";
		if (algorithm != null) {
			repr += Messages.format("test.alg", algorithm.repr()) + "\n";
		}
		return repr;
	}
	
	/**
	 * Возвращает название запуска алгоритма, позволяющее его идентифицировать.
	 * 
	 * @param index
	 *    индекс (с отсчетом от нуля) запуска
	 * @return
	 *    название запуска
	 */
	private String getRunName(int index) {
		return (index % 2 == 0) 
				? Messages.format("test.fold.train", index/2 + 1)
				: Messages.format("test.fold.ctrl", index/2 + 1);
	}
	
	public String repr() {
		String repr = reprHeader() + "\n";
		for (int i = 0; i < runs.length; i++) {
			repr += getRunName(i) + "\n";
			repr += runs[i].repr() + "\n";
		}
		
		repr += "\n";
		repr += Messages.format("test.mean_train", meanTraining.repr()) + "\n";
		repr += Messages.format("test.mean_ctrl", meanControl.repr());
		
		return repr;
	}
	
	/**
	 * При десериализации после стандартного механизма десериализации Java делается 
	 * попытка считать алгоритм распознавания. Даже если эта попытка приводит к ошибке (например, если
	 * изменилась сигнатура класса, соответствующего алгоритму), большинство методов коллекции 
	 * все равно можно использовать.
	 * 
	 * @param in
	 *    поток для считывания объекта
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		
		try {
			this.algorithm = (SeqAlgorithm) in.readObject();
		} catch (Exception e) {
			getEnv().debug(0, Messages.format("test.load_error", e));
		}
	}

	/**
	 * Алгоритм, для которого оценивается качество распознавания, сохраняется 
	 * отдельно после задействования стандартного механизма сериализации Java.
	 * При сохранении создается копия алгоритма с помощью метода {@link SeqAlgorithm#clearClone()},
	 * то есть сохраняются исключительно управляющие параметры алгоритма, но не параметры,
	 * полученные в результате обучения на прецедентах.
	 * 
	 * @param out
	 *    поток для записи объекта
	 * @throws IOException
	 */
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		if (this.algorithm != null) {
			SeqAlgorithm alg = (SeqAlgorithm) this.algorithm.clearClone();
			out.writeObject(alg);
		}
	}
}

package ua.kiev.icyb.bio;

import java.io.IOException;

import ua.kiev.icyb.bio.alg.ThreadedAlgorithm;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Класс для оценки качества алгоритма распознавания. Обучение и оценка качества алгоритма
 * могут проводиться на различных выборках.
 */
public class QualityEstimation extends AbstractLaunchable implements RunCollection, Representable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Полная выборка прецедентов, используемая при оценке качества.
	 */
	private final SequenceSet trainingSet;
	/**
	 * Полная выборка прецедентов, используемая при оценке качества.
	 */
	private final SequenceSet controlSet;
	
	/**
	 * Запуск алгоритма распознавания.
	 */
	private final AlgorithmRun run;
	
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
	 * Создает экземпляр класса для оценки качества распознавания.
	 * 
	 * @param trainingSet
	 *    выборка для обучения алгоритма
	 * @param controlSet
	 *    выборка для оценки качества алгоритма
	 */
	public QualityEstimation(SequenceSet trainingSet, SequenceSet controlSet) {
		this.trainingSet = trainingSet;
		this.controlSet = controlSet;
		this.run = new AlgorithmRun(this, 0);
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
	
	@Override
	public SequenceSet getSet(int index) {
		if (index == 0) {
			return controlSet;
		}
		throw new IllegalArgumentException("Invalid set index: " + index);
	}
	
	/**
	 * Возвращает качество распознавания.
	 * 
	 * @return
	 *    текущая оценка качества алгоритма распознавания
	 */
	public PredictionQuality getQuality() {
		return run.getQuality();
	}

	@Override
	protected void doRun() {
		if (algorithm == null) {
			throw new IllegalStateException(Messages.getString("test.no_alg"));
		}
		SeqAlgorithm tAlgorithm = new ThreadedAlgorithm(algorithm, getEnv());
		getEnv().debug(1, this.repr());
		
		tAlgorithm.train(trainingSet);
		run.run(tAlgorithm);
		getEnv().debug(1, Messages.format("test.quality", run.getQuality().repr()));
	}
	
	@Override
	public String repr() {
		String repr = Messages.format("test.repr", 
				controlSet.size(), run.getProcessedCount()) + "\n";
		repr += Messages.format("test.train_set", trainingSet.repr()) + "\n";
		repr += Messages.format("test.control_set", controlSet.repr()) + "\n";
		
		if (algorithm != null) {
			repr += Messages.format("test.alg", algorithm.repr()) + "\n";
		}
		if (run.getProcessedCount() > 0) {
			repr += Messages.format("test.quality", getQuality().repr());
		}
		return repr;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		try {
			this.algorithm = (SeqAlgorithm) in.readObject();
		} catch (Exception e) {
			getEnv().debug(0, Messages.format("test.load_error", e));
		}
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		if (this.algorithm != null) {
			SeqAlgorithm alg = (SeqAlgorithm) this.algorithm.clearClone();
			out.writeObject(alg);
		}
	}
}

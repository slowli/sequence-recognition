package ua.kiev.icyb.bio.alg.tree;

import java.util.HashMap;
import java.util.Map;

import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.AbstractSeqAlgorithm;
import ua.kiev.icyb.bio.res.Messages;


/**
 * Алгоритм распознавания, применяющий для распознавания скрытой последовательности
 * строки наблюдаемых состояний один из составляющих алгоритмов, выбираемый в зависимости
 * от свойств этой строки.
 */
public class SwitchAlgorithm extends AbstractSeqAlgorithm {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Алгоритмы, использующиеся для распознавания. 
	 */
	private SeqAlgorithm algorithms[];
	
	/**
	 * Индексы составляющих алгоритмов, которые обрабатывают каждую строку наблюдаемых
	 * состояний из {@link #set}.
	 */
	private Map<String, Byte> labels = new HashMap<String, Byte>(); 
	
	/**
	 * Создает новый алгоритм с заданными составляющими. Логика соответствия между 
	 * составляющими алгоритмами и строками наблюдаемых состояний не определяется;
	 * подклассы, использующие этот конструктор, должны позаботиться об этом.
	 * 
	 * @param algs
	 *    массив составляющих алгоритмов
	 */
	protected SwitchAlgorithm(SeqAlgorithm[] algs) {
		this.algorithms = algs.clone();
	}
	
	/**
	 * Создает новый алгоритм с априорно заданным распределением областей компетентности
	 * на конечном множестве прецедентов. При попытке использовать алгоритм на 
	 * строке наблюдаемых состояний, не входящей в прецеденты, будет вызвано исключение.
	 * 
	 * @param classes
	 *    индекс компетентного алгоритма для каждого прецедента
	 * @param set
	 *    набор прецедентов, который будет далее использоваться с этим алгоритмом
	 * @param algs
	 *    составляющие алгоритмы распознавания
	 */
	public SwitchAlgorithm(Map<String, Byte> labels, SeqAlgorithm[] algs) {
		this(algs);
		this.labels = labels;
	}
	
	
	/**
	 * Возвращает индекс компетентного составляющего алгоритма для заданной последовательности
	 * наблюдаемых состояний.
	 * 
	 * <p>Имплементация по умолчанию ищет строку состояний (по содержимому) в выборке,
	 * предоставленной с  {@linkplain #SwitchAlgorithm(byte[], SequenceSet, SeqAlgorithm[]) публичным конструктором}
	 * и возвращает соответствующий индекс алгоритма. Если строка не содержится в выборке,
	 * вызывается исключение времени исполнения.
	 * 
	 * <p>Подклассы должны переопределять метод, если они не используют упомянутый выше конструктор.
	 * 
	 * @param sequence 
	 *    последовательность наблюдаемых состояний
	 * @return 
	 *    индекс (с отсчетом от нуля) компетентного составляющего алгоритма
	 */
	public int index(Sequence sequence) {
		Byte idx = labels.get(sequence);
		if (idx == null) {
			throw new RuntimeException("Sequence is not in the training set!");
		}
		return idx;
	}
	
	@Override
	public void train(Sequence sequence) {
		algorithms[index(sequence)].train(sequence);
	}

	@Override
	public void reset() {
		for (SeqAlgorithm model: algorithms) {
			model.reset();
		}
	}

	@Override
	public byte[] run(Sequence sequence) {
		return algorithms[index(sequence)].run(sequence);
	}
	
	@Override
	public String toString() {
		return String.format("%d x %s", algorithms.length, algorithms[0]);
	}

	@Override
	public Object clearClone() {
		SwitchAlgorithm other = (SwitchAlgorithm) super.clearClone();
		other.algorithms = algorithms.clone();
		for (int i = 0; i < algorithms.length; i++) {
			other.algorithms[i] = (SeqAlgorithm) algorithms[i].clearClone();
		}
		other.labels = new HashMap<String, Byte>();
		other.labels.putAll(labels);
		return other;
	}
	
	@Override
	public String repr() {
		String repr = super.repr() + "\n";
		repr += Messages.format("alg.algs_n", algorithms.length) + "\n";
		repr += Messages.format("alg.base", algorithms[0].repr());
		return repr;
	}
}

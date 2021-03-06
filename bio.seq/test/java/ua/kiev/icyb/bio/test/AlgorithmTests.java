package ua.kiev.icyb.bio.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import ua.kiev.icyb.bio.CrossValidation;
import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.PredictionQuality;
import ua.kiev.icyb.bio.QualityEstimation;
import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.Approximation;
import ua.kiev.icyb.bio.alg.Distribution;
import ua.kiev.icyb.bio.alg.FallthruAlgorithm;
import ua.kiev.icyb.bio.alg.FallthruChain;
import ua.kiev.icyb.bio.alg.GeneTransformAlgorithm;
import ua.kiev.icyb.bio.alg.GeneViterbiAlgorithm;
import ua.kiev.icyb.bio.alg.MarkovChain;
import ua.kiev.icyb.bio.alg.TransformAlgorithm;
import ua.kiev.icyb.bio.alg.ViterbiAlgorithm;
import ua.kiev.icyb.bio.alg.tree.PriorityCompAlgorithm;
import ua.kiev.icyb.bio.filters.PeriodicTransform;
import ua.kiev.icyb.bio.filters.RandomFilter;
import ua.kiev.icyb.bio.filters.TerminalTransform;

/**
 * Тестирование алгоритмов распознавания.
 */
public class AlgorithmTests {
	
	private static Env env;
	
	private static SequenceSet set1;
	
	@BeforeClass
	public static void setup() throws IOException {
		final String testDir = System.getProperty("testdir", "test");
		env = new Env(testDir + "/env.conf");
		set1 = env.loadSet("elegans-I");
	}
	
	private static void checkSanity(PredictionQuality q) {
		assertTrue(q.symbolPrecision() > 0.5);
		assertTrue(q.symbolSpec(0) > 0.5);
		assertTrue(q.symbolSens(0) > 0.5);
		assertTrue(q.symbolACP(0) > 0.5);
		assertTrue(q.symbolCC(0) > 0.5);
		assertTrue(q.regionSens(0) > 0.25);
		assertTrue(q.regionSpec(0) > 0.25);
		
		assertTrue(q.symbolSpec(1) > 0.5);
		assertTrue(q.symbolSens(1) > 0.5);
		assertTrue(q.symbolACP(1) > 0.5);
		assertTrue(q.symbolCC(1) > 0.5);
		assertTrue(q.regionSens(1) > 0.25);
		assertTrue(q.regionSpec(1) > 0.25);
	}

	/**
	 * Тестирует алгоритм распознавания на простой выборке с использованием кросс-валидации.
	 * 
	 * @param set
	 *    выборка, используемая для тестирования
	 * @param algorithm
	 *    алгоритм, работу которого надо протестировать
	 *    
	 * @throws IOException
	 */
	public static void testAlgorithm(SequenceSet set, SeqAlgorithm algorithm) {
		boolean[] selector = new boolean[set.size()];
		for (int i = 0; i < 1000; i++) {
			selector[i] = true;
		}
		set = set.filter(selector);
		
		CrossValidation cv = new CrossValidation(set, 5);
		cv.attachAlgorithm(algorithm);
		cv.run(env);
		checkSanity(cv.meanControl());
		//System.out.println(cv.repr());
	}
	
	/**
	 * Проверяет эффективность алгоритма распознавания по максимизации правдоподобия.
	 * 
	 * @param algorithm
	 *    алгоритм, работу которого надо протестировать
	 * @param distr
	 *    распределение вероятности, относительно которого проверяется работа алгоритма
	 */
	public static void testAlgorithmFit(SeqAlgorithm algorithm, Distribution<Sequence> distr) {
		SequenceSet set = set1;
		algorithm.reset();
		distr.reset();
		algorithm.train(set);
		distr.train(set);
		
		set = set.filter(new RandomFilter(0.1));
		
		double trueToEst = 0.0, estToTrue = 0.0;
		int count = 0;
		for (Sequence seq : set) {
			byte[] hidden = algorithm.run(seq);
			if (hidden != null) {
				double trueP = distr.estimate(seq);
				double estP = distr.estimate(new Sequence("", seq.observed, hidden));
				trueToEst += Math.max(0.0, trueP - estP);
				estToTrue += Math.max(0.0, estP - trueP);
				count++;
			}
		}
		
		trueToEst /= count;
		estToTrue /= count;
		
		assertTrue(trueToEst < 10.0);
		assertTrue(estToTrue > 0.0);
		assertTrue(estToTrue < 50.0);
	}

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	private boolean isZeroArray(byte[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] != 0) return false;
		}
		
		return true;
	}
	
	/**
	 * Без обучения алгоритм распознавания должен отказываться от классификации 
	 * на любой строке.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAlgorithmNoData() throws IOException {
		SequenceSet set = set1;
		ViterbiAlgorithm alg = new ViterbiAlgorithm(1, 6);
		byte[] result = alg.run(set.get(0));
		assertNull(result);
	}
	
	/**
	 * Проверка выполнения алгоритмов распознавания.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAlgorithmRun() throws IOException {
		SequenceSet set = set1;
		ViterbiAlgorithm alg = new ViterbiAlgorithm(1, 6);
		alg.train(set);
		byte[] result = alg.run(set.get(0));
		assertFalse(isZeroArray(result));
	}

	/**
	 * Проверка операции клонирования алгоритмов.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAlgorithmClone() throws IOException {
		SequenceSet set = set1;
		Sequence seq = set.get(0);
		
		ViterbiAlgorithm alg = new ViterbiAlgorithm(1, 6);
		alg.train(set);
		
		
		SeqAlgorithm otherAlg = alg.clearClone();
		byte[] result = otherAlg.run(seq);
		assertNull(result);
		
		otherAlg.train(set);
		assertArrayEquals(alg.run(seq), otherAlg.run(seq));
	}
	
	/**
	 * Проверка сериализации алгоритмов.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAlgorithmSerialization() throws IOException {
		SequenceSet set = set1;
		
		ViterbiAlgorithm alg = new ViterbiAlgorithm(1, 6);
		alg.train(set);
		
		File file = tempFolder.newFile();
		env.save(alg, file.getAbsolutePath());
		
		assertTrue(file.isFile());
		// Файл большого размера, т.к. содержит данные обучения
		assertTrue(file.length() > 50000);
		
		ViterbiAlgorithm copy = env.load(file.getAbsolutePath());
		assertArrayEquals(alg.run(set.get(0)), copy.run(set.get(0)));
	}
	
	/**
	 * Проверка сериализации алгоритмов, когда сохраняются исключительно его гиперпараметры.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAlgorithmClearSerialization() throws IOException {
		SequenceSet set = set1;
		
		ViterbiAlgorithm alg = new ViterbiAlgorithm(1, 6);
		alg.train(set);
		
		File file = tempFolder.newFile();
		env.save((ViterbiAlgorithm) alg.clearClone(), file.getAbsolutePath());
		
		assertTrue(file.isFile());
		assertTrue(file.length() < 2000);
		
		ViterbiAlgorithm copy = env.load(file.getAbsolutePath());
		assertNull(copy.run(set.get(0)));
		
		copy.train(set);
		assertArrayEquals(alg.run(set.get(0)), copy.run(set.get(0)));
	}
	
	/**
	 * Проверка класса {@link ua.kiev.icyb.bio.alg.EstimatesSet EstimatesSet} (алгоритм не обучается на данных).
	 * 
	 * @throws IOException
	 */
	@Test
	public void testEstimatesNoData() throws IOException {
		SequenceSet set = set1;
		
		ViterbiAlgorithm alg = new ViterbiAlgorithm(1, 6);
		
		boolean[] selector = new boolean[set.size()];
		for (int i = 0; i < 10; i++) {
			selector[i] = true;
		}
		SequenceSet subset = set.filter(selector);
		
		SequenceSet estimates = alg.runSet(subset);
		assertEquals(set.observedStates(), estimates.observedStates());
		assertEquals(set.hiddenStates(), estimates.hiddenStates());
		assertEquals(set.completeStates(), estimates.completeStates());
		assertEquals(subset.size(), estimates.size());
		
		for (int i = 0; i < estimates.size(); i++) {
			assertSame(set.observed(i), estimates.observed(i));
			assertSame(set.observed(i), estimates.get(i).observed);
			assertSame(set.id(i), estimates.id(i));
			assertSame(set.id(i), estimates.get(i).id);
			assertNull(estimates.hidden(i));
			assertNull(estimates.get(i).hidden);
		}
	}
	
	/**
	 * Проверка класса {@link ua.kiev.icyb.bio.alg.EstimatesSet EstimatesSet}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testEstimates() throws IOException {
		SequenceSet set = set1;
		
		ViterbiAlgorithm alg = new ViterbiAlgorithm(1, 6);
		alg.train(set);
		
		byte[] result = alg.run(set.get(0));
		
		boolean[] selector = new boolean[set.size()];
		for (int i = 0; i < 10; i++) {
			selector[i] = true;
		}
		SequenceSet subset = set.filter(selector);
		
		SequenceSet estimates = alg.runSet(subset);
		assertEquals(set.observedStates(), estimates.observedStates());
		assertEquals(set.hiddenStates(), estimates.hiddenStates());
		assertEquals(set.completeStates(), estimates.completeStates());
		assertEquals(subset.size(), estimates.size());
		
		assertSame(set.observed(3), estimates.observed(3));
		assertSame(set.id(2), estimates.id(2));
		assertArrayEquals(result, estimates.hidden(0));
		assertArrayEquals(result, estimates.get(0).hidden);
	}
	
	/**
	 * Проверка базовой функциональности класса {@link QualityEstimation}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testQuality() throws IOException {
		SequenceSet set = set1;
		
		boolean[] selector = new boolean[set.size()];
		for (int i = 0; i < 10; i++) {
			selector[i] = true;
		}
		SequenceSet subset = set.filter(selector);
		
		ViterbiAlgorithm alg = new ViterbiAlgorithm(1, 6);
		
		QualityEstimation est = new QualityEstimation(set, subset);
		est.attachAlgorithm(alg);
		est.run(env);
		checkSanity(est.getQuality());
	}
	
	/**
	 * Проверка сериализации класса {@link QualityEstimation}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testQualitySave() throws IOException {
		SequenceSet set = set1;
		
		boolean[] selector = new boolean[set.size()];
		for (int i = 0; i < 10; i++) {
			selector[i] = true;
		}
		SequenceSet subset = set.filter(selector);
		
		ViterbiAlgorithm alg = new ViterbiAlgorithm(1, 6);
		
		QualityEstimation est = new QualityEstimation(set, subset);
		est.attachAlgorithm(alg);
		
		File file = tempFolder.newFile(); 
		env.save(est, file.getAbsolutePath());
		
		assertTrue(file.isFile());
		assertTrue(file.length() < 2000);
		
		est = env.load(file.getAbsolutePath());
		assertEquals(subset.size(), est.getSet(0).size());
		est.run(env);
		checkSanity(est.getQuality());
	}
	
	private Set<String> getIds(SequenceSet set) {
		Set<String> ids = new HashSet<String>(set.size());
		for (int i = 0; i < set.size(); i++) {
			ids.add(set.id(i));
		}
		return ids;
	}
	
	/**
	 * Проверка выделения обчающих и контрольных выборок при кросс-валидации.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCVSets() throws IOException {
		SequenceSet set = set1;
		
		CrossValidation cv = new CrossValidation(set, 5);
		for (int f = 0; f < 10; f += 2) {
			assertTrue(cv.getSet(f).size() > 0.6 * set.size());
			assertTrue(cv.getSet(f).size() < 0.9 * set.size());
			assertTrue(cv.getSet(f + 1).size() > 0.1 * set.size());
			assertTrue(cv.getSet(f + 1).size() < 0.3 * set.size());
			
			assertEquals(set.size(), cv.getSet(f).size() + cv.getSet(f + 1).size());
			
			// проверить, что обучающая и контрольная выборки не пересекаются
			Set<String> trainIds = getIds(cv.getSet(f)), ctrlIds = getIds(cv.getSet(f + 1));
			assertTrue(Collections.disjoint(trainIds, ctrlIds));
		}
	}
	
	/**
	 * Проверка кросс-валидации.
	 * 
	 * @throws IOException
	 */
	@Test
	@Category(SlowTest.class)
	public void testCV() throws IOException {
		SequenceSet set = set1;
		
		boolean[] selector = new boolean[set.size()];
		for (int i = 0; i < 1000; i++) {
			selector[i] = true;
		}
		set = set.filter(selector);
		
		CrossValidation cv = new CrossValidation(set, 5);
		cv.attachAlgorithm(new ViterbiAlgorithm(1, 6));
		
		cv.run(env);
		checkSanity(cv.meanControl());
	}
	
	/**
	 * Проверка сериализации класса {@link CrossValidation}.
	 * 
	 * @throws IOException
	 */
	@Test
	@Category(SlowTest.class)
	public void testCVSave() throws IOException {
		SequenceSet set = set1;
		
		boolean[] selector = new boolean[set.size()];
		for (int i = 0; i < 1000; i++) {
			selector[i] = true;
		}
		set = set.filter(selector);
		
		CrossValidation cv = new CrossValidation(set, 5);
		cv.attachAlgorithm(new ViterbiAlgorithm(1, 6));
		
		File file = tempFolder.newFile(); 
		env.save(cv, file.getAbsolutePath());
		
		assertTrue(file.isFile());
		assertTrue(file.length() < 10000);
		
		cv = env.load(file.getAbsolutePath());
		cv.run(env);
		checkSanity(cv.meanControl());
	}
	
	/**
	 * Проверка алгоритма Витерби.
	 */
	@Test
	@Category(SlowTest.class)
	public void testViterbiAlgorithm() {
		testAlgorithm(set1, new ViterbiAlgorithm(1, 6));
	}
	
	/**
	 * Проверка качества оптимизации лограифмического правдоподобия алгоритмом Витерби.
	 */
	@Test
	@Category(SlowTest.class)
	public void testViterbiAlogrithmFit() {
		SeqAlgorithm alg = new ViterbiAlgorithm(1, 6);
		Distribution<Sequence> distr = new MarkovChain(1, 6, set1.states());
		testAlgorithmFit(alg, distr);
	}
	
	@Test
	public void testTransformAlgorithmClearClone() {
		SeqAlgorithm alg = new TransformAlgorithm(new ViterbiAlgorithm(1, 6), new TerminalTransform());
		alg.train(set1);
		assertNotNull(alg.run(set1.get(0)));
		
		SeqAlgorithm clearClone = alg.clearClone();
		assertNull(clearClone.run(set1.get(0)));
		assertNotNull(alg.run(set1.get(0)));
	}
	
	@Test
	public void testTerminalAlgorithmConstraints() {
		SeqAlgorithm alg = new TransformAlgorithm(new ViterbiAlgorithm(1, 6), new TerminalTransform());
		alg.train(set1);
		for (int i = 0; i < 100; i++) {
			final Sequence seq = set1.get(i);
			byte[] hidden = alg.run(seq);
			if (hidden == null) continue;
			
			assertEquals(seq.length(), hidden.length);
			assertEquals(0, hidden[seq.length() - 1]);
		}
	}
	
	@Test
	public void testGeneAlgorithmConstraints() {
		SeqAlgorithm alg = new GeneTransformAlgorithm(5);
		alg.train(set1);
		for (int i = 0; i < 20; i++) {
			final Sequence seq = set1.get(i);
			byte[] hidden = alg.run(seq);
			if (hidden == null) continue;
			
			assertEquals(seq.length(), hidden.length);
			assertEquals(0, hidden[seq.length() - 1]);
			int exCount = 0;
			for (int pos = 0; pos < hidden.length; pos++) {
				if (hidden[pos] == 0) exCount++;
			}
			assertEquals(0, exCount % 3);
		}
	}
	
	/**
	 * Проверка алгоритма Витерби (модификация для генов).
	 */
	@Test
	@Category(SlowTest.class)
	public void testGeneViterbiAlgorithm() {
		testAlgorithm(set1, new GeneViterbiAlgorithm(6, true));
	}
	
	@Test
	@Category(SlowTest.class)
	public void testPeriodicViterbiAlgorithm() {
		SeqAlgorithm alg = new TransformAlgorithm(new ViterbiAlgorithm(1, 5), new PeriodicTransform());
		testAlgorithm(set1, alg);
	}
	
	@Test
	@Category(SlowTest.class)
	public void testGeneTransformAlgorithm() {
		SeqAlgorithm alg = new GeneTransformAlgorithm(5);
		testAlgorithm(set1, alg);
	}
	
	@Test
	@Category(SlowTest.class)
	public void testGeneTransformAlgorithmFit() {
		SeqAlgorithm alg = new GeneTransformAlgorithm(5);
		Distribution<Sequence> distr = new MarkovChain(1, 5, set1.states());
		testAlgorithmFit(alg, distr);
	}
	
	/**
	 * Проверка качества оптимизации лограифмического правдоподобия алгоритмом Витерби для генов.
	 */
	@Test
	@Category(SlowTest.class)
	public void testGeneViterbiAlgorithmFit() {
		SeqAlgorithm alg = new GeneViterbiAlgorithm(6, true);
		Distribution<Sequence> distr = new MarkovChain(1, 6, set1.states());
		testAlgorithmFit(alg, distr);
	}
	
	/**
	 * Проверка алгоритма Витерби с цепями переменного порядка.
	 */
	@Test
	@Category(SlowTest.class)
	public void testFallthruAlgorithm() {
		testAlgorithm(set1,
				new FallthruAlgorithm(new Approximation(
						6, 3, Approximation.Strategy.FIRST)));
	}
	
	@Test
	@Category(SlowTest.class)
	public void testFallthruAlgorithmFit() {
		final Approximation approx = new Approximation(6, 3, Approximation.Strategy.FIRST);
		SeqAlgorithm alg = new FallthruAlgorithm(approx);
		Distribution<Sequence> distr = new FallthruChain(approx, set1.states());
		testAlgorithmFit(alg, distr);
	}
	
	/**
	 * Проверка алгоритмических композиций с голосованием по старшинству.
	 * 
	 * @throws IOException
	 */
	@Test
	@Category(SlowTest.class)
	public void testPriorityCompAlgorithm() {
		testAlgorithm(set1, new PriorityCompAlgorithm(3, 6));
	}
}

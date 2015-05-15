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
import org.junit.rules.TemporaryFolder;

import ua.kiev.icyb.bio.CrossValidation;
import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.PredictionQuality;
import ua.kiev.icyb.bio.QualityEstimation;
import ua.kiev.icyb.bio.SeqAlgorithm;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.Approximation;
import ua.kiev.icyb.bio.alg.FallthruAlgorithm;
import ua.kiev.icyb.bio.alg.GeneViterbiAlgorithm;
import ua.kiev.icyb.bio.alg.ViterbiAlgorithm;
import ua.kiev.icyb.bio.alg.tree.PriorityCompAlgorithm;

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
	
		System.out.println(q.repr());
	}

	/**
	 * Тестирует алгоритм распознавания на простой выборке с использованием кросс-валидации.
	 * 
	 * @param algorithm
	 *    алгоритм, работу которого надо протестировать
	 *    
	 * @throws IOException
	 */
	public static void testAlgorithm(SeqAlgorithm algorithm) throws IOException {
		SequenceSet set = set1;
		boolean[] selector = new boolean[set.size()];
		for (int i = 0; i < 1000; i++) {
			selector[i] = true;
		}
		set = set.filter(selector);
		
		CrossValidation cv = new CrossValidation(set, 5);
		cv.attachAlgorithm(algorithm);
		cv.run(env);
		checkSanity(cv.meanControl());
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
		System.out.println("File length: " + file.length());
		
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
		System.out.println("File length: " + file.length());
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
		System.out.println(subset.repr());
		
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
		System.out.println(subset.repr());
		
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
		System.out.println("File length: " + file.length());
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
		System.out.println("File length: " + file.length());
		assertTrue(file.length() < 10000);
		
		cv = env.load(file.getAbsolutePath());
		cv.run(env);
		checkSanity(cv.meanControl());
	}
	
	/**
	 * Проверка алгоритма Витерби.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testViterbiAlgorithm() throws IOException {
		testAlgorithm(new ViterbiAlgorithm(1, 6));
	}
	
	/**
	 * Проверка алгоритма Витерби (модификация для генов).
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGeneViterbiAlgorithm() throws IOException {
		testAlgorithm(new GeneViterbiAlgorithm(6, true));
	}
	
	/**
	 * Проверка алгоритма Витерби с цепями переменного порядка.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testFallthruAlgorithm() throws IOException {
		testAlgorithm(new FallthruAlgorithm(
				new Approximation(6, 3, Approximation.Strategy.FIRST)));
	}
	
	/**
	 * Проверка алгоритмических композиций с голосованием по старшинству.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPriorityCompAlgorithm() throws IOException {
		testAlgorithm(new PriorityCompAlgorithm(3, 6));
	}
}

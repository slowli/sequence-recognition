package ua.kiev.icyb.bio.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.DistributionUtils;
import ua.kiev.icyb.bio.alg.EmpiricalDistribution;
import ua.kiev.icyb.bio.alg.Fragment;
import ua.kiev.icyb.bio.alg.FragmentFactory;
import ua.kiev.icyb.bio.alg.MarkovChain;

/**
 * Тестирует распределения на основе скрытых моделей Маркова.
 */
public class DistributionTests {
	
	private static final String SET_1 = "elegans-I";
	
	private static final String CONF_FILE = "tests/env.conf";
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	/**
	 * Тестирует создание фрагментов.
	 */
	@Test
	public void testFragmentCreation() {
		FragmentFactory factory = new FragmentFactory("ACGT", "xi", "ACGTacgt", 5);
		Fragment fragment = factory.fragment(2, 1, 1);
		assertEquals("g", fragment.toString());
		assertEquals(6, fragment.index());
		
		fragment = factory.fragment(2, 1, 2);
		assertEquals(1 * 16 + 2, fragment.index());
		assertEquals("Ag", fragment.toString());
		
		fragment = factory.fragment(3, 3, 2);
		assertEquals(3 * 16 + 3, fragment.index());
		assertEquals("at", fragment.toString());
	}
	
	/**
	 * Тестирует создание фрагментов с использованием байтовых массивов.
	 */
	@Test
	public void testFragmentCreationFromArrays() {
		FragmentFactory factory = new FragmentFactory("ACGT", "xi", "ACGTacgt", 5);
		// TCgaGt
		byte[] observed = new byte[] { 3, 1, 2, 0, 2, 3 };
		byte[] hidden   = new byte[] { 0, 0, 1, 1, 0, 1 };
		
		Fragment fragment = factory.fragment(observed, hidden, 0, 1);
		assertEquals(1, fragment.length);
		assertEquals(3, fragment.observed);
		assertEquals(0, fragment.hidden);
		assertEquals("T", fragment.toString());
		
		fragment = factory.fragment(observed, hidden, 1, 4);
		assertEquals(4, fragment.length);
		assertEquals("CgaG", fragment.toString());
		
		factory.fragment(observed, hidden, 3, 3, fragment);
		assertEquals(3, fragment.length);
		assertEquals("aGt", fragment.toString());
		
		factory.fragment(observed, 2, 2, 2, fragment);
		assertEquals(2, fragment.length);
		assertEquals(2, fragment.hidden);
		assertEquals("gA", fragment.toString());
	}

	/**
	 * Тестирует определение префиксов фрагментов.
	 */
	@Test
	public void testFragmentPrefix() {
		FragmentFactory factory = new FragmentFactory("ACGT", "xi", "ACGTacgt", 5);
		// TCgaGt
		byte[] observed = new byte[] { 3, 1, 2, 0, 2, 3 };
		byte[] hidden   = new byte[] { 0, 0, 1, 1, 0, 1 };
		
		// TCgaG
		Fragment fragment = factory.fragment(observed, hidden, 0, 5);
		Fragment prefix = fragment.prefix(2);
		assertEquals(2, prefix.length);
		assertEquals("TC", prefix.toString());
		
		fragment.prefix(4, prefix);
		assertEquals(4, prefix.length);
		assertEquals("TCga", prefix.toString());
		
		fragment.prefix(3, fragment);
		assertEquals(3, fragment.length);
		assertEquals("TCg", fragment.toString());
	}
	
	/**
	 * Тестирует определение суффиксов фрагментов.
	 */
	@Test
	public void testFragmentSuffix() {
		FragmentFactory factory = new FragmentFactory("ACGT", "xi", "ACGTacgt", 5);
		// TCgaGt
		byte[] observed = new byte[] { 3, 1, 2, 0, 2, 3 };
		byte[] hidden   = new byte[] { 0, 0, 1, 1, 0, 1 };
		
		// TCgaG
		Fragment fragment = factory.fragment(observed, hidden, 0, 5);
		Fragment suffix = fragment.suffix(2);
		assertEquals(2, suffix.length);
		assertEquals("aG", suffix.toString());
		
		fragment.suffix(4, suffix);
		assertEquals(4, suffix.length);
		assertEquals("CgaG", suffix.toString());
		
		fragment.suffix(3, fragment);
		assertEquals(3, fragment.length);
		assertEquals("gaG", fragment.toString());
	}
	
	/**
	 * Тестирует конкатенацию фрагментов.
	 */
	@Test
	public void testFragmentAppend() {
		FragmentFactory factory = new FragmentFactory("ACGT", "xi", "ACGTacgt", 10);
		// TCgaGt
		byte[] observed = new byte[] { 3, 1, 2, 0, 2, 3 };
		byte[] hidden   = new byte[] { 0, 0, 1, 1, 0, 1 };
		
		Fragment fragment = factory.fragment(observed, hidden, 1, 4);
		assertEquals("CgaG", fragment.toString());
		Fragment other = factory.fragment(observed, hidden, 3, 2);
		assertEquals("aG", other.toString());
		
		Fragment concat = fragment.append(other);
		assertEquals(fragment.length + other.length, concat.length);
		assertEquals("CgaGaG", concat.toString());
		
		fragment.append(other, fragment);
		assertEquals(6, fragment.length);
		assertEquals("CgaGaG", fragment.toString());
		
		fragment.append(other, other);
		assertEquals(8, other.length);
		assertEquals("CgaGaGaG", other.toString());
	}
	
	/**
	 * Тестирует обучение скрытой марковской модели на отдельном прецеденте.
	 */
	@Test
	public void testMarkovChainTrain() {
		MarkovChain chain = new MarkovChain(1, 1, "ACGT", "xi", null);
		final FragmentFactory factory = chain.factory();
		
		// TCgaGtgt
		byte[] observed = new byte[] { 3, 1, 2, 0, 2, 3, 2, 3 };
		byte[] hidden   = new byte[] { 0, 0, 1, 1, 0, 1, 1, 1 };
		
		Sequence sequence = new Sequence(observed, hidden);
		chain.train(sequence);

		assertEquals(1, chain.getInitialTable().size());
		assertEquals(sequence.length() - 2, chain.getTransitionTable().size());
		
		for (Fragment fragment : factory.allFragments(1)) {
			if ((fragment.observed == 3) && (fragment.hidden == 0)) {
				assertEquals(1.0, chain.getInitialP(fragment), 1e-6);
			} else {
				assertEquals(0.0, chain.getInitialP(fragment), 1e-6);
			}
		}
		
		// p(g|C) = 1.0; p([^g]|C) = 0.0
		Fragment tail = factory.fragment(1, 0, 1);
		for (Fragment head : factory.allFragments(1)) {
			if ((head.observed == 2) && (head.hidden == 1)) {
				assertEquals(1.0, chain.getTransP(tail, head), 1e-6);
			} else {
				assertEquals(0.0, chain.getTransP(tail, head), 1e-6);
			}
		}
		
		// p(a|g) = p(t|g) = 0.5
		tail = factory.fragment(2, 1, 1);
		for (Fragment head : factory.allFragments(1)) {
			if (((head.observed == 0) || (head.observed == 3)) && (head.hidden == 1)) {
				assertEquals(0.5, chain.getTransP(tail, head), 1e-6);
			} else {
				assertEquals(0.0, chain.getTransP(tail, head), 1e-6);
			}
		}
	}
	
	/**
	 * Тестирует обучение скрытой марковской модели на выборке.
	 */
	@Test
	public void testMarkovChainTrainOnSet() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		MarkovChain chain = new MarkovChain(1, 4, set);
		chain.train(set);
		final FragmentFactory factory = chain.factory();
		
		assertEquals(6, chain.getInitialTable().size());
		int nVars = 0;
		
		List<Fragment> heads = factory.allFragments(1);
		
		for (Fragment tail : factory.allFragments(4)) {
			double p = 0.0;
			int localVars = 0;
			for (Fragment head : heads) {
				p += chain.getTransP(tail, head);
				if (chain.getTransP(tail, head) > 0) {
					localVars++;
				}
			}
			
			if (localVars > 0) {
				assertEquals(1.0, p, 1e-6);
				nVars += (localVars - 1);
			}
		}
		
		assertTrue((nVars > 2000) && (nVars < 3000));
	}
	
	/**
	 * Тестирует определение правдоподобия после обучения на отдельном прецеденте.
	 */
	@Test
	public void testMarkovChainEstimate() {
		MarkovChain chain = new MarkovChain(1, 1, "ACGT", "xi", null);
		
		// TCgaGtgt
		byte[] observed = new byte[] { 3, 1, 2, 0, 2, 3, 2, 3 };
		byte[] hidden   = new byte[] { 0, 0, 1, 1, 0, 1, 1, 1 };
		
		Sequence sequence = new Sequence(observed, hidden);
		chain.train(sequence);
		
		double logP = chain.estimate(sequence);
		assertEquals(Math.log(0.25), logP, 1e-6);
	}
	
	/**
	 * Тестирует определение правдоподобия после обучения на выборке.
	 */
	@Test
	public void testMarkovChainEstimateOnSet() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		MarkovChain chain = new MarkovChain(1, 4, set);
		chain.train(set);
		
		for (Sequence sequence : set) {
			double logP = chain.estimate(sequence);
			logP /= sequence.length();
			assertTrue(logP < -1.0);
			assertTrue(logP > -5.0);
		}
	}
	
	/**
	 * Тестирует сериализацию марковских моделей.
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testMarkovChainSerialization() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		MarkovChain chain = new MarkovChain(1, 5, set);
		chain.train(set);
		
		File file = tempFolder.newFile();
		env.save(chain, file.getAbsolutePath());
		assertTrue(file.isFile());
		assertTrue(file.length() < 500000);
		
		MarkovChain copy = env.load(file.getAbsolutePath());
		assertEquals(chain.estimate(set.get(0)), copy.estimate(set.get(0)), 1e-6);
	}
	
	/**
	 * Тестирует сериализацию марковских моделей после сбрасывания результатов обучения.
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testMarkovChainClearSerialization() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		MarkovChain chain = new MarkovChain(1, 5, set);
		chain.train(set);
		chain.reset();
		
		File file = tempFolder.newFile();
		env.save(chain, file.getAbsolutePath());
		assertTrue(file.isFile());
		assertTrue(file.length() < 5000);
	}
	
	/**
	 * Тестирует метод {@link DistributionUtils#choose(Object[], double[])}.
	 */
	@Test
	public void testDistributionUtilsChoose() {
		String[] objects = new String[] { "1", "2", "3" };
		double[] probabilities = new double[] { 0.1, 0.6, 0.3 };
		
		int[] counts = new int[objects.length];
		final int nSamples = 100000;
		for (int i = 0; i < nSamples; i++) {
			String obj = DistributionUtils.choose(objects, probabilities);
			for (int j = 0; j < objects.length; j++) {
				if (objects[j].equals(obj)) {
					counts[j]++;
				}
			}
		}
		
		for (int j = 0; j < objects.length; j++) {
			double expectedCount = nSamples * probabilities[j];
			assertTrue(Math.abs(expectedCount - counts[j]) < 3.0 * Math.sqrt(expectedCount));
		}
	}
	
	/**
	 * Тестирует эмпирическое распределение со сглаживанием.
	 */
	@Test
	public void testEmpiricalDistribution() {
		final int max = 1000, nSamples = 10000;
		
		EmpiricalDistribution distr = new EmpiricalDistribution(max, 100, 1e-7);
		for (int i = 0; i < nSamples; i++) {
			int sample = (int) Math.floor(Math.random() * max);
			distr.train(sample);
		}
		
		double p = 0;
		for (int i = 0; i <= max; i++) {
			p += Math.exp(distr.estimate(i));
		}
		assertEquals(1.0, p, 0.01);
	}
	
	/**
	 * Тестирует генерацию случайных чисел эмпирическим распределением.
	 */
	@Test
	public void testEmpiricalDistributionGenerate() {
		final int max = 1000, nSamples = 10000;
		
		EmpiricalDistribution distr = new EmpiricalDistribution(max, 100, 1e-7);
		for (int i = 0; i < nSamples; i++) {
			int sample = (int) Math.floor(Math.random() * max);
			distr.train(sample);
		}
		
		double mean = 0.0, dev = 0.0;
		int[] samples = new int[nSamples];
		for (int i = 0; i < nSamples; i++) {
			samples[i] = distr.generate();
			mean += 1.0 * samples[i] / nSamples;
		}
		for (int i = 0; i < nSamples; i++) {
			dev += (samples[i] - mean) * (samples[i] - mean) / (nSamples - 1);
		}
		dev = Math.sqrt(dev);
		
		assertEquals(0.5 * max, mean, 5.0);
		assertEquals(max / Math.sqrt(12), dev, 5.0);
	}
	
	/**
	 * Тестирует генерацию данных марковским распределением.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMarkovChainGenetate() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		MarkovChain chain = new MarkovChain(1, 4, set);
		chain.train(set);
		
		Sequence seq = chain.generate();
		assertTrue(seq.length() > 0);
		assertEquals(seq.observed.length, seq.hidden.length);
		
		int[] observedStats = new int[set.observedStates().length()];
		for (int i = 0; i < seq.length(); i++) {
			observedStats[seq.observed[i]]++;
		}
		
		for (int i = 0; i < observedStats.length; i++) {
			assertTrue(observedStats[i] > 0.1 * seq.length());
			assertTrue(observedStats[i] < 0.5 * seq.length());
		}
		
		double logP = chain.estimate(seq) / seq.length();
		assertTrue(logP < -1.0);
		assertTrue(logP > -5.0);
	}
}

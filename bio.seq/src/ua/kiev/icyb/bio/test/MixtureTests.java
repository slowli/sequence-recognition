package ua.kiev.icyb.bio.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.SimpleSequenceSet;
import ua.kiev.icyb.bio.alg.MarkovChain;
import ua.kiev.icyb.bio.alg.mixture.EMAlgorithm;
import ua.kiev.icyb.bio.alg.mixture.IncrementalEMAlgorithm;
import ua.kiev.icyb.bio.alg.mixture.MarkovMixture;
import ua.kiev.icyb.bio.alg.mixture.MixtureWeights;

/**
 * Тесты, связанные со смесями моделей.
 */
public class MixtureTests {

	private static final String SET_1 = "elegans-I";
	
	private static final String SET_2 = "elegans-II";
	
	public static final String CONF_FILE = "tests/env.conf";
	
	/**
	 * Проверяет добавление компонент в модель.
	 */
	@Test
	public void testMixtureAdd() {
		MarkovMixture mixture = new MarkovMixture();
		MarkovChain mc1 = new MarkovChain(1, 6, "ACGT", "xi");
		mixture.add(mc1, 1.0);
		
		assertEquals(1, mixture.size());
		assertEquals(1.0, mixture.weight(0), 1e-6);
		
		MarkovChain mc2 = new MarkovChain(1, 5, "ACGT", "xi");
		mixture.add(mc2, 0.5);
		
		assertEquals(2, mixture.size());
		assertEquals(0.5, mixture.weight(0), 1e-6);
		assertEquals(0.5, mixture.weight(1), 1e-6);
		assertSame(mc1, mixture.model(0));
		assertSame(mc2, mixture.model(1));
		
		MarkovChain mc3 = new MarkovChain(1, 5, "ACGT", "xi");
		mixture.add(mc3, 0.5);
		
		assertEquals(3, mixture.size());
		assertEquals(0.25, mixture.weight(0), 1e-6);
		assertEquals(0.25, mixture.weight(1), 1e-6);
		assertEquals(0.5, mixture.weight(2), 1e-6);
		assertSame(mc3, mixture.model(2));
	}
	
	/**
	 * Проверяет удаление компонент из модели.
	 */
	@Test
	public void testMixtureDelete() {
		MarkovMixture mixture = new MarkovMixture();
		MarkovChain mc1 = new MarkovChain(1, 6, "ACGT", "xi");
		mixture.add(mc1, 1.0);
		MarkovChain mc2 = new MarkovChain(1, 5, "ACGT", "xi");
		mixture.add(mc2, 0.5);
		MarkovChain mc3 = new MarkovChain(1, 5, "ACGT", "xi");
		mixture.add(mc3, 0.5);
		
		assertEquals(0.25, mixture.weight(0), 1e-6);
		assertEquals(0.25, mixture.weight(1), 1e-6);
		assertEquals(0.5, mixture.weight(2), 1e-6);
		
		mixture.delete(1);
		assertEquals(2, mixture.size());
		assertEquals(1.0 / 3, mixture.weight(0), 1e-6);
		assertEquals(2.0 / 3, mixture.weight(1), 1e-6);
		assertSame(mc1, mixture.model(0));
		assertSame(mc3, mixture.model(1));
		
		mixture.delete(0);
		assertEquals(1, mixture.size());
		assertEquals(1.0, mixture.weight(0), 1e-6);
		assertSame(mc3, mixture.model(0));
	}
	
	/**
	 * Проверяет создание смеси с заданным видом компонент.
	 */
	@Test
	public void testMixtureNew() {
		SequenceSet dummySet = new SimpleSequenceSet("ACGT", "xi", null);
		MarkovMixture mixture = new MarkovMixture(4, 6, dummySet);
		
		assertEquals(4, mixture.size());
		for (int i = 0; i < 4; i++) {
			assertEquals(0.25, mixture.weight(i), 1e-6);
			assertEquals(6, mixture.model(i).order());
			
			for (int j = 0; j < i; j++) {
				assertNotSame(mixture.model(i), mixture.model(j));
			}
		}
	}
	
	@Test
	public void testMixtureRandomFill() {
		fail("Not implemented");
	}
	
	@Test
	public void testMixtureWeights() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set1 = env.loadSet(SET_1);
		SequenceSet set2 = env.loadSet(SET_2);
		
		MarkovMixture mixture = new MarkovMixture(2, 5, set1);
		mixture.model(0).train(set1);
		mixture.model(1).train(set2);
		
		MixtureWeights mw = new MixtureWeights(mixture, set2);
		mw.run(env);
		
		for (int i = 0; i < set2.size(); i++) {
			assertEquals(1.0, mw.weights[0][i] + mw.weights[1][i], 1e-6);
			int label = mw.labels.get(set2.id(i));
			int expLabel = (mw.weights[0][i] > 0.5) ? 0 : 1;
			assertEquals(expLabel, label);
		}
	}
	
	public static void checkSanity(MarkovMixture mixture) {
		assertTrue(mixture.size() > 0);
		assertEquals(mixture.size(), mixture.weights().length);
		
		double sum = 0.0;
		for (int i = 0; i < mixture.size(); i++) {
			double w = mixture.weight(i);
			assertEquals(w, mixture.weights()[i], 1e-6);
			assertTrue(w >= 0.0);
			assertTrue(w <= 1.0);
			sum += w;
		}
		assertEquals(1.0, sum, 1e-6);
		
		for (int i = 0; i < mixture.size(); i++) {
			assertNotNull(mixture.model(i));
		}
	}
	
	@Test
	public void testEMAlgorithm() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set1 = env.loadSet(SET_1);
		SequenceSet set2 = env.loadSet(SET_2);
		
		MarkovMixture mixture = new MarkovMixture(2, 5, set1);
		mixture.model(0).train(set1);
		mixture.model(1).train(set2);
		
		EMAlgorithm alg = new EMAlgorithm();
		alg.set = set1;
		alg.mixture = mixture;
		alg.nIterations = 10;
		
		env.setDebugLevel(1);
		alg.run(env);
		
		MarkovMixture newMixture = alg.mixture;
		checkSanity(newMixture);
		
		assertNotEquals(0.5, newMixture.weight(0), 1e-6);
		double logP = mixture.estimate(set1), newLogP = newMixture.estimate(set1);
		System.out.format("logP(init) = %.0f\n", logP);
		System.out.format("logP(final) = %.0f\n", newLogP);
		assertTrue(newLogP - logP > 50000.0);
	}
	
	@Test
	public void testIncEMAlgorithm() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set1 = env.loadSet(SET_1);
		
		MarkovMixture mixture = new MarkovMixture(1, 6, set1);
		mixture.model(0).train(set1);
		
		IncrementalEMAlgorithm alg = new IncrementalEMAlgorithm();
		alg.set = set1;
		alg.mixture = mixture;
		alg.nIterations = 10;
		alg.maxModels = 3;
		
		env.setDebugLevel(1);
		alg.run(env);
		
		MarkovMixture newMixture = alg.mixture;
		assertEquals(3, newMixture.size());
		checkSanity(newMixture);
		
		double logP = mixture.estimate(set1), newLogP = newMixture.estimate(set1);
		System.out.format("logP(init) = %.0f\n", logP);
		System.out.format("logP(final) = %.0f\n", newLogP);
		assertTrue(newLogP - logP > 50000.0);
	}
	
	@Test
	public void testDecEMAlgorithm() {
		fail("Not implemented");
	}
	
	@Test
	public void testMixtureAlgorithm() {
		fail("Not implemented");
	}
}

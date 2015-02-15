package ua.kiev.icyb.bio.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.SimpleSequenceSet;
import ua.kiev.icyb.bio.alg.tree.ContentPartitionRule;
import ua.kiev.icyb.bio.alg.tree.FragmentSet;
import ua.kiev.icyb.bio.alg.tree.Partition;
import ua.kiev.icyb.bio.alg.tree.PartitionRule;
import ua.kiev.icyb.bio.alg.tree.PartitionRuleTree;
import ua.kiev.icyb.bio.alg.tree.RuleEntropy;
import ua.kiev.icyb.bio.alg.tree.RuleTreeGenerator;

/**
 * Тесты, связанные с алгоритмами с областями компетентности.
 */
public class TreeTests {

	private static final String SET_1 = "elegans-I";
	
	private static final String CONF_FILE = "tests/env.conf";
	
	private static class RandomPartitionRule extends PartitionRule {
		
		private final double p;
		
		public RandomPartitionRule(double p) {
			this.p = p;
		}
		
		@Override
		public boolean test(byte[] seq) {
			return (Math.random() < p);
		}
		
		@Override
		public String toString() {
			return "Random(" + p + ")";
		}
	}
	
	private static byte[] randomSequence(int alphabetSize, int length) {
		byte[] seq = new byte[length];
		for (int i = 0; i < length; i++) {
			seq[i] = (byte) Math.floor(Math.random() * alphabetSize);
		}
		return seq;
	}
	
	/**
	 * Тестирует базовые методы множества цепочек.
	 */
	@Test
	public void testFragmentSet() {
		FragmentSet set = new FragmentSet("ACGT", 1);
		set.add(1);
		set.add(2);
		assertEquals("C,G", set.toString());
		
		List<String> strings = new ArrayList<String>();
		strings.add("A");
		strings.add("T");
		set = new FragmentSet("ACGT", strings);
		assertEquals(2, set.size());
		assertTrue(set.contains(0));
		assertTrue(set.contains(3));
	}
	
	/**
	 * Тестирует поиск дополнения к множеству.
	 */
	@Test
	public void testFragmentSetComplementary() {
		FragmentSet set = new FragmentSet("ACGT", 1);
		set.add(1);
		set.add(2);
		assertEquals("C,G", set.toString());
		
		set = set.complmentary();
		assertEquals("A,T", set.toString());
	}
	
	/**
	 * Тестирует поиск подмножеств множества цепочек.
	 */
	@Test
	public void testFragmentSetSubsets() {
		FragmentSet set = new FragmentSet("ACGT", 1);
		set.add(1);
		set.add(2); // set ~ { C, G }
		
		Collection<FragmentSet> subsets = set.subsets();
		assertEquals(2, subsets.size());
		
		set.add(0); // set ~ { A, C, G }
		subsets = set.subsets();
		assertEquals(6, subsets.size());
		
		set.add(3); // set ~ { A, C, G, T }
		subsets = set.subsets();
		assertEquals(7, subsets.size());
		for (FragmentSet subset : subsets) {
			assertTrue(subset.size() <= 2);
		}
	}
	
	/**
	 * Тестирует вычисление концентрации цепочек в строке.
	 */
	@Test
	public void testFragmentSetContent() {
		FragmentSet frag = new FragmentSet("ACGT", 1);
		frag.add(1);
		frag.add(2); // frag ~ { C, G }
		
		byte[] seq = new byte[] { 1, 3, 2, 1, 2, 0, 0 };
		assertEquals(4.0 / 7, frag.content(seq), 1e-6);
		seq = new byte[] { 0, 3, 2, 1, 2, 0, 0 };
		assertEquals(3.0 / 7, frag.content(seq), 1e-6);
		seq = new byte[] { 0, 0, 3, 0, 0 };
		assertEquals(0.0, frag.content(seq), 1e-6);
		seq = new byte[] { 0, 0, 0, 0, 0, 1 };
		assertEquals(1.0 / 6, frag.content(seq), 1e-6);
	}
	
	/**
	 * Тестирует вычисление медианной концентрации множества цепочек в наборе строк.
	 */
	@Test
	public void testFragmentSetMedianContent() throws IOException {
		FragmentSet frag = new FragmentSet("ACGT", 1);
		frag.add(1);
		frag.add(2); // frag ~ { C, G }
		
		SimpleSequenceSet set = new SimpleSequenceSet("ACGT", "xi", null);
		byte[] seq = new byte[] { 1, 3, 2, 1, 2, 0, 0 };
		set.add(new Sequence(seq, null));
		seq = new byte[] { 0, 0, 3, 0, 0 };
		set.add(new Sequence(seq, null));
		seq = new byte[] { 0, 0, 0, 0, 0, 1 };
		set.add(new Sequence(seq, null));
		
		double content = frag.medianContent(set);
		assertEquals(1.0 / 6, content, 1e-6);
		
		seq = new byte[] { 0, 0, 2, 0, 0, 1 };
		set.add(new Sequence(seq, null));
		
		content = frag.medianContent(set);
		assertEquals(1.0 / 3, content, 1e-6);
	}
	
	/**
	 * Тестирует базовые методы предиката на основе концентрации множества наблюдаемых состояний. 
	 */
	@Test
	public void testContentPartitionRule() {
		FragmentSet set = new FragmentSet("ACGT", 1);
		set.add(1);
		set.add(2); // set ~ { C, G }
		
		PartitionRule rule = new ContentPartitionRule(set, 0.5);
		byte[] seq = new byte[] { 1, 3, 2, 1, 2, 0, 0 };
		assertTrue(rule.test(seq));
		seq = new byte[] { 0, 3, 2, 1, 2, 0, 0 };
		assertFalse(rule.test(seq));
		rule = new ContentPartitionRule(set, 0.3);
		assertTrue(rule.test(seq));
	}
	
	/**
	 * Тестирует разбиение выборки предикатом.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testContentPartitionRuleSplit() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		FragmentSet frag = new FragmentSet("ACGT", 1);
		frag.add(1);
		frag.add(2); // frag ~ { C, G }
		
		PartitionRule rule = new ContentPartitionRule(frag, 0.5);
		SequenceSet[] parts = rule.split(set);
		assertEquals(2, parts.length);
		assertEquals(set.size(), parts[0].size() + parts[1].size());
		
		for (Sequence seq : parts[0]) {
			assertTrue(frag.content(seq.observed) > 0.5);
		}
		for (Sequence seq : parts[1]) {
			assertTrue(frag.content(seq.observed) <= 0.5);
		}
	}
	
	/**
	 * Тестирует базовые методы дерева предикатов.
	 */
	@Test
	public void testParititionRuleTree() {
		PartitionRuleTree tree = new PartitionRuleTree();
		assertEquals(1, tree.size());
		
		PartitionRule rule1 = new RandomPartitionRule(0.5);
		tree.add(rule1, 0);
		assertEquals(2, tree.size());
		assertSame(rule1, tree.rule(0));
		
		PartitionRule rule2 = new RandomPartitionRule(0.5);
		tree.add(rule2, 0);
		assertEquals(3, tree.size());
		assertSame(rule1, tree.rule(0));
		assertSame(rule2, tree.rule(1));
		
		tree.trim(1);
		assertEquals(2, tree.size());
		assertSame(rule1, tree.rule(0));
	}
	
	/**
	 * Тестирует поиск компонент разбиения.
	 */
	@Test
	public void testParititionRuleTreePartitions() {
		final FragmentSet frag = new FragmentSet("ACGT", 1);
		frag.add(1);
		frag.add(2); // frag ~ {c, g}
		
		PartitionRuleTree tree = new PartitionRuleTree();
		PartitionRule rule1 = new ContentPartitionRule(frag, 0.475);
		tree.add(rule1, 0);
		PartitionRule rule2 = new ContentPartitionRule(frag, 0.525);
		tree.add(rule2, 1);
		
		List<Partition> leaves = tree.leaves();
		assertEquals(tree.size(), leaves.size());
		
		int[] counts = new int[leaves.size()];
		for (Partition part : leaves) {
			counts[part.index()]++;
		}
		for (int i = 0; i < counts.length; i++) {
			assertEquals(1, counts[i]);
		}
		
		final int nSamples = 10000;
		for (int i = 0; i < nSamples; i++) {
			byte[] seq = randomSequence(4, 100);
			int partIdx = tree.getPart(seq);
			
			for (Partition part : leaves) {
				assertEquals(partIdx == part.index(), part.rule().test(seq));
			}
		}
	}
	
	/**
	 * Тестирует метод {@link PartitionRuleTree#getPart(byte[])}.
	 */
	@Test
	public void testParititionRuleTreeGetPart() {
		PartitionRuleTree tree = new PartitionRuleTree();
		assertEquals(1, tree.size());
		
		PartitionRule rule1 = new RandomPartitionRule(0.6);
		tree.add(rule1, 0);
		PartitionRule rule2 = new RandomPartitionRule(0.3);
		tree.add(rule2, 0);
		
		byte[] seq = new byte[] { 1, 2, 3 };
		int[] counts = new int[tree.size()];
		final int nSamples = 100000;
		
		for (int i = 0; i < nSamples; i++) {
			counts[tree.getPart(seq)]++;
		}
		
		final double dev = 3.0 / Math.sqrt(nSamples);
		assertEquals((1 - 0.6) * (1 - 0.3), 1.0 * counts[0] / nSamples, dev);
		assertEquals(0.6, 1.0 * counts[1] / nSamples, dev);
		assertEquals((1 - 0.6) * 0.3, 1.0 * counts[2] / nSamples, dev);
	}
	
	/**
	 * Тестирует уменьшение размера дерева разбиения.
	 */
	@Test
	public void testPartitionRuleTreeTrim() {
		PartitionRuleTree tree = new PartitionRuleTree();
		
		PartitionRule rule1 = new RandomPartitionRule(0.6);
		tree.add(rule1, 0);
		PartitionRule rule2 = new RandomPartitionRule(0.3);
		tree.add(rule2, 0);
		
		assertEquals(3, tree.size());
		
		tree.trim(5);
		assertEquals(3, tree.size());
		assertSame(rule1, tree.rule(0));
		assertSame(rule2, tree.rule(1));
		
		tree.trim(2);
		assertEquals(3, tree.size());
		assertSame(rule1, tree.rule(0));
		assertSame(rule2, tree.rule(1));
		
		tree.trim(1);
		assertEquals(2, tree.size());
		assertSame(rule1, tree.rule(0));
		
		byte[] seq = new byte[] { 1, 2, 3 };
		int[] counts = new int[tree.size()];
		final int nSamples = 100000;
		
		for (int i = 0; i < nSamples; i++) {
			counts[tree.getPart(seq)]++;
		}
		
		final double dev = 3.0 / Math.sqrt(nSamples);
		assertEquals(0.4, 1.0 * counts[0] / nSamples, dev);
		assertEquals(0.6, 1.0 * counts[1] / nSamples, dev);
	}
	
	/**
	 * Тестирует разбиение выборки с помощью дерева предикатов.
	 */
	@Test
	public void testParititionRuleTreeSplit() {
		PartitionRuleTree tree = new PartitionRuleTree();
		assertEquals(1, tree.size());
		
		PartitionRule rule1 = new RandomPartitionRule(0.6);
		tree.add(rule1, 0);
		PartitionRule rule2 = new RandomPartitionRule(0.3);
		tree.add(rule2, 0);
				
		byte[] seq = new byte[] { 1, 2, 3 };
		final int nSamples = 100000;
		SimpleSequenceSet set = new SimpleSequenceSet("ACGT", "xi", null);
		for (int i = 0; i < nSamples; i++) {
			set.add(new Sequence(seq, null));
		}
		
		SequenceSet[] parts = tree.split(set);
		
		final double dev = 3.0 / Math.sqrt(nSamples);
		assertEquals((1 - 0.6) * (1 - 0.3), 1.0 * parts[0].size() / nSamples, dev);
		assertEquals(0.6, 1.0 * parts[1].size() / nSamples, dev);
		assertEquals((1 - 0.6) * 0.3, 1.0 * parts[2].size() / nSamples, dev);
	}
	
	/**
	 * Тестирует вычисление информационной энтропии.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testEntropy() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		RuleEntropy entropy = new RuleEntropy(set, 5);
		
		PartitionRule falseRule = new RandomPartitionRule(-1.0);
		assertEquals(0.0, entropy.fitness(falseRule), 1e-6);
		PartitionRule trueRule = new RandomPartitionRule(2.0);
		assertEquals(0.0, entropy.fitness(trueRule), 1e-6);
		
		PartitionRule rule = new RandomPartitionRule(0.5);
		boolean[] selector = rule.test(set);
		double H = entropy.fitness(set.filter(selector));
		
		for (int i = 0; i < selector.length; i++) {
			selector[i] = !selector[i];
		}
		double otherH = entropy.fitness(set.filter(selector));
		assertEquals(H, otherH, 1e-6);
	}
	
	@Test
	public void testFitnessForSubsets() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		RuleEntropy entropy = new RuleEntropy(set, 5);
		
		PartitionRule rule = new RandomPartitionRule(0.6);
		boolean[] selector = rule.test(set);
		
		SequenceSet subset1 = set.filter(selector);
		for (int i = 0; i < selector.length; i++) {
			selector[i] = !selector[i];
		}
		SequenceSet subset2 = set.filter(selector);
		
		double H = entropy.fitness(subset1);
		double otherH = entropy.fitness(subset1, subset2);
		assertEquals(H, otherH, 0.01);
	}
	
	/**
	 * Тестирует унимодальность функционала качества разбиения для семейства предикатов,
	 * различающихся пороговым значением концентрации. 
	 * 
	 * @throws IOException
	 */
	@Test
	public void testEntropyUnimodality() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		RuleEntropy entropy = new RuleEntropy(set, 5);
		final FragmentSet frag = new FragmentSet(set.observedStates(), 1);
		frag.add(1);
		frag.add(2); // frag ~ {c, g}
		
		int i = 0, maxI = -1;
		double[] fitness = new double[61];
		double maxFitness = 0.0;
		for (double t = 0.2; t < 0.8; t += 0.01) {
			PartitionRule rule = new ContentPartitionRule(frag, t);
			fitness[i] = entropy.fitness(rule);
			if (fitness[i] > maxFitness) {
				maxI = i;
				maxFitness = fitness[i];
			}
			i++;
		}
		
		assertTrue(maxI > 5);
		assertTrue(maxI < fitness.length - 5);
		
		for (i = 0; i < maxI; i++) {
			assertTrue(fitness[i] <= fitness[i + 1]);
		}
		for (i = maxI + 1; i < fitness.length; i++) {
			assertTrue(fitness[i - 1] >= fitness[i]);
		}
	}
	
	/**
	 * Тестирует генерирование дерева разбиения на основе выборки.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testTreeGen() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		final FragmentSet frag = new FragmentSet(set.observedStates(), 1);
		frag.add(1);
		frag.add(2);
		
		RuleTreeGenerator alg = new RuleTreeGenerator();
		alg.set = set;
		alg.percentages = new double[] { 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7 };
		alg.baseSets = Collections.singleton(frag);
		alg.order = 5;
		alg.minPartSize = 10;
		alg.treeSize = 3;
		
		env.run(alg);
		
		assertEquals(alg.treeSize + 1, alg.tree.size());
		
		int count = 0;
		for (RuleTreeGenerator.Fitness f : alg.computedFitness) {
			if (f.isOptimal) count++;
		}
		assertEquals(2 * alg.treeSize - 1, count);
		
		count = 0;
		double fitness = 0.0;
		for (RuleTreeGenerator.Fitness f : alg.computedFitness) {
			if (f.isGloballyOptimal) {
				count++;
				fitness += f.fitness;
			}
		}
		assertEquals(alg.treeSize, count);
		
		RuleEntropy entropy = new RuleEntropy(set, 5);
		assertEquals(entropy.fitness(alg.tree), fitness, 0.1);
	}
}

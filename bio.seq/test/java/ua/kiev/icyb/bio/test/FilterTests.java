package ua.kiev.icyb.bio.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.SimpleSequenceSet;
import ua.kiev.icyb.bio.StatesDescription;
import ua.kiev.icyb.bio.Transform;
import ua.kiev.icyb.bio.alg.Fragment;
import ua.kiev.icyb.bio.alg.MarkovChain;
import ua.kiev.icyb.bio.filters.LabelFilter;
import ua.kiev.icyb.bio.filters.LengthFilter;
import ua.kiev.icyb.bio.filters.MappingTransform;
import ua.kiev.icyb.bio.filters.PeriodicTransform;
import ua.kiev.icyb.bio.filters.RandomFilter;
import ua.kiev.icyb.bio.filters.TerminalTransform;
import ua.kiev.icyb.bio.filters.TransformComposition;
import ua.kiev.icyb.bio.filters.ValidGenesFilter;

/**
 * Тесты, сязанные с фильтрами и преобразованиями выборок.
 */
public class FilterTests {
	
	private static Env env;
	
	private static SequenceSet set1;
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	@BeforeClass
	public static void setup() throws IOException {
		final String testDir = System.getProperty("testdir", "test");
		env = new Env(testDir + "/env.conf");
		set1 = env.loadSet("elegans-I");
	}
	
	/**
	 * Проверка базовой функциональности фильтра длины.
	 */
	@Test
	public void testLengthFilter() {
		final SequenceSet set = set1;
		final int maxLen = 1000;
		
		SequenceSet filtered = set.filter(new LengthFilter(maxLen));
		for (Sequence seq : filtered) {
			assertTrue(set.contains(seq));
			assertTrue(seq.length() <= maxLen);
		}
		
		for (Sequence seq : set) {
			if (!filtered.contains(seq)) {
				assertTrue(seq.length() > maxLen);
			}
		}
	}
	
	/**
	 * Проверка функциональности фильтра длины для двустороннего ограничения длины строк.
	 */
	@Test
	public void testLengthFilterBand() {
		final SequenceSet set = set1;
		final int minLen = 500, maxLen = 2000;
		
		SequenceSet filtered = set.filter(new LengthFilter(minLen, maxLen));
		for (Sequence seq : filtered) {
			assertTrue(set.contains(seq));
			assertTrue(seq.length() >= minLen);
			assertTrue(seq.length() <= maxLen);
		}
		
		for (Sequence seq : set) {
			if (!filtered.contains(seq)) {
				assertTrue((seq.length() < minLen) || (seq.length() > maxLen));
			}
		}
	}
	
	/**
	 * Проверка граничных значений для фильтра длины.
	 */
	@Test
	public void testLengthFilterExact() {
		final SequenceSet set = set1;
		final int len = set.get(0).length();
		
		SequenceSet filtered = set.filter(new LengthFilter(len));
		assertTrue(filtered.contains(set.get(0)));
		filtered = set.filter(new LengthFilter(len - 1));
		assertFalse(filtered.contains(set.get(0)));
		filtered = set.filter(new LengthFilter(len + 1));
		assertTrue(filtered.contains(set.get(0)));
		
		filtered = set.filter(new LengthFilter(len, Integer.MAX_VALUE));
		assertTrue(filtered.contains(set.get(0)));
		filtered = set.filter(new LengthFilter(len - 1, Integer.MAX_VALUE));
		assertTrue(filtered.contains(set.get(0)));
		filtered = set.filter(new LengthFilter(len + 1, Integer.MAX_VALUE));
		assertFalse(filtered.contains(set.get(0)));
	}
	
	/**
	 * Проверка граничных значений для фильтра рандомизации.
	 */
	@Test
	public void testRandomFilterMargins() {
		final SequenceSet set = set1;
		SequenceSet filtered = set.filter(new RandomFilter(0.0));
		assertTrue(filtered.isEmpty());
		filtered = set.filter(new RandomFilter(-1.0));
		assertTrue(filtered.isEmpty());
		filtered = set.filter(new RandomFilter(1.0));
		assertEquals(set.size(), filtered.size());
		filtered = set.filter(new RandomFilter(2.0));
		assertEquals(set.size(), filtered.size());
	}
	
	/**
	 * Проверка рандомизированности для фильтра рандомизации.
	 */
	@Test
	public void testRandomFilterRandomness() {
		final SequenceSet set = set1;
		final double p = 0.4;
		
		SequenceSet filtered = set.filter(new RandomFilter(p));
		SequenceSet filtered2 = set.filter(new RandomFilter(p));
		assertFalse((filtered.size() == filtered2.size()) && filtered.containsAll(filtered2));
	}
	
	/**
	 * Проверка покрытия выборки фильтром рандомизации.
	 */
	@Test
	public void testRandomFilterCoverage() {
		final SequenceSet set = set1;
		Set<Sequence> inFiltered = new HashSet<Sequence>();
		
		final int nRuns = 50;
		final RandomFilter filter = new RandomFilter(0.2); 
		for (int i = 0; i < nRuns; i++) {
			SequenceSet filtered = set.filter(filter);
			inFiltered.addAll(filtered);
		}
		
		assertEquals(set.size(), inFiltered.size());
	}
	
	/**
	 * Проверка размера выборки при использовании фильтра рандомизации.
	 */
	@Test
	public void testRandomFilterSetSize() {
		final SequenceSet set = set1;
		
		for (double p = 0.1; p <= 0.9; p += 0.1) {
			SequenceSet filtered = set.filter(new RandomFilter(p));
			double dev = Math.abs(filtered.size() - p * set.size());
			assertTrue(dev < 3 * Math.sqrt(set.size()));
		}
	}
	
	/**
	 * Проверка граничных значений для фильтра меток.
	 */
	@Test
	public void testLabelFilterMarginCases() {
		final SequenceSet set = set1;
		Map<String, Integer> labelMap = new HashMap<String, Integer>();
		for (Sequence seq : set) {
			labelMap.put(seq.id, 1);
		}
		
		LabelFilter filter = new LabelFilter(labelMap, 0);
		SequenceSet filtered = set.filter(filter);
		assertTrue(filtered.isEmpty());
		filter = new LabelFilter(labelMap, 1);
		filtered = set.filter(filter);
		assertEquals(set.size(), filtered.size());
	}
	
	/**
	 * Проверка функциональности фильтра меток с одной активной меткой.
	 */
	@Test
	public void testLabelFilterSingleMark() {
		final SequenceSet set = set1;
		Map<String, Integer> labelMap = new HashMap<String, Integer>();
		int i = 0;
		for (Sequence seq : set) {
			labelMap.put(seq.id, i);
			i = 1 - i;
		}
		
		LabelFilter filter = new LabelFilter(labelMap, 1);
		SequenceSet filtered = set.filter(filter);
		assertEquals(set.size() / 2, filtered.size());
		for (i = 1; i < set.size(); i += 2) {
			assertTrue(filtered.contains(set.get(i)));
		}
	}
	
	/**
	 * Проверка функциональности фильтра меток при нескольких запусках.
	 */
	@Test
	public void testLabelFilterMultipleRuns() {
		final SequenceSet set = set1;
		Map<String, Integer> labelMap = new HashMap<String, Integer>();
		int i = 0;
		for (Sequence seq : set) {
			labelMap.put(seq.id, i);
			i = 1 - i;
		}
		
		LabelFilter filter = new LabelFilter(labelMap, 1);
		SequenceSet filtered = set.filter(filter);
		assertEquals(set.size() / 2, filtered.size());
		filtered = filtered.filter(filter);
		assertEquals(set.size() / 2, filtered.size());
	}
	
	/**
	 * Проверка граничных значений для фильтра меток при выборе нескольких меток.
	 */
	@Test
	public void testLabelFilterMarginCases_MarkSets() {
		final SequenceSet set = set1;
		Map<String, Integer> labelMap = new HashMap<String, Integer>();
		int i = 0;
		for (Sequence seq : set) {
			labelMap.put(seq.id, i);
			i = 1 - i;
		}
		
		Set<Integer> marks = new HashSet<Integer>();
		LabelFilter filter = new LabelFilter(labelMap, marks);
		SequenceSet filtered = set.filter(filter);
		assertTrue(filtered.isEmpty());
		
		marks.add(0); marks.add(1);
		filtered = set.filter(filter);
		// Проверить, не сохраняется ли множество по ссылке
		assertTrue(filtered.isEmpty());
		
		filter = new LabelFilter(labelMap, marks);
		filtered = set.filter(filter);
		assertEquals(set.size(), filtered.size());
	}
	
	/**
	 * Проверка функциональности фильтра меток при выборе нескольких меток.
	 */
	@Test
	public void testLabelFilterMarkSets() {
		final SequenceSet set = set1;
		Map<String, Integer> labelMap = new HashMap<String, Integer>();
		for (int i = 0; i < set.size(); i++) {
			labelMap.put(set.get(i).id, i % 4);
		}
		
		Set<Integer> marks = new HashSet<Integer>();
		marks.add(0);
		marks.add(2);
		LabelFilter filter = new LabelFilter(labelMap, marks);
		SequenceSet filtered = set.filter(filter);
		for (int i = 0; i < set.size(); i++) {
			if ((i % 4 == 0) || (i % 4 == 2)) {
				assertTrue(filtered.contains(set.get(i)));
			} else {
				assertFalse(filtered.contains(set.get(i)));
			}
		}
	}
	
	/**
	 * Проверка функциональности фильтра корректных генов.
	 */
	@Test
	public void testValidGenesFilter() {
		StatesDescription states = StatesDescription.create("ACGT", "xi", "ACGTacgt");
		
		ValidGenesFilter filter = new ValidGenesFilter(false);
		Sequence seq = Sequence.parse(states, "ATGTAA");
		assertTrue(filter.eval(seq));
		seq = Sequence.parse(states, "ATGAA");
		assertFalse(filter.eval(seq));
		seq = Sequence.parse(states, "ATGtaG");
		assertTrue(filter.eval(seq));
		seq = Sequence.parse(states, "ATTGtaG");
		assertFalse(filter.eval(seq));
		seq = Sequence.parse(states, "ATgacgtagtga");
		assertTrue(filter.eval(seq));
	}
	
	/**
	 * Проверка функциональности фильтра корректных генов при использовании особых множеств наблюдаемых состояний.
	 */
	@Test
	public void testValidGenesFilterWithCustomStates() {
		StatesDescription states = StatesDescription.create("CGNAT", "xi", "CGNATcgnat");
		
		ValidGenesFilter filter = new ValidGenesFilter(false);
		Sequence seq = Sequence.parse(states, "ATGTAA");
		assertTrue(filter.eval(seq));
		seq = Sequence.parse(states, "ATGAA");
		assertFalse(filter.eval(seq));
		seq = Sequence.parse(states, "ATGtaG");
		assertTrue(filter.eval(seq));
		seq = Sequence.parse(states, "ATTGtaG");
		assertFalse(filter.eval(seq));
		seq = Sequence.parse(states, "ATgacgtagtga");
		assertTrue(filter.eval(seq));
		seq = Sequence.parse(states, "ATGnNnTAG");
		assertTrue(filter.eval(seq));
		seq = Sequence.parse(states, "AnGTAA");
		assertFalse(filter.eval(seq));
	}
	
	/**
	 * Проверка фильтра корректных генов при проверке интронов.
	 */
	@Test
	public void testValidGenesFilterIntrons() {
		StatesDescription states = StatesDescription.create("ACGT", "xi", "ACGTacgt");
		
		ValidGenesFilter filter = new ValidGenesFilter(true);
		Sequence seq = Sequence.parse(states, "ATGTAA");
		assertTrue(filter.eval(seq));
		seq = Sequence.parse(states, "ATGAA");
		assertFalse(filter.eval(seq));
		seq = Sequence.parse(states, "ATGtaG");
		assertFalse(filter.eval(seq));
		seq = Sequence.parse(states, "ATTGtaG");
		assertFalse(filter.eval(seq));
		
		seq = Sequence.parse(states, "ATGgtagTGA");
		assertTrue(filter.eval(seq));
		seq = Sequence.parse(states, "ATgtagTGA");
		assertTrue(filter.eval(seq));
		seq = Sequence.parse(states, "ATGgtgTGA");
		assertFalse(filter.eval(seq));
		
		seq = Sequence.parse(states, "ATGgttttagTgtaaagTGA");
		assertTrue(filter.eval(seq));
		seq = Sequence.parse(states, "ATGgtctcagTGtaaagTGA");
		assertFalse(filter.eval(seq));
	}
	
	/**
	 * Проверка фильтра корректных генов на реальных данных. 
	 */
	@Test
	public void testValidGenesFilterRealData() {
		final SequenceSet set = set1;
		SequenceSet filtered = set.filter(new ValidGenesFilter(false));
		assertTrue(filtered.size() < set.size());
		assertTrue(filtered.size() > 0.9 * set.size());
		SequenceSet filtered2 = set.filter(new ValidGenesFilter(true));
		assertTrue(filtered2.size() < set.size());
		assertTrue(filtered2.size() > 0.9 * set.size());
		assertTrue(filtered2.size() < filtered.size());
	}
	
	/**
	 * Проверка фильтра корректных генов с несколькими запусками.
	 */
	@Test
	public void testValidGenesFilterMultipleRuns() {
		final SequenceSet set = set1;
		SequenceSet filtered = set.filter(new ValidGenesFilter(false));
		SequenceSet filtered2 = filtered.filter(new ValidGenesFilter(false));
		assertEquals(filtered, filtered2);
		
		filtered2 = filtered.filter(new ValidGenesFilter(true));
		filtered = set.filter(new ValidGenesFilter(true));
		assertEquals(filtered, filtered2);
	}
	
	private static class DummyTransform implements Transform, Serializable {

		private static final long serialVersionUID = 1L;	

		@Override
		public StatesDescription states(StatesDescription original) {
			return original;
		}

		@Override
		public Sequence sequence(Sequence original) {
			return new Sequence(original.id + "!", original.observed, original.hidden);
		}

		@Override
		public Sequence inverse(Sequence transformed) {
			return transformed;
		}
	}
	
	private static byte[] flip(byte[] sequence) {
		byte[] flipped = new byte[sequence.length];
		for (int i = 0; i < sequence.length; i++) {
			flipped[i] = (byte) (1 - sequence[i]);
		}
		return flipped;
	}
	
	private static class FlipTransform implements Transform, Serializable {

		private static final long serialVersionUID = 1L;	

		@Override
		public StatesDescription states(StatesDescription original) {
			return original;
		}

		@Override
		public Sequence sequence(Sequence original) {
			return new Sequence(original.id + "!", original.observed, flip(original.hidden));
		}
		
		@Override
		public Sequence inverse(Sequence transformed) {
			return new Sequence(transformed.observed, flip(transformed.hidden));
		}
	}
	
	@Test
	public void testDummyTransform() {
		final SequenceSet set = set1;
		SequenceSet transformed = set.transform(new DummyTransform());
		assertEquals(set.size(), transformed.size());
		
		for (int i = 0; i < set.size(); i++) {
			assertArrayEquals(set.get(i).observed, transformed.get(i).observed);
			assertArrayEquals(set.get(i).hidden, transformed.get(i).hidden);
		}
	}
	
	@Test
	public void testFlipTransform() {
		final SequenceSet set = set1;
		SequenceSet transformed = set.transform(new FlipTransform());
		assertEquals(set.size(), transformed.size());
		
		for (int i = 0; i < set.size(); i++) {
			assertArrayEquals(set.get(i).observed, transformed.get(i).observed);
			assertArrayEquals(flip(set.get(i).hidden), transformed.get(i).hidden);
		}
	}
	
	@Test
	public void testSerializationOnTransform() throws IOException {
		File file = tempFolder.newFile();
		final SequenceSet set = set1.transform(new DummyTransform());
		
		env.save(set, file.getAbsolutePath());
		
		assertTrue(file.isFile());
		assertTrue(file.length() < 1000);
		
		SequenceSet copy = env.load(file.getAbsolutePath());
		assertEquals(set.size(), copy.size());
		for (int i = 0; i < set.size(); i++) {
			assertArrayEquals(set.observed(i), copy.observed(i));
			assertArrayEquals(set.hidden(i), copy.hidden(i));
		}
	}
	
	@Test
	public void testSerializationOnTransform_Flip() throws IOException {
		File file = tempFolder.newFile();
		final SequenceSet set = set1.transform(new FlipTransform());
		
		env.save(set, file.getAbsolutePath());
		
		assertTrue(file.isFile());
		assertTrue(file.length() < 1000);
		
		SequenceSet copy = env.load(file.getAbsolutePath());
		assertEquals(set.size(), copy.size());
		for (int i = 0; i < set.size(); i++) {
			assertArrayEquals(set.observed(i), copy.observed(i));
			assertArrayEquals(set.hidden(i), copy.hidden(i));
		}
	}
	
	@Test
	public void testTailTransform() {
		Transform transform = new TerminalTransform();
		StatesDescription states = StatesDescription.create("ACGT", "xi", "ACGTacgt");
		StatesDescription tStates = transform.states(states);
		
		assertEquals("ACGT$", tStates.observed());
		assertEquals("xi", tStates.hidden());
		assertEquals("ACGT$acgt$", tStates.complete());
		
		SimpleSequenceSet set = new SimpleSequenceSet(states);
		set.add(Sequence.parse(states, "ACGttt"));
		Sequence seq = set.get(0);
		Sequence tSeq = transform.sequence(seq);
		assertEquals(seq.length() + 1, tSeq.length());
		assertEquals(seq.length() + 1, tSeq.observed.length);
		assertEquals(seq.length() + 1, tSeq.hidden.length);
		for (int i = 0; i < seq.length(); i++) {
			assertEquals(seq.observed[i], tSeq.observed[i]);
			assertEquals(seq.hidden[i], tSeq.hidden[i]);
		}
		assertEquals(4, tSeq.observed[seq.length()]);
		assertEquals(0, tSeq.hidden[seq.length()]);
	}
	
	@Test
	public void testTailTransformOnSet() {
		StatesDescription states = StatesDescription.create("ACGT", "xi", "ACGTacgt");
		SimpleSequenceSet set = new SimpleSequenceSet(states);
		set.add(Sequence.parse(states, "ACGttt"));
		set.add(Sequence.parse(states, "aaTA"));
		
		SequenceSet tSet = set.transform(new TerminalTransform());
		assertEquals(tSet.size(), set.size());
		assertTrue(tSet.get(0).toString().contains("ACGttt$"));
		assertTrue(tSet.get(1).toString().contains("aaTA$"));
	}
	
	@Test
	public void testTailTransformOnRealData() {
		final SequenceSet set = set1;
		SequenceSet tSet = set.transform(new TerminalTransform());
		assertEquals(tSet.size(), set.size());
		
		for (int i = 0; i < set.size(); i++) {
			assertEquals(set.get(i).length() + 1, tSet.get(i).length());
			assertEquals(4, tSet.get(i).observed[set.get(i).length()]);
			assertEquals(0, tSet.get(i).hidden[set.get(i).length()]);
		}
	}
	
	@Test
	public void testTailTransformProbabilities() {
		SequenceSet set = set1;
		set = set.transform(new TerminalTransform());
		
		MarkovChain chain = new MarkovChain(1, 1, set.states());
		chain.train(set);
		Fragment terminal = chain.factory().fragment(set.states().nObserved() - 1, 0, 1);
		for (int obs = 0; obs < set.states().nObserved(); obs++) {
			Fragment frag = chain.factory().fragment(obs, 0, 1);
			if ((obs != 0) && (obs != 2)) { // a, g
				assertEquals(0.0, chain.getTransP(frag, terminal), 1e-6);
			} else {
				assertNotEquals(0.0, chain.getTransP(frag, terminal), 1e-6);
			}
			frag = chain.factory().fragment(obs, 1, 1);
			assertEquals(0.0, chain.getTransP(frag, terminal), 1e-6);
		}
	}
	
	@Test
	public void testPeriodicTransform() {
		Transform transform = new PeriodicTransform();
		StatesDescription states = StatesDescription.create("ACGT", "xi", "ACGTacgt");
		StatesDescription tStates = transform.states(states);
		
		assertEquals("ACGT", tStates.observed());
		assertEquals("xyzijk", tStates.hidden());
		assertNull(tStates.complete());
		
		SimpleSequenceSet set = new SimpleSequenceSet(states);
		set.add(Sequence.parse(states, "ACtttGTAG"));
		Sequence seq = set.get(0);
		Sequence tSeq = transform.sequence(seq);
		assertEquals(seq.length(), tSeq.length());
		assertEquals(seq.length(), tSeq.observed.length);
		assertEquals(seq.length(), tSeq.hidden.length);
		for (int i = 0; i < seq.length(); i++) {
			assertEquals(seq.observed[i], tSeq.observed[i]);
			assertEquals(seq.hidden[i], tSeq.hidden[i] / 3);
		}
		assertEquals(0, tSeq.hidden[0]);
		assertEquals(2, tSeq.hidden[8]);
		
		Sequence invSeq = transform.inverse(tSeq);
		assertArrayEquals(seq.observed, invSeq.observed);
		assertArrayEquals(seq.hidden, invSeq.hidden);
	}
	
	@Test
	public void testPeriodicTransformOnRealData() {
		SequenceSet set = set1.filter(new ValidGenesFilter(true));
		Transform transform = new PeriodicTransform();
		SequenceSet tSet = set.transform(transform);
		
		for (int i = 0; i < set.size(); i++) {
			Sequence seq = set.get(i), tSeq = tSet.get(i);
			assertEquals(seq.length(), tSeq.length());
			assertArrayEquals(seq.observed, tSeq.observed);
			assertEquals(2, tSeq.hidden[tSeq.length() - 1]);
			assertArrayEquals(seq.hidden, transform.inverse(tSeq).hidden);
		}
	}
	
	@Test
	public void testTransformComposition() {
		Transform transform = new TransformComposition(new PeriodicTransform(), new TerminalTransform());
		StatesDescription states = StatesDescription.create("ACGT", "xi", "ACGTacgt");
		StatesDescription tStates = transform.states(states);
		
		assertEquals("ACGT$", tStates.observed());
		assertEquals("xyzijk", tStates.hidden());
		assertNull(tStates.complete());
		
		SimpleSequenceSet set = new SimpleSequenceSet(states);
		set.add(Sequence.parse(states, "ACtttGTAG"));
		Sequence seq = set.get(0);
		Sequence tSeq = transform.sequence(seq);
		assertEquals(seq.length() + 1, tSeq.length());
		assertEquals(seq.length() + 1, tSeq.observed.length);
		assertEquals(seq.length() + 1, tSeq.hidden.length);
		for (int i = 0; i < seq.length(); i++) {
			assertEquals(seq.observed[i], tSeq.observed[i]);
			assertEquals(seq.hidden[i], tSeq.hidden[i] / 3);
		}
		assertEquals(0, tSeq.hidden[0]);
		assertEquals(2, tSeq.hidden[8]);
		assertEquals(0, tSeq.hidden[9]);
		
		Sequence invSeq = transform.inverse(tSeq);
		assertArrayEquals(seq.observed, invSeq.observed);
		assertArrayEquals(seq.hidden, invSeq.hidden);
	}
	
	@Test
	public void testMappingBasics() {
		Map<Character, Character> map = MappingTransform.map("abcdef:bccaff");
		assertEquals(6, map.size());
		assertEquals('b', (char) map.get('a'));
		assertEquals('c', (char) map.get('b'));
		assertEquals('c', (char) map.get('c'));
		assertEquals('a', (char) map.get('d'));
		assertEquals('f', (char) map.get('e'));
		assertEquals('f', (char) map.get('f'));
	}
	
	@Test
	public void testMappingTransform() {
		Map<Character, Character> map = MappingTransform.map("CGT:ATT");
		Transform transform = new MappingTransform(map, Collections.<Character, Character> emptyMap());
		StatesDescription states = StatesDescription.create("ACGTN", "xi", "ACGTNacgtn");
		StatesDescription tStates = transform.states(states);
		
		assertEquals("ATN", tStates.observed());
		assertEquals("xi", tStates.hidden());
		assertNull(tStates.complete());
		
		Sequence seq = Sequence.parse(states, "ACGTatg");
		Sequence tSeq = transform.sequence(seq);
		assertEquals(seq.length(), tSeq.length());
		assertArrayEquals(seq.hidden, tSeq.hidden);
		for (int i = 0; i < seq.length(); i++) {
			if ((seq.observed[i] == 0) || (seq.observed[i] == 1)) {
				assertEquals(0, tSeq.observed[i]);
			} else {
				assertEquals(1, tSeq.observed[i]);
			}
		}
	}
	
	@Test
	public void testMappingTransformOnRealData() throws IOException {
		SequenceSet proteins = env.loadSet("prot");
		
		Map<Character, Character> map = MappingTransform.map("-TSGHIEB:---HHHSS");
		Transform transform = new MappingTransform(MappingTransform.TRIVIAL_MAP, map);
		
		proteins = proteins.transform(transform);
		int[] stateStats = new int[3];
		int len = 0;
		for (Sequence seq : proteins) {
			for (int pos = 0; pos < seq.length(); pos++) {
				stateStats[seq.hidden[pos]]++;
			}
			len += seq.length();
		}
		for (int i = 0; i < 3; i++) {
			assertTrue(stateStats[i] > 0.1 * len);
			assertTrue(stateStats[i] < 0.5 * len);
		}
	}
}

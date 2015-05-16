package ua.kiev.icyb.bio.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.SequenceUtils;
import ua.kiev.icyb.bio.SimpleSequenceSet;
import ua.kiev.icyb.bio.filters.LabelFilter;
import ua.kiev.icyb.bio.filters.LengthFilter;
import ua.kiev.icyb.bio.filters.RandomFilter;
import ua.kiev.icyb.bio.filters.ValidGenesFilter;

/**
 * Тесты, сязанные с фильтрами выборок.
 */
public class FilterTests {
	
	private static Env env;
	
	private static SequenceSet set1;
	
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
		SequenceSet dummySet = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		
		ValidGenesFilter filter = new ValidGenesFilter(false);
		Sequence seq = SequenceUtils.parseSequence(dummySet, "ATGTAA");
		assertTrue(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATGAA");
		assertFalse(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATGtaG");
		assertTrue(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATTGtaG");
		assertFalse(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATgacgtagtga");
		assertTrue(filter.eval(seq));
	}
	
	/**
	 * Проверка функциональности фильтра корректных генов при использовании особых множеств наблюдаемых состояний.
	 */
	@Test
	public void testValidGenesFilterWithCustomStates() {
		SequenceSet dummySet = new SimpleSequenceSet("CGNAT", "xi", "CGNATcgnat");
		
		ValidGenesFilter filter = new ValidGenesFilter(false);
		Sequence seq = SequenceUtils.parseSequence(dummySet, "ATGTAA");
		assertTrue(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATGAA");
		assertFalse(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATGtaG");
		assertTrue(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATTGtaG");
		assertFalse(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATgacgtagtga");
		assertTrue(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATGnNnTAG");
		assertTrue(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "AnGTAA");
		assertFalse(filter.eval(seq));
	}
	
	/**
	 * Проверка фильтра корректных генов при проверке интронов.
	 */
	@Test
	public void testValidGenesFilterIntrons() {
		SequenceSet dummySet = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		
		ValidGenesFilter filter = new ValidGenesFilter(true);
		Sequence seq = SequenceUtils.parseSequence(dummySet, "ATGTAA");
		assertTrue(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATGAA");
		assertFalse(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATGtaG");
		assertFalse(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATTGtaG");
		assertFalse(filter.eval(seq));
		
		seq = SequenceUtils.parseSequence(dummySet, "ATGgtagTGA");
		assertTrue(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATgtagTGA");
		assertTrue(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATGgtgTGA");
		assertFalse(filter.eval(seq));
		
		seq = SequenceUtils.parseSequence(dummySet, "ATGgttttagTgtaaagTGA");
		assertTrue(filter.eval(seq));
		seq = SequenceUtils.parseSequence(dummySet, "ATGgtctcagTGtaaagTGA");
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
}

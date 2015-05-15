package ua.kiev.icyb.bio.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.SequenceUtils;
import ua.kiev.icyb.bio.SimpleSequenceSet;
import ua.kiev.icyb.bio.filters.RandomFilter;

/**
 * Тесты, связанные с выборками данных.
 */
public class SetTests {

	private static Env env;
	
	private static SequenceSet set1;
	private static SequenceSet set2;
	
	@BeforeClass
	public static void setup() throws IOException {
		final String testDir = System.getProperty("testdir", "test");
		env = new Env(testDir + "/env.conf");
		set1 = env.loadSet("elegans-I");
		set2 = env.loadSet("elegans-II");
	}
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	/**
	 * Проверяет базовые характеристики выборки данных.
	 * 
	 * @param set
	 *    выборка, которую надо проверить
	 */
	public static void checkSanity(SequenceSet set) {
		for (int i = 0; i < set.size(); i++) {
			assertNotNull(set.id(i));
			assertNotNull(set.observed(i));
			assertNotNull(set.hidden(i));
			
			assertSame(set.get(i).id, set.id(i));
			assertSame(set.get(i).observed, set.observed(i));
			assertSame(set.get(i).hidden, set.hidden(i));
		}
	}
	
	private void checkSubset(SequenceSet set, SequenceSet subset, int offset) {
		for (int i = 0; i < subset.size(); i++) {
			assertSame(subset.id(i), set.id(i + offset));
			assertTrue(subset.observed(i) == set.observed(i + offset));
			assertTrue(subset.hidden(i) == set.hidden(i + offset));
		}
	}
	
	/**
	 * Проверяет единичную выборку данных.
	 */
	@Test
	public void testSingleSet() {
		final SequenceSet set = set1;
		assertEquals("ACGT", set.observedStates());
		assertEquals("xi", set.hiddenStates());
		assertEquals("ACGTacgt", set.completeStates());
		assertTrue(set.size() > 3000);
		
		checkSanity(set);
	}
	
	/**
	 * Проверяет единичную выборку данных.
	 */
	@Test
	public void testIterator() {
		SequenceSet set = set1;
		
		boolean[] selector = new  boolean[set.size()];
		for (int i = 0; i < 10; i++) {
			selector[i] = true;
		}
		set = set.filter(selector);
		
		int i = 0;
		for (Sequence seq : set) {
			assertEquals(set.get(i), seq);
			i++;
		}
	}
	
	/**
	 * Проверяет выборку с возможностью добавления прецедентов.
	 */
	@Test
	public void testMutableSet() {
		SimpleSequenceSet set = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		
		set.add(new Sequence("1", new byte[10], new byte[10]));
		assertEquals(1, set.size());
		assertEquals("1", set.id(0));
		assertEquals(10, set.get(0).length());
		checkSanity(set);
		
		set.add(new Sequence("2", new byte[20], new byte[20]));
		assertEquals(2, set.size());
		assertEquals("1", set.id(0));
		assertEquals("2", set.id(1));
		assertEquals(10, set.get(0).length());
		assertEquals(20, set.get(1).length());
		checkSanity(set);
		
		set.remove(set.get(0));
		assertEquals(1, set.size());
		assertEquals("2", set.id(0));
		assertEquals(20, set.get(0).length());
		checkSanity(set);
	}
	
	/**
	 * Проверяет очистку выборки.
	 */
	@Test
	public void testMutableSetClear() {
		SimpleSequenceSet set = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		
		for (int i = 0; i < 1000; i++) {
			set.add(new Sequence("" + i, new byte[100], new byte[100]));
		}
		
		assertEquals(1000, set.size());
		checkSanity(set);
		for (int i = 0; i < set.size(); i++) {
			assertTrue(set.contains(set.get(i)));
		}
		
		Sequence[] sequences = set.toArray(new Sequence[0]);
		set.clear();
		assertEquals(0, set.size());
		for (Sequence seq : sequences) {
			assertFalse(set.contains(seq));
		}
	}
	
	/**
	 * Проверяет метод {@link SequenceSet#contains(Object)}.
	 */
	@Test
	public void testSetContains() {
		SimpleSequenceSet set = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		Sequence seq = new Sequence("1", new byte[100], new byte[100]);
		set.add(seq);
		assertTrue(set.contains(seq));
		
		Sequence seq2 = new Sequence("1", new byte[50], new byte[50]);
		assertTrue(set.contains(seq2));
		
		seq2 = new Sequence("2", new byte[100], new byte[100]);
		assertFalse(set.contains(seq2));
		
		set.add(seq2);
		assertTrue(set.contains(seq2));
		
		set.remove(0);
		assertFalse(set.contains(seq));
		assertTrue(set.contains(seq2));
	}
	
	/**
	 * Проверяет удаление прецедентов из выборки.
	 */
	@Test
	public void testSetRemove() {
		SimpleSequenceSet set = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		for (int i = 0; i < 100; i++) {
			Sequence seq = new Sequence("" + i, new byte[100], new byte[100]);
			set.add(seq);
		}
		
		Sequence seq = set.remove(10);
		assertEquals(99, set.size());
		assertEquals("10", seq.id);
		
		seq = new Sequence("20", new byte[0], new byte[0]);
		boolean result = set.remove(seq);
		assertTrue(result);
		assertEquals(98, set.size());
		assertFalse(set.contains(seq));
		assertEquals("22", set.get(20).id);
	}
	
	/**
	 * Проверяет удаление прецедентов из выборки с помощью итератора.
	 */
	@Test
	public void testSetIteratorRemove() {
		SimpleSequenceSet set = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		for (int i = 0; i < 100; i++) {
			Sequence seq = new Sequence("" + i, new byte[100], new byte[100]);
			set.add(seq);
		}
		
		int count = 0;
		Iterator<Sequence> iter = set.iterator();
		while (iter.hasNext()) {
			assertEquals("" +  count, iter.next().id);
			if (count % 2 == 0) {
				iter.remove();
			}
			count++;
		}
		
		count = 0;
		for (Sequence seq : set) {
			assertEquals("" + (2 * count + 1), seq.id);
			count++;
		}
	}
	
	@Test(expected = IndexOutOfBoundsException.class)
	public void testSetInvalidIndex() {
		SequenceSet set = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		set.get(0);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testSetUnsupportedAdd() {
		final SequenceSet set = set1;
		set.add(new Sequence("1", new byte[10], new byte[10]));
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testSetUnsupportedRemove() {
		final SequenceSet set = set1;
		set.remove(new Sequence("1", new byte[10], new byte[10]));
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testSetUnsupportedAddAll() {
		final SequenceSet set = set1;
		set.addAll(new ArrayList<Sequence>());
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testSetUnsupportedRemoveAll(){
		final SequenceSet set = set1;
		set.removeAll(new ArrayList<Sequence>());
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testSetUnsupportedRetainAll() {
		final SequenceSet set = set1;		
		set.retainAll(new ArrayList<Sequence>());
	}
	
	/**
	 * Проверяет объединение выборок.
	 */
	@Test
	public void testSetUnion() {
		checkSanity(set1);
		checkSanity(set2);
		
		SequenceSet union = set1.join(set2);
		checkSanity(union);
		assertEquals(set1.size() + set2.size(), union.size());
		checkSubset(union, set1, 0);
		checkSubset(union, set2, set1.size());
	}
	
	/**
	 * Проверяет фильтрацию выборок.
	 */
	@Test
	public void testFiltering() {
		final SequenceSet set = set1;
		boolean[] selector = new boolean[set.size()];
		
		int cnt = 0;
		for (int i = 0; i < selector.length; i++) {
			selector[i] = Math.random() < 0.5;
			if (selector[i]) cnt++;
		}
		
		SequenceSet filtered = set.filter(selector);
		checkSanity(filtered);
		assertEquals(cnt, filtered.size());
		
		cnt = 0;
		for (int i = 0; i < selector.length; i++) {
			if (selector[i]) {
				assertSame(filtered.id(cnt), set.id(i));
				assertTrue(filtered.observed(cnt) == set.observed(i));
				assertTrue(filtered.hidden(cnt) == set.hidden(i));
				cnt++;
			}
		}
	}
	
	/**
	 * Проверяет кэширование выборок после их загрузки.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testLoadedCache() throws IOException {
		SequenceSet set1 = SetTests.set1;
		SequenceSet set2 = env.loadSet("elegans-I");
		assertSame(set1, set2);
	}
	
	/**
	 * Проверяет сериализацию выборок (простой случай).
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSerializationSimple() throws IOException {
		File file = tempFolder.newFile();
		final SequenceSet set = set1;
		env.save(set, file.getAbsolutePath());
		
		assertTrue(file.isFile());
		System.out.println("len = " + file.length());
		assertTrue(file.length() < 1000);
		
		SequenceSet copy = env.load(file.getAbsolutePath());
		checkSanity(copy);
		assertEquals(set.size(), copy.size());
		checkSubset(set, copy, 0);
	}
	
	/**
	 * Проверяет сериализацию выборок (сложный случай).
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSerializationAdvanced() throws IOException {
		SequenceSet set = set1.join(set2).filter(new RandomFilter(0.4));
		
		File file = tempFolder.newFile();
		env.save(set, file.getAbsolutePath());
		
		assertTrue(file.isFile());
		assertTrue(file.length() < 100000);
		
		SequenceSet copy = env.load(file.getAbsolutePath());
		checkSanity(copy);
		assertEquals(set.size(), copy.size());
		checkSubset(set, copy, 0);
	}
	
	/**
	 * Проверяет создание последовательностей с помощью метода
	 * {@link SequenceUtils#parseSequence(SequenceSet, String)}.
	 */
	@Test
	public void testSequenceCreation() {
		SequenceSet set = new SimpleSequenceSet("ACGT", "xi", null);
		Sequence sequence = SequenceUtils.parseSequence(set, "GxAxTiAiGi");
		assertNull(sequence.id);
		assertSame(set, sequence.set);
		assertEquals(5, sequence.length());
		assertEquals(5, sequence.observed.length);
		assertEquals(5, sequence.hidden.length);
		assertEquals(2, sequence.observed[0]);
		assertEquals(0, sequence.hidden[0]);
		assertEquals(0, sequence.observed[3]);
		assertEquals(1, sequence.hidden[3]);
		
		set = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		
		sequence = SequenceUtils.parseSequence(set, "AagaTc");
		assertNull(sequence.id);
		assertSame(set, sequence.set);
		assertEquals(6, sequence.length());
		assertEquals(6, sequence.observed.length);
		assertEquals(6, sequence.hidden.length);
		assertEquals(0, sequence.observed[0]);
		assertEquals(0, sequence.hidden[0]);
		assertEquals(1, sequence.observed[5]);
		assertEquals(1, sequence.hidden[5]);
	}
	
	/**
	 * Проверяет разбиение последовательности состояний на сегменты.
	 */
	@Test
	public void testSequenceSegmentation() {
		SequenceSet set = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		Sequence sequence = SequenceUtils.parseSequence(set, "AagaggTCAc");
		
		List<Sequence.Segment> segments = sequence.segments();
		assertEquals(4, segments.size());
		
		assertEquals(0, segments.get(0).start);
		assertEquals(0, segments.get(0).end);
		assertEquals(1, segments.get(0).length());
		assertEquals(0, segments.get(0).state);
		
		assertEquals(1, segments.get(1).start);
		assertEquals(5, segments.get(1).end);
		assertEquals(5, segments.get(1).length());
		assertEquals(1, segments.get(1).state);
		
		assertEquals(6, segments.get(2).start);
		assertEquals(8, segments.get(2).end);
		assertEquals(3, segments.get(2).length());
		assertEquals(0, segments.get(2).state);
	}
	
	@Test
	public void testSequenceAutoIds() {
		byte[] observed = new byte[] { 0 };
		final int nSamples = 10000;
		
		Set<String> ids = new HashSet<String>(nSamples);
		
		for (int i = 0; i < nSamples; i++) {
			Sequence seq = new Sequence(observed, null);
			ids.add(seq.id);
		}
		assertEquals(nSamples, ids.size());
	}
}

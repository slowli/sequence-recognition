package ua.kiev.icyb.bio.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

	private static final String SET_1 = "elegans-I";
	
	private static final String SET_2 = "elegans-II";
	
	public static final String CONF_FILE = "tests/env.conf";
	
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
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSingleSet() throws IOException {
		Env env = new Env(CONF_FILE);
		
		SequenceSet set = env.loadSet(SET_1);
		assertEquals("ACGT", set.observedStates());
		assertEquals("xi", set.hiddenStates());
		assertEquals("ACGTacgt", set.completeStates());
		assertEquals(3531, set.size());
		
		checkSanity(set);
	}
	
	/**
	 * Проверяет единичную выборку данных.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testIterator() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
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
	
	@Test
	public void testMutableSetClear() {
		SimpleSequenceSet set = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		
		for (int i = 0; i < 1000; i++) {
			set.add(new Sequence("" + i, new byte[100], new byte[100]));
		}
		
		assertEquals(1000, set.size());
		checkSanity(set);
		
		set.clear();
		assertEquals(0, set.size());
	}
	
	@Test(expected = IndexOutOfBoundsException.class)
	public void testSetInvalidIndex() {
		SequenceSet set = new SimpleSequenceSet("ACGT", "xi", "ACGTacgt");
		set.get(0);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testSetUnsupportedAdd() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		set.add(new Sequence("1", new byte[10], new byte[10]));
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testSetUnsupportedRemove() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		set.remove(new Sequence("1", new byte[10], new byte[10]));
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testSetUnsupportedAddAll() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		set.addAll(new ArrayList<Sequence>());
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testSetUnsupportedRemoveAll() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		set.removeAll(new ArrayList<Sequence>());
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testSetUnsupportedRetainAll() throws IOException {
		Env env = new Env(CONF_FILE);
		SequenceSet set = env.loadSet(SET_1);
		
		set.retainAll(new ArrayList<Sequence>());
	}
	
	/**
	 * Проверяет объединение выборок.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSetUnion() throws IOException {
		Env env = new Env(CONF_FILE);
		
		SequenceSet set1 = env.loadSet(SET_1);
		assertEquals(3531, set1.size());
		checkSanity(set1);
		
		SequenceSet set2 = env.loadSet(SET_2);
		assertEquals(4097, set2.size());
		checkSanity(set2);
		
		SequenceSet union = set1.join(set2);
		checkSanity(union);
		assertEquals(set1.size() + set2.size(), union.size());
		checkSubset(union, set1, 0);
		checkSubset(union, set2, set1.size());
	}
	
	/**
	 * Проверяет фильтрацию выборок.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testFiltering() throws IOException {
		Env env = new Env(CONF_FILE);
		
		SequenceSet set = env.loadSet(SET_1);
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
		Env env = new Env(CONF_FILE);
		
		SequenceSet set1 = env.loadSet(SET_1);
		SequenceSet set2 = env.loadSet(SET_1);
		assertSame(set1, set2);
	}
	
	/**
	 * Проверяет сериализацию выборок (простой случай).
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSerializationSimple() throws IOException {
		Env env = new Env(CONF_FILE);
		
		File file = tempFolder.newFile();
		SequenceSet set = env.loadSet(SET_1);
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
		Env env = new Env(CONF_FILE);
		SequenceSet set1 = env.loadSet(SET_1), set2 = env.loadSet(SET_2);
		
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
}

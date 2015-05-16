package ua.kiev.icyb.bio.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.biojava.bio.BioException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.io.DSSPReader;
import ua.kiev.icyb.bio.io.GenbankReader;

/**
 * Тесты, связанные с преобразованиями данных.
 */
public class IOTests {
	
	private static Env env;
	
	private static String setFilename;
	
	private static String dsspDir;
	
	@BeforeClass
	public static void setup() throws IOException {
		final String testDir = System.getProperty("testdir", "test");
		env = new Env(testDir + "/env.conf");
		setFilename = testDir + "/I.gb.gz";
		dsspDir = testDir + "/dssp";
	}
	
	/**
	 * Тестирует обработчик файлов Genbank (гены).
	 * 
	 * @throws IOException
	 * @throws BioException
	 */
	@Test
	@Category(SlowTest.class)
	public void testGenbank() throws IOException, BioException {
		GenbankReader reader = new GenbankReader(setFilename);
		reader.run(env);
		SequenceSet set = reader.set;
		
		SetTests.checkSanity(set);
		assertEquals("ACGT", set.observedStates());
		assertEquals("xi", set.hiddenStates());
		assertEquals("ACGTacgt", set.completeStates());
		
		assertTrue(set.size() > 2000);
	}

	/**
	 * Тестирует обработчик файлов DSSP (белки).
	 */
	@Test
	public void testDSSP() {
		File dir = new File(dsspDir);
		String[] filenames = dir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".dssp");
			}
		});
		
		for (int i = 0; i < filenames.length; i++) {
			filenames[i] = dir + "/" + filenames[i];
		}
		
		DSSPReader reader = new DSSPReader(filenames);
		reader.uniqueNames = false;
		reader.uniquePrefix = false;
		reader.run(env);
		
		SequenceSet set = reader.getSet();
		SetTests.checkSanity(set);
		
		assertEquals(filenames.length, set.size());
		assertEquals("ACDEFGHIKLMNPQRSTVWY?", set.observedStates());
		assertEquals("-GHIEBTS", set.hiddenStates());
		assertNull(set.completeStates());
		
		for (int i = 0; i < set.size(); i++) {
			assertTrue(set.get(i).length() > 50);
			assertTrue(set.get(i).length() < 5000);
		}
	}
	
	// TODO settings
}

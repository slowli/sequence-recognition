package ua.kiev.icyb.bio.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.biojava.bio.BioException;
import org.junit.Test;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.io.DSSPReader;
import ua.kiev.icyb.bio.io.GenbankReader;

/**
 * Тесты, связанные с преобразованиями данных.
 */
public class IOTests {
	
	private static final String SOURCE_SET_1 = "tests/I.gb.gz";
	
	private static final String SOURCE_DSSP_DIR = "tests/dssp";
	
	public static final String CONF_FILE = "tests/env.conf";
	
	/**
	 * Тестирует обработчик файлов Genbank (гены).
	 * 
	 * @throws IOException
	 * @throws BioException
	 */
	@Test
	public void testGenbank() throws IOException, BioException {
		Env env = new Env(CONF_FILE);
		GenbankReader reader = new GenbankReader(SOURCE_SET_1);
		reader.run(env);
		SequenceSet set = reader.set;
		
		SetTests.checkSanity(set);
		assertEquals("ACGT", set.observedStates());
		assertEquals("xi", set.hiddenStates());
		assertEquals("ACGTacgt", set.completeStates());
		
		System.out.println(set.repr());
		assertTrue(set.size() > 2000);
	}

	/**
	 * Тестирует обработчик файлов DSSP (белки).
	 */
	@Test
	public void testDSSP() {
		Env env = new Env();
		
		File dir = new File(SOURCE_DSSP_DIR);
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
		
		System.out.println(set.repr());
	}
	
	// TODO settings
}

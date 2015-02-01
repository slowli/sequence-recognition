package ua.kiev.icyb.bio.test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.io.DSSPReader;
import ua.kiev.icyb.bio.io.GenbankReader;

public class Init {
	
	public static void main(String[] args) throws IOException {
		Env env = new Env();
		env.setDebugLevel(1);
		
		// Создать выборки для генов
		
		GenbankReader reader = new GenbankReader("I.gb.gz");
		reader.run(env);
		reader.set.saveToFile("elegans-I.gz");
		
		reader = new GenbankReader("II.gb.gz");
		reader.run(env);
		reader.set.saveToFile("elegans-II.gz");
		
		// Создать выборки для белков
		
		File dir = new File("dssp");
		String[] filenames = dir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".dssp");
			}
		});
		
		for (int i = 0; i < filenames.length; i++) {
			filenames[i] = dir + "/" + filenames[i];
		}
		
		DSSPReader dsspReader = new DSSPReader(filenames);
		dsspReader.uniqueNames = false;
		dsspReader.uniquePrefix = false;
		dsspReader.run(env);
		
		dsspReader.getSet().saveToFile("proteins.gz");
	}
}

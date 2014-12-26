package ua.kiev.icyb.bio.io;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;

import org.biojava.bio.BioException;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Feature;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.Location;
import org.biojava.bio.symbol.LocationTools;
import org.biojava.bio.symbol.SymbolList;
import org.biojavax.bio.seq.RichFeature;
import org.biojavax.bio.seq.RichLocation;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.RichSequence.IOTools;
import org.biojavax.bio.seq.SimpleRichLocation;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.GenericSequenceSet;
import ua.kiev.icyb.bio.IOUtils;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.io.res.Messages;

/**
 * Класс для обработки файлов в формате Genbank.
 */
public class GenbankReader {

	/**
	 * Создает непрерывную локацию, покрывающую все нуклеотиды из заданной локации.
	 * 
	 * @param loc
	 *    локация, которую необходимо покрыть
	 * @return
	 *    покрытие
	 */
	private static Location shadow(Location loc) {
		if (loc instanceof RichLocation) {
			RichLocation richLoc = (RichLocation) loc;
			return new SimpleRichLocation(richLoc.getMinPosition(), 
					richLoc.getMaxPosition(),
					richLoc.getRank(),
					richLoc.getStrand());
		} else {
			return LocationTools.shadow(loc);
		}
	}

	private BufferedReader reader;
	private SymbolList sourceSequence;
	
	/** 
	 * Следует ли добавлять в выборку единственную кодирующую последовательность 
	 * для каждого гена (первую упомянутую в файле). По умолчанию в выборку добавляются все
	 * кодирующие последовательности.  
	 */
	public boolean uniqueGenes = false;
	
	/**
	 * Следует ли добавлять в выборку гены с неизвестными нуклеотидами. По умолчанию
	 * такие гены игнорируются. 
	 */
	public boolean allowUnknownNts = false;
	
	/**
	 * Создает объект для чтения данных из файла Genbank.
	 * 
	 * @param filename
	 *    имя файла
	 * @throws IOException
	 *    если при работе с файлом произошла ошибка ввода/вывода
	 */
	public GenbankReader(String filename) throws IOException {
		reader = IOUtils.getReader(filename);
		// Разделить файл на две части: 
		reader = new BufferedReader(splitFile(reader));
	}
	
	/**
	 * Разделяет файл в формате Genbank на две части:
	 * атрибуты последовательности нуклеотидов и сама последовательность.
	 * Обе они сохраняются в строки для последующего использования.
	 * Разделение необходимо из-за того, что инструменты BioJava для
	 * работы с файлами GenBank при наличии длинной последовательности нуклеотидов
	 * работают чересчур медленно.
	 * 
	 * @param reader
	 *    автомат для чтения файла Genbank
	 * @return
	 *    автомат для чтения файла Genbank с вырезанной последовательностью нуклеотидов
	 * @throws IOException
	 */
	private Reader splitFile(BufferedReader reader) throws IOException {
		String line;
		
		boolean readingAttrs = true;
		StringBuilder sequence = new StringBuilder();
		StringBuilder attributes = new StringBuilder(); 
		
		while ((line = reader.readLine()) != null) {
			if (readingAttrs) {
				attributes.append(line + "\n");
			} else {
				sequence.append(line.replaceAll("[^a-z]+", ""));
			}
			
			if (line.startsWith("ORIGIN")) {
				attributes.append("//");
				readingAttrs = false;
			}	
		}
		
		if (readingAttrs) {
			throw new IllegalStateException(Messages.getString("genbank.e_no_seq"));
		}
		try {
			sourceSequence = DNATools.createDNA(sequence.toString());
		} catch (IllegalSymbolException e) {
			throw new IllegalStateException(Messages.getString("genbank.e_illegal_seq"), e);
		}
		Env.debug(1, Messages.format("genbank.read_seq", sourceSequence.length()));
		
		return new StringReader(attributes.toString());
	}
	
	/**
	 * Возвращает последовательность скрытых состояний для заданного
	 * положения кодирующей последовательности.
	 * 
	 * @param loc
	 *    позиция кодирующей последовательности в ДНК
	 * @return
	 *    байтовый массив, в котором нули соответствуют нуклеотидам
	 *    в составе экзонов, единицы — нуклеотидам в интронах
	 */
	private byte[] getHiddenSequence(Location loc) {
		if (LocationTools.coverage(loc) == 0) {
			return new byte[0];
		}
		
		loc = loc.translate(-loc.getMin());
		byte[] seq = new byte[loc.getMax() - loc.getMin() + 1];
		Arrays.fill(seq, (byte) 1); // First, denote all symbols as beloging to introns
		
		for (Iterator<Location> iter = loc.blockIterator(); iter.hasNext(); ) {
			Location block = iter.next();
			Arrays.fill(seq, block.getMin(), block.getMax() + 1, (byte) 0);						
		}
		
		boolean flip = (loc instanceof RichLocation) 
				&& ((RichLocation) loc).getStrand().equals(RichLocation.Strand.NEGATIVE_STRAND); 
		if (flip) {
			for (int i = 0; i < seq.length / 2; i++) {
				byte tmp = seq[i];
				seq[i] = seq[seq.length - i - 1];
				seq[seq.length - i - 1] = tmp;
			}
		}
		
		return seq;
	}
	
	/**
	 * Возвращает последовательность наблюдаемых состояний (т.е. нуклеотидов)
	 * для заданного положения гена.
	 * 
	 * @param alphabet
	 *    алфавит наблюдаемых состояний
	 * @param loc
	 *    положение гена в последовательности ДНК
	 * @return
	 *    массив индексов нуклеотидов гена в предоставленном алфавите;
	 *    <code>null</code>, если по крайней мере один нуклеотид отсутствует
	 *    в алфавите 
	 */
	private byte[] getObservedSequence(String alphabet, Location loc) {
		byte[] seq = new byte[loc.getMax() - loc.getMin() + 1];
		String geneNts = shadow(loc).symbols(sourceSequence).seqString();
		
		for (int i = 0; i < geneNts.length(); i++) {
			char nt = Character.toUpperCase(geneNts.charAt(i));
			int idx = alphabet.indexOf(nt);
			if (idx < 0) {
				// Invalid symbol in nucleotide sequence
				return null;
			}
			seq[i] = (byte)idx;
		}
		return seq;
	}
	
	/**
	 * Добавляет ген в выборку. Ген добавляется только если все его нуклеотиды
	 * есть в алфавите наблюдаемых состояний выборки.
	 * 
	 * @param set
	 *    выборка
	 * @param gene
	 *    позиция гена в ДНК
	 * @param cds
	 *    позиция кодирующей последовательности гена в ДНК
	 */
	private void addGene(GenericSequenceSet set, RichFeature gene, RichFeature cds) {
		byte[] hidden = getHiddenSequence(cds.getLocation());
		byte[] observed = getObservedSequence(set.observedStates(), cds.getLocation());
		
		if (observed != null) {
			set.add(observed, hidden);
		} else {
			Env.debugInline(2, "?");
		}
	}
	
	/**
	 * Преобразует гены в файле формата Genbank в выборку.
	 * 
	 * @return
	 *    выборка из строк, соответствующих генам
	 * @throws BioException
	 *    в случае ошибки обработки файла
	 */
	public SequenceSet transform() throws BioException {
		RichSequence rs = IOTools.readGenbankDNA(reader, null).nextRichSequence();
		Env.debug(1, Messages.format("genbank.seq_name", rs.getName()));
		Env.debug(1, Messages.format("genbank.seq_descr", rs.getDescription()));
		Env.debug(1, "");
		
		int geneIndex = 0;
		Feature gene = null;
		boolean cdsFound = true;
		
		Iterator<Feature> iter = rs.features();
		Feature feat = iter.next();
		//sourceSequence = feat.getSymbols(); // The first feature contains the entire sequence
		
		GenericSequenceSet set = new GenericSequenceSet(
				this.allowUnknownNts ? "ACGTN" : "ACGT", "xi",
				this.allowUnknownNts ? "ACGTNacgtn" : "ACGTacgt");
		
		while (iter.hasNext()) {
			feat = iter.next();			
			
			if (feat.getType().equals("gene")) {
				geneIndex++;
				Env.debugInline(2, ".");
				if (geneIndex % 50 == 0) {
					Env.debug(2, Messages.format("genbank.n_genes", geneIndex));
				}
				gene = feat;
				cdsFound = false;
			} else if ((feat.getType().equals("CDS"))) {
				if (!uniqueGenes || !cdsFound) {
					addGene(set, (RichFeature) gene, (RichFeature) feat);
					cdsFound = true;
				}
			}
		}
		
		Env.debug(1, Messages.format("genbank.n_total", geneIndex));
		return set;
	}
}

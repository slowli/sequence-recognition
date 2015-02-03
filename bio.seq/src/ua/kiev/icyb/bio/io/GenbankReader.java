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
import org.biojavax.CrossRef;
import org.biojavax.RankedCrossRef;
import org.biojavax.bio.seq.RichFeature;
import org.biojavax.bio.seq.RichLocation;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.RichSequence.IOTools;
import org.biojavax.bio.seq.SimpleRichLocation;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.Launchable;
import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.SimpleSequenceSet;

/**
 * Класс для обработки файлов в формате Genbank.
 */
public class GenbankReader implements Launchable {

	private static final long serialVersionUID = 1L;
	
	
	/**
	 * Имя базы данных, согласно которой последовательностям присваиваются идентификаторы.
	 */
	private static final String[] GENE_DB_NAMES = { "GeneID", "GI" };
	
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
	
	public SequenceSet set = null;
	
	private Env env;
	
	private final String filename; 
	
	/**
	 * Создает объект для чтения данных из файла Genbank.
	 * 
	 * @param filename
	 *    имя файла
	 * @throws IOException
	 *    если при работе с файлом произошла ошибка ввода/вывода
	 */
	public GenbankReader(String filename) throws IOException {
		this.filename = filename;
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
		env.debug(1, Messages.format("genbank.read_seq", sourceSequence.length()));
		
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
		
		
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (Iterator<Location> iter = loc.blockIterator(); iter.hasNext(); ) {
			Location block = iter.next();
			if (block.getMin() < min) min = block.getMin();
			if (block.getMax() > max) max = block.getMax();
		}
		
		if (loc.getMin() != min) {
			// Ошибка в BioJava?
			return null;
		}
		
		loc = loc.translate(-min);
		byte[] seq = new byte[max - min + 1];
		
		Arrays.fill(seq, (byte) 1); // Обозначить все нуклеотиды как принадлежащие интронам
		
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
				// Некорректный символ в последовательности нуклеотидов
				return null;
			}
			seq[i] = (byte)idx;
		}
		return seq;
	}
	
	/**
	 * Извлекает идентификатор гена из записи.
	 * 
	 * @param feature
	 *    запись, соответствующая гену или кодирующему участку гена
	 * @return
	 *    идентификатор гена или {@code null}, если идентификатор не содержится в записи
	 */
	private String getGeneID(RichFeature feature) {
		for (Object obj : feature.getRankedCrossRefs()) {
			if (obj instanceof RankedCrossRef) {
				CrossRef ref = ((RankedCrossRef) obj).getCrossRef();
				for (String dbName : GENE_DB_NAMES) {
					if (dbName.equals(ref.getDbname())) {
						return "GI:" + ref.getAccession() + "." + ref.getVersion();
					}
				}
			}
		}
		return null;
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
	 * @param prefix
	 *    префикс для идентификатора гена, если для него задана ссылка на базу данных GeneID
	 */
	private void addGene(SequenceSet set, RichFeature gene, RichFeature cds, String prefix) {
		
		byte[] hidden = getHiddenSequence(cds.getLocation());
		byte[] observed = getObservedSequence(set.observedStates(), cds.getLocation());
		
		Object protId = cds.getRichAnnotation().getProperty("protein_id");
		if (protId == null) {
			env.debug(1, "Missing protein id for the CDS");
		}
		
		String id = null;
		id = getGeneID(gene);
		if (id == null) id = getGeneID(cds);
		
		if (id == null) {
			id = prefix + set.size();
		}
		
		if (protId != null) {
			id += ";prot:" + protId;
		}
		
		if ((observed != null) && (hidden != null)) {
			set.add(new Sequence(id, observed, hidden));
		} else {
			env.debugInline(2, "?");
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
	private SequenceSet transform() throws BioException {
		RichSequence rs = IOTools.readGenbankDNA(reader, null).nextRichSequence();
		env.debug(1, Messages.format("genbank.seq_name", rs.getName()));
		env.debug(1, Messages.format("genbank.seq_descr", rs.getDescription()));
		env.debug(1, "");
		
		String prefix = rs.getAccession() + "." + rs.getVersion() + ":";
		
		int geneIndex = 0;
		Feature gene = null;
		boolean cdsFound = true;
		
		Iterator<Feature> iter = rs.features();
		Feature feat = iter.next();
		
		SequenceSet set = new SimpleSequenceSet(
				this.allowUnknownNts ? "ACGTN" : "ACGT", "xi",
				this.allowUnknownNts ? "ACGTNacgtn" : "ACGTacgt");
		
		while (iter.hasNext()) {
			feat = iter.next();			
			
			if (feat.getType().equals("gene")) {
				geneIndex++;
				env.debugInline(2, ".");
				if (geneIndex % 50 == 0) {
					env.debug(2, Messages.format("genbank.n_genes", geneIndex));
				}
				gene = feat;
				cdsFound = false;
			} else if ((feat.getType().equals("CDS"))) {
				if (!uniqueGenes || !cdsFound) {
					addGene(set, (RichFeature) gene, (RichFeature) feat, prefix);
					cdsFound = true;
				}
			}
		}
		
		env.debug(1, Messages.format("genbank.n_total", geneIndex));
		return set;
	}

	@Override
	public void run(Env env) {
		try {
			this.env = env;
			
			reader = Env.getReader(filename);
			// Разделить файл на две части:
			reader = new BufferedReader(splitFile(reader));
			this.set = this.transform();
		} catch (BioException e) {
			env.exception(e);
		} catch (IOException e) {
			env.exception(e);
		}
	}

	@Override
	public Env getEnv() {
		return this.env;
	}
}

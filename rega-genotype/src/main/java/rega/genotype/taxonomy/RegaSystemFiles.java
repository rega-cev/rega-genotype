package rega.genotype.taxonomy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.EdirectUtil;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Utils;

/**
 * Periodically download the taxonomy file from uniprot url = http://www.uniprot.org/taxonomy/?query=Viruses&format=tab
 * The file is used for taxonomy trees.
 * 
 * @author michael
 */
public class RegaSystemFiles {
	public static String SYSTEM_FILES_DIR = "system-files";
	public static String TAXONOMY_URL = "http://www.uniprot.org/taxonomy/?query=Viruses&format=tab";
	public static String TAXONOMY_FILE_NAME = SYSTEM_FILES_DIR + File.separator + "taxonomy.tab";
	public static String NCBI_VIRUSES_DB_URL = "ftp://ftp.ncbi.nlm.nih.gov/refseq/release/viral/viral.1.1.genomic.fna.gz";
	public static String NCBI_VIRUSES_DB_FILE_NAME = SYSTEM_FILES_DIR + File.separator + "ncbi-viruses.fasta";
	public static String NCBI_VIRUSES_DB_ANNOTATED_FILE_NAME = SYSTEM_FILES_DIR + File.separator + "ncbi-viruses_annotated.fasta";
	public static String NCBI_VIRUSES_AC_NUM_FILE_NAME = SYSTEM_FILES_DIR + File.separator + "ncbi-viruses_accession_numbers";
	public static String NCBI_VIRUSES_TAXONOMY_FILE_NAME = SYSTEM_FILES_DIR + File.separator + "ncbi-taxonomy";

	public static File downloadTaxonomyFile() {
		File taxonomyFile = taxonomyFile();
		if (Utils.wget(TAXONOMY_URL, taxonomyFile))
			return taxonomyFile;
		else
			return null;
	}

	public static File taxonomyFile() {
		final File baseDir = new File(Settings.getInstance().getBaseDir());
		return new File(baseDir, TAXONOMY_FILE_NAME);
	}

	public static File unzipNcbiViruses(File ncbiFileGz) {
		File ncbiVirusesFile = ncbiVirusesFileAnnotated();
		if (FileUtil.unGzip1File(ncbiFileGz, ncbiVirusesFile))
			return ncbiVirusesFile;

		return null;
	}

	public static File downloadNcbiViruses(){
		final File baseDir = new File(Settings.getInstance().getBaseDir());
		File ncbiVirusesFile = ncbiVirusesFileAnnotated();
		File ncbiFileGz = new File(baseDir, SYSTEM_FILES_DIR + File.separator + "viral.1.1.genomic.fna.gz");
		if (Utils.wget(NCBI_VIRUSES_DB_URL, ncbiFileGz)) 
			if (FileUtil.unGzip1File(ncbiFileGz, ncbiVirusesFile))
				return ncbiVirusesFile;

		return null;
	}

	public static File ncbiVirusesFileAnnotated() {
		final File baseDir = new File(Settings.getInstance().getBaseDir());
		return new File(baseDir, NCBI_VIRUSES_DB_ANNOTATED_FILE_NAME);
	}

	public static String taxonomyIdFromAnnotatedNcbiSeq(String seqDescription) {
		String pattern = ".*_taxonomy-id_(\\d+)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(seqDescription);
		return (m.find()) ? m.group(1) : null;
	}

	public static File removePhages() throws ParameterProblemException, IOException, FileFormatException {
		// TODO: currently we identify a phage if it has the word phage in description that is not accurate!!
		final File baseDir = new File(Settings.getInstance().getBaseDir());
		final File ncbiVirusesFile = new File(baseDir, NCBI_VIRUSES_DB_FILE_NAME);

		AlignmentAnalyses alignmentAnalyses = AlignmentAnalyses.readFastaToAlignmentAnalyses(
				new File(baseDir, SYSTEM_FILES_DIR), ncbiVirusesFile);
		List<AbstractSequence> sequences = alignmentAnalyses.getAlignment().getSequences();
		for (int i = sequences.size() - 1; i > -1; --i)
			if (sequences.get(i).getDescription().contains("phage"))
				sequences.remove(i);
		alignmentAnalyses.getAlignment().writeOutput(new FileOutputStream(ncbiVirusesFile),
				SequenceAlignment.FILETYPE_FASTA);

		return ncbiVirusesFile;
	}

	public static File annotateNcbiDb() throws IOException, ParameterProblemException, 
	FileFormatException, ApplicationException, InterruptedException {
		final File baseDir = new File(Settings.getInstance().getBaseDir());
		final File ncbiVirusesFile = new File(baseDir, NCBI_VIRUSES_DB_FILE_NAME);
		final File workDir = new File(baseDir, SYSTEM_FILES_DIR);
		final File acNumbers = new File(baseDir, NCBI_VIRUSES_AC_NUM_FILE_NAME);
		final File taxonomy = new File(baseDir, NCBI_VIRUSES_TAXONOMY_FILE_NAME);
		final File ncbiVirusesFileAnnotated = ncbiVirusesFileAnnotated();
		workDir.mkdirs();

		AlignmentAnalyses ncbiSequences = AlignmentAnalyses.readFastaToAlignmentAnalyses(workDir, ncbiVirusesFile);
		EdirectUtil.createNcbiAccQuery(ncbiSequences, acNumbers);
		EdirectUtil.querytaxonomyIds(acNumbers, taxonomy);
		
		BufferedReader taxonomyBr = new BufferedReader(new FileReader(taxonomy));

		PrintWriter ncbiVirusesWriter = new PrintWriter(ncbiVirusesFileAnnotated);

		String l = null;
		while ((l = taxonomyBr.readLine()) != null) {
			String[] row = l.split("\t");

			String taxonomyId = row[1];
			//String organizedName = row[2];
			
			String ac = EdirectUtil.getAccessionNumberFromNCBI(row[0]);
			AbstractSequence seq = null;
			for (AbstractSequence s : ncbiSequences.getAlignment().getSequences()){
				if (s.getName().contains(ac)){
					seq = s;
					break;
				}
			}

			ncbiVirusesWriter.println(">" + seq.getName()
					+ " " + seq.getDescription() + "_taxonomy-id_" + taxonomyId );
			ncbiVirusesWriter.println(seq.getSequence());
		}
		ncbiVirusesWriter.close();
		
		taxonomyBr.close();

		return ncbiVirusesFileAnnotated;
	}
}

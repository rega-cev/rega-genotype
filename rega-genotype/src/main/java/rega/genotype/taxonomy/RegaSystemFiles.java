package rega.genotype.taxonomy;
import java.io.File;

import rega.genotype.singletons.Settings;
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
		File ncbiVirusesFile = ncbiVirusesFile();
		if (FileUtil.unGzip1File(ncbiFileGz, ncbiVirusesFile))
			return ncbiVirusesFile;

		return null;
	}
	
	public static File downloadNcbiViruses(){
		final File baseDir = new File(Settings.getInstance().getBaseDir());
		File ncbiVirusesFile = ncbiVirusesFile();
		File ncbiFileGz = new File(baseDir, SYSTEM_FILES_DIR + File.separator + "viral.1.1.genomic.fna.gz");
		if (Utils.wget(NCBI_VIRUSES_DB_URL, ncbiFileGz)) 
			if (FileUtil.unGzip1File(ncbiFileGz, ncbiVirusesFile))
				return ncbiVirusesFile;

		return null;
	}

	public static File ncbiVirusesFile() {
		final File baseDir = new File(Settings.getInstance().getBaseDir());
		return new File(baseDir, NCBI_VIRUSES_DB_FILE_NAME);
	}
}

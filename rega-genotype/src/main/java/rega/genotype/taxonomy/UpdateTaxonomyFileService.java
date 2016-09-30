package rega.genotype.taxonomy;
import java.io.File;
import java.io.IOException;

import rega.genotype.singletons.Settings;

/**
 * Periodically download the taxonomy file from uniprot url = http://www.uniprot.org/taxonomy/?query=Viruses&format=tab
 * The file is used for taxonomy trees.
 * 
 * @author michael
 */
public class UpdateTaxonomyFileService {
	private static String TAXONOMY_URL = "http://www.uniprot.org/taxonomy/?query=Viruses&format=tab";
	private static String TAXONOMY_FILE_NAME = "taxonomy.tab";

	public static File download() {
		File taxonomyFile = taxonomyFile();

		taxonomyFile.delete();

		Process fetchFasta = null;
		try {
			String cmd = "wget '" + TAXONOMY_URL + "' -O " + taxonomyFile.getAbsolutePath();
			String[] shellCmd = {"/bin/sh", "-c", cmd};
			System.err.println(shellCmd);
			fetchFasta = Runtime.getRuntime().exec(shellCmd);

			int exitResult = fetchFasta.waitFor();
			if (exitResult != 0)
				return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}

		return taxonomyFile;
	}

	public static File taxonomyFile() {
		final File baseDir = new File(Settings.getInstance().getBaseDir());
		return new File(baseDir, TAXONOMY_FILE_NAME);
	}
}

package rega.genotype.taxonomy;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

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
		RandomAccessFile fos = null;

		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(TAXONOMY_URL).openConnection();

			File taxonomyFile = taxonomyFile();
			taxonomyFile.delete();
			taxonomyFile.createNewFile();

			fos = new RandomAccessFile(taxonomyFile, "rw");

			final int BUF_SIZE = 4 * 1024;
			byte[] bytes = new byte[BUF_SIZE];
			int read = 0;

			BufferedInputStream binaryreader = new BufferedInputStream(conn.getInputStream());

			while ((read = binaryreader.read(bytes)) > 0)
				fos.write(bytes, 0, read);

			binaryreader.close();
			return taxonomyFile;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public static File taxonomyFile() {
		final File baseDir = new File(Settings.getInstance().getBaseDir());
		return new File(baseDir, TAXONOMY_FILE_NAME);
	}
}

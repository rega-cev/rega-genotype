package rega.genotype.taxonomy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;

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

	public static void updateTaxonomyModel() {
		download();
	}
	
	public static File download() {
		URLConnection connection;
		File taxonomyFile = taxonomyFile();
		try {
			connection = new URL(TAXONOMY_URL).openConnection();
			connection.setRequestProperty("Content-Type", "multipart/form-data"); // Allow to add a file

			FileWriter w = new FileWriter(taxonomyFile);
			IOUtils.copy(connection.getInputStream(), w);

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		System.err.println("Taxonomy file downloaded.");
		
		return taxonomyFile;
	}

	public static File taxonomyFile() {
		final File baseDir = new File(Settings.getInstance().getBaseDir());
		return new File(baseDir, TAXONOMY_FILE_NAME);
	}
}

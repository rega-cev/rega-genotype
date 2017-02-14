package rega.genotype;

/**
 * Global constants and enums
 * @author michael
 */
public class Constants {
	// The Tool manifest may change with the time and newer version will have to handle old manifest files.
	public static final String SOFTWARE_VERSION = "0";

	public static final String RESULT_FILE_NAME = "result.xml";
	public static final String NGS_RESULT_FILE_NAME = "ngs-results.xml";
	public static final String SEQUENCES_FILE_NAME = "sequences.fasta";
	public static final String BLAST_XML_FILE_NAME = "blast.xml";
	
	public enum Permissions {Read, Write}
	public enum Mode {Classical, Ngs}
}

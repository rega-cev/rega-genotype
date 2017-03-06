package rega.genotype.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.singletons.Settings;

public class EdirectUtil {

	public static void querytaxonomyIds(File accessionNumbersQuery, File out) throws ApplicationException, IOException, InterruptedException {
		String edirectPath = Settings.getInstance().getConfig().getGeneralConfig().getEdirectPath();

		String epost = edirectPath + "epost";
		String efetch = edirectPath + "efetch";
		String xtract = edirectPath + "xtract";

		String cmd = 
				// query taxonomy ids from NCBI
				"cat " + accessionNumbersQuery.getAbsolutePath() + "|" 
				+ epost + " -db nuccore -format acc|"
				+ efetch + " -db nuccore -format docsum | " 
				+ xtract + " -pattern DocumentSummary -element Extra,TaxId,Organism > " + out.getAbsolutePath();
		Utils.execShellCmd(cmd);
	}

	public static void queryFasta(File accessionNumbersQuery, File out) throws ApplicationException, IOException, InterruptedException {
		String edirectPath = Settings.getInstance().getConfig().getGeneralConfig().getEdirectPath();

		String epost = edirectPath + "epost";
		String efetch = edirectPath + "efetch";

		String cmd = 
				// query fasta ids from NCBI
				" cat " + accessionNumbersQuery.getAbsolutePath() + "|" 
				+ epost + " -db nuccore -format acc|" 
				+ efetch + " -db nuccore -format fasta > " + out.getAbsolutePath();

		Utils.execShellCmd(cmd);
	}

	public static String getAccessionNumberFromNCBI(String fastaName) {
		String accessionNumber = null;
		if (fastaName.contains("|")){
			String[] split = fastaName.split("\\|");
			for(int i = 0; i < split.length - 1; ++i){
				String s = split[i];
				if (s.equals("gb") || s.equals("emb") || s.equals("dbj")
						|| s.equals("tpe") || s.equals("ref"))
					accessionNumber = split[i + 1];
			}
			if (accessionNumber == null) {
				System.err.println("bad accession numebr regex " + fastaName);
				return null;
			}
		} else 
			accessionNumber = fastaName;

		return accessionNumber;
	}

	
	public static void createNcbiAccQuery(AlignmentAnalyses ncbiSequences, File outFile) 
			throws IOException, ParameterProblemException, FileFormatException {
		StringBuilder accessionBuild = new StringBuilder();

		for (AbstractSequence s: ncbiSequences.getAlignment().getSequences()) {
			String accessionNumber = getAccessionNumberFromNCBI(s.getName());
			if (accessionNumber == null){
				System.err.println("WARNING: could not find AC num for " + s.getName());
				continue;
			}

			accessionBuild.append(accessionNumber);
			accessionBuild.append(System.getProperty("line.separator"));
		}

		FileUtil.writeStringToFile(outFile, accessionBuild.toString());
	}

	public static void createNcbiAccQuery(List<String> accessionNumbers, File outFile) 
			throws IOException, ParameterProblemException, FileFormatException {
		StringBuilder accessionBuild = new StringBuilder();

		for (String accessionNumber: accessionNumbers) {
			accessionBuild.append(accessionNumber);
			accessionBuild.append(System.getProperty("line.separator"));
		}

		FileUtil.writeStringToFile(outFile, accessionBuild.toString());
	}
}

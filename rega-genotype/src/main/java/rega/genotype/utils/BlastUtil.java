package rega.genotype.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import rega.genotype.AbstractSequence;
import rega.genotype.ApplicationException;
import rega.genotype.BlastAnalysis;
import rega.genotype.singletons.Settings;

/**
 * Use command line blast to restive raw blast output (not in specific typing tool context like BlastAnalysis). 
 * 
 * @author michael
 */
public class BlastUtil {
	/**
	 * call NCBI blast formatdb.
	 * 
	 * @param db in fasta format
	 * @param workDir process work dir. 
	 * @throws ApplicationException
	 */
	public static void formatDB(File db, File workDir) throws ApplicationException {
		String blastPath = Settings.getInstance().getBlastPathStr();
		String cmd = blastPath + "formatdb -p F -o T -i " + db.getAbsolutePath();

		System.err.println(cmd);
		
		Utils.executeCmd(cmd, workDir, "blast exited with error");
	}

	public static List<String[]> blastResults(AbstractSequence sequence, File workDir, File blastdb, Logger logger) throws IOException, InterruptedException, ApplicationException {
		Process blast = null;
		String blastPath = Settings.getInstance().getBlastPathStr();
		List<String[]> ans = new ArrayList<String[]>();

		if (sequence.getLength() != 0) {
			File query = new File(workDir,"query.fasta");
			FileOutputStream queryFile = new FileOutputStream(query);
			sequence.writeFastaOutput(queryFile);
			queryFile.close();

			String cmd = blastPath + "blastall -p blastn -q -1 -r 1 " 
					+ " -i " + query.getAbsolutePath()
					+ " -m 8 -d " + blastdb.getAbsolutePath();

			logger.info(cmd);

			blast = Runtime.getRuntime().exec(cmd, null, workDir);

			BufferedReader inReader = new BufferedReader(new InputStreamReader(blast.getInputStream()));
			String inLine = null;
			while ((inLine = inReader.readLine()) != null) { // clear in buff so java dont dead lock
				String[] values = inLine.split("\t");
				ans.add(values);
			}


			final BufferedReader errReader = new BufferedReader(new InputStreamReader(blast.getErrorStream()));
			String errLine;
			while ((errLine = errReader.readLine()) != null)
				System.out.println(errLine);

			int exitResult = blast.waitFor();

			blast.getErrorStream().close();
			blast.getInputStream().close();
			blast.getOutputStream().close();

			if (exitResult != 0)
				throw new ApplicationException("blast exited with error: " + exitResult);
		} 

		return ans;			
	}

	/**
	 * Run blastn on input sequence. 
	 * Write the result refseq with best score in out.
	 * Note: formatDB must be called inthe same workDir before.
	 * @return the bit score of best mutch.
	 */
	public static Double computeBestRefSeq(AbstractSequence sequence, File workDir, File out, File blastdb, 
			double maxEValue, double minBitScore, Logger logger)
			throws ApplicationException, IOException, InterruptedException {

		out.getParentFile().mkdirs();
		if (out.exists())
			out.delete();

		List<String[]> blastResults = blastResults(sequence, workDir, blastdb, logger);
		if (blastResults.size() > 0) {
			String[] values = blastResults.get(0);
			if (values.length != 12)
				throw new ApplicationException("blast result format error");

			double eVal = Double.valueOf(values[BlastAnalysis.BLAST_RESULT_E_VALUE_IDX]);
			double bitScore = Double.valueOf(values[BlastAnalysis.BLAST_RESULT_BIT_SCORE_IDX]);

			if (eVal < maxEValue && bitScore > minBitScore) {
				findSequence(values[BlastAnalysis.BLAST_RESULT_SUBJECT_ID_IDX],
						blastdb, out, logger);
				return bitScore;
			}
		}

		return null;
	}

	/**
	 * Run blastn on input sequence. 
	 * Write the result refseq with best score in out.
	 * Note: formatDB must be called inthe same workDir before.
	 * @return the bit score of best mutch.
	 */
	public static Map<String, Double> computeAllBestRefSeq(AbstractSequence sequence, File workDir, File out, File blastdb, 
			double maxEValue, double minBitScore, Logger logger)
			throws ApplicationException, IOException, InterruptedException {

		out.getParentFile().mkdirs();
		if (out.exists())
			out.delete();

		Map<String, Double> ans = new HashMap<String, Double>();
		List<String[]> blastResults = blastResults(sequence, workDir, blastdb, logger);
		for (String[] values: blastResults) {
			if (values.length != 12)
				throw new ApplicationException("blast result format error");

			double eVal = Double.valueOf(values[BlastAnalysis.BLAST_RESULT_E_VALUE_IDX]);
			double bitScore = Double.valueOf(values[BlastAnalysis.BLAST_RESULT_BIT_SCORE_IDX]);
			String id = values[BlastAnalysis.BLAST_RESULT_SUBJECT_ID_IDX];
			if (!ans.containsKey(id) && eVal < maxEValue && bitScore > minBitScore) {
				findSequence(id, blastdb, out, logger);
				ans.put(id, bitScore);
			}
		}

		return ans;
	}

	public static void findSequence(String sequenceId, File blastdb, File out, Logger logger) throws IOException, ApplicationException, InterruptedException {

		String blastPath = Settings.getInstance().getBlastPathStr();
		String cmd = blastPath + "fastacmd -s '" + sequenceId + "' -d " + blastdb.getAbsolutePath() 
				+ " >> " + out.getAbsolutePath();

		logger.info(cmd);

		String[] shellCmd = {"/bin/sh", "-c", cmd};

		Process p = Runtime.getRuntime().exec(shellCmd);
		int exitResult = p.waitFor();

		if (exitResult != 0)
			throw new ApplicationException("blast exited with error: " + exitResult);
	}
}

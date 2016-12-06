package rega.genotype.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

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

	/**
	 * Run blastn on input sequence. 
	 * Write the result refseq with best score in out.
	 * Note: formatDB must be called inthe same workDir before.
	 */
	public static boolean computeBestRefSeq(AbstractSequence sequence, File workDir, File out, File blastdb, double maxEValue, double minBitScore)
			throws ApplicationException, IOException, InterruptedException {

		Process blast = null;
		String blastPath = Settings.getInstance().getBlastPathStr();

		if (sequence.getLength() != 0) {
			File query = new File(workDir,"query.fasta");
			FileOutputStream queryFile = new FileOutputStream(query);
			sequence.writeFastaOutput(queryFile);
			queryFile.close();

			String cmd = blastPath + "blastall -p blastn -q -1 -r 1 " 
					+ " -i " + query.getAbsolutePath()
					+ " -m 8 -d " + blastdb.getAbsolutePath();

			System.err.println(cmd);

			blast = Runtime.getRuntime().exec(cmd, null, workDir);

			BufferedReader inReader = new BufferedReader(new InputStreamReader(blast.getInputStream()));
			String inLine = inReader.readLine();
			String[] values = null;
			if (inLine != null){
				values = inLine.split("\t");

				while ((inLine = inReader.readLine()) != null) // clear in buff so java dont dead lock
					System.out.println(inLine);
			} else {
				System.err.println("WARNING: computeBestRefSeq: ref seq not found for " + sequence.getName());
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

			if (values == null)
				return false;

			if (values.length != 12)
				throw new ApplicationException("blast result format error");

			double eVal = Double.valueOf(values[BlastAnalysis.BLAST_RESULT_E_VALUE_IDX]);
			double bitScore = Double.valueOf(values[BlastAnalysis.BLAST_RESULT_BIT_SCORE_IDX]);

			if (eVal < maxEValue && bitScore > minBitScore) {
				findSequence(values[BlastAnalysis.BLAST_RESULT_SUBJECT_ID_IDX],
						blastdb, out);
				return true;
			} else
				return false;
		} else
			return false;
	}

	public static void findSequence(String sequenceId, File blastdb, File out) throws IOException, ApplicationException, InterruptedException {
		out.getParentFile().mkdirs();
		if (out.exists())
			out.delete();

		String blastPath = Settings.getInstance().getBlastPathStr();
		String cmd = blastPath + "fastacmd -s '" + sequenceId + "' -d " + blastdb.getAbsolutePath() 
				+ " -o " + out.getAbsolutePath();

		System.err.println(cmd);

		String[] shellCmd = {"/bin/sh", "-c", cmd};

		Process p = Runtime.getRuntime().exec(shellCmd);
		int exitResult = p.waitFor();

		if (exitResult != 0)
			throw new ApplicationException("blast exited with error: " + exitResult);
	}
}

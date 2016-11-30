package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;

import rega.genotype.ApplicationException;
import rega.genotype.config.NgsModule;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.LogUtils;
import rega.genotype.utils.StreamReaderRuntime;

public class Assemble {

	/**
	 * Assemble many shot reads fastq to long contigs.
	 * 
	 * @param sequenceFile1 File with forward reads.
	 * @param sequenceFile2 File with reverse reads.
	 * @param workDir
	 * @param virusName as defined by diamond blast
	 * @return the contig file
	 * @throws ApplicationException
	 */
	public static File spadesAssemble(File sequenceFile1, File sequenceFile2,
			File workDir, String virusName, NgsModule ngsModule) throws ApplicationException {

		long startTime = System.currentTimeMillis();

		File contigsDir = new File(workDir, NgsFileSystem.contigsDir(virusName));
		contigsDir.mkdirs();

		String cmd = Settings.getInstance().getConfig().getGeneralConfig().getSpadesCmd();
		// TODO: now only pair-ends
		cmd += " -1 " + sequenceFile1.getAbsolutePath();
		cmd += " -2 " + sequenceFile2.getAbsolutePath();

		cmd += " -o " + contigsDir.getAbsolutePath();
		cmd += " --threads 6 " + ngsModule.getSpadesOptions();

		LogUtils.getLogger(workDir).info(cmd);
		Process p = null;

		try {
			p = StreamReaderRuntime.exec(cmd, null, workDir.getAbsoluteFile());
			int exitResult = p.waitFor();

			if (exitResult != 0) {
				throw new ApplicationException("Spades exited with error: " + exitResult);
			}
		} catch (IOException e) {
			throw new ApplicationException("Spades failed error: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			if (p != null)
				p.destroy();
			throw new ApplicationException("Spades failed error: " + e.getMessage(), e);
		}

		File ans = new File(contigsDir, "contigs.fasta");
		if (!ans.exists())
			throw new ApplicationException("Spades did not create contigs file.");

		System.err.println("Assembly time for: " + sequenceFile1.getName() + " is: " + (startTime - System.currentTimeMillis()));
		
		return ans;
	}
}

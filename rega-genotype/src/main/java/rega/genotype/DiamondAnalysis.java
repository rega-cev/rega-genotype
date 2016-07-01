package rega.genotype;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import rega.genotype.utils.StreamReaderRuntime;

public class DiamondAnalysis extends AbstractAnalysis {

	public static String diamondPath = "";
	protected static double limiteScore = 50.0;
	public static final int DIAMOND_RESULT_QUERY_ID_IDX = 0;
	public static final int DIAMOND_RESULT_SUBJECT_ID_IDX = 1;
	public static final int DIAMOND_RESULT_PERCENT_IDENTITY_IDX = 2;
	public static final int DIAMOND_RESULT_ALINGMENT_LENGTH_IDX = 3;
	public static final int DIAMOND_RESULT_MISMATCHES_IDX = 4;
	public static final int DIAMOND_RESULT_GAP_IDX = 5;
	public static final int DIAMOND_RESULT_Q_START_IDX = 6;
	public static final int DIAMOND_RESULT_Q_END_IDX = 7;
	public static final int DIAMOND_RESULT_S_START_IDX = 8;
	public static final int DIAMOND_RESULT_S_END_IDX = 9;
	public static final int DIAMOND_RESULT_E_VALUE_IDX = 10;
	public static final int DIAMOND_RESULT_BIT_SCORE_IDX = 11;

	public DiamondAnalysis(AlignmentAnalyses owner, String id) {
		super(owner, id);
		// TODO Auto-generated constructor stub
	}
	
	private void cleanOldDB() {
		getTempFile("query.fasta").delete();
		getTempFile("matches.daa").delete();
		getTempFile("matches.m8").delete();
	}

	public boolean makeDB(SequenceAlignment analysis)
			throws ApplicationException {
		cleanOldDB();
		Process makedb = null;
		File db = getTempFile("db.faa");
		try {
			String cmd = diamondPath + " makedb -i "
					+ db.getAbsolutePath() + " -d " + getTempFile("")
					+ File.separator + "nr.dmnd --quiet";
			System.err.println(cmd);
			makedb = StreamReaderRuntime.exec(cmd, null, workingDir);
			int exitResult = makedb.waitFor();

			if (exitResult != 0) {
				throw new ApplicationException("makedb exited with error: "
						+ exitResult);
			}

		} catch (FileNotFoundException e) {
			if (makedb != null)
				makedb.destroy();
			throw new ApplicationException("makedb failed error: "
					+ e.getMessage(), e);
		} catch (IOException e) {
			if (makedb != null)
				makedb.destroy();
			throw new ApplicationException("makedb failed error: "
					+ e.getMessage(), e);
		} catch (InterruptedException e) {
			if (makedb != null)
				makedb.destroy();
			throw new ApplicationException("makedb failed error: "
					+ e.getMessage(), e);
		}
		return true;
	}

	public boolean blastX() throws ApplicationException {
		File db = getTempFile("db.dmnd");
		File query = getTempFile("query.fna");
		Process blastx = null;
		try {
			String cmd = diamondPath + " blastx -d "
					+ db.getAbsolutePath() + " -q " + query.getAbsolutePath()
					+ " -a " + getTempFile("") + File.separator + "matches.daa --sensitive --min-score "+ limiteScore +" --quiet";
			blastx = StreamReaderRuntime.exec(cmd, null, workingDir);
			int exitResult = blastx.waitFor();

			if (exitResult != 0) {
				throw new ApplicationException("blastx exited with error: "
						+ exitResult);
			}
		} catch (FileNotFoundException e) {
			if (blastx != null)
				blastx.destroy();
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		} catch (IOException e) {
			if (blastx != null)
				blastx.destroy();
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		} catch (InterruptedException e) {
			if (blastx != null)
				blastx.destroy();
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		}

		return true;
	}

	public boolean blastP() throws ApplicationException {
		File db = getTempFile("db.dmnd");
		File query = getTempFile("query.fna");
		Process blastp = null;

		try {
			String cmd = diamondPath + " blastp -d "
					+ db.getAbsolutePath() + " -q " + query.getAbsolutePath()
					+ " -a " + getTempFile("") + File.separator + "matches.daa --sensitive --min-score "+ limiteScore +" --quiet";
			blastp = StreamReaderRuntime.exec(cmd, null, workingDir);
			int exitResult = blastp.waitFor();

			if (exitResult != 0) {
				throw new ApplicationException("blastp exited with error: "
						+ exitResult);
			}
		} catch (FileNotFoundException e) {
			if (blastp != null)
				blastp.destroy();
			throw new ApplicationException("blastp failed error: "
					+ e.getMessage(), e);
		} catch (IOException e) {
			if (blastp != null)
				blastp.destroy();
			throw new ApplicationException("blastp failed error: "
					+ e.getMessage(), e);
		} catch (InterruptedException e) {
			if (blastp != null)
				blastp.destroy();
			throw new ApplicationException("blastp failed error: "
					+ e.getMessage(), e);
		}
		return true;
	}

	private Result view(AbstractSequence sequence) throws ApplicationException {
		File db = getTempFile("matches.daa");
		Process diamond = null;
		try {
			String cmd = diamondPath + " view -a " + db.getAbsolutePath()
					+ " -o " + getTempFile("") + File.separator
					+ "matches.m8 --quiet";
			System.err.println(cmd);
			diamond = Runtime.getRuntime().exec(cmd, null, workingDir);
			Result result = null;
			
			return result;

		} catch (IOException e) {
			if (diamond != null)
				diamond.destroy();
			throw new ApplicationException(
					"Error: I/O Error while invoking blast: " + e.getMessage());
		}
	}

	public interface DiamondResults {
		String[] next() throws ApplicationException;
	}

	

	
	public Result run(AbstractSequence sequence) throws AnalysisException {
    	try {
    		Result r = view(sequence);
    		if (getTracer() != null)
    			getTracer().addResult(r);
    		return (rega.genotype.DiamondAnalysis.Result) r;
    	} catch (ApplicationException e) {
    		throw new AnalysisException(getId(), sequence, e);
    	}
		// return (rega.genotype.BlastAnalysis.Result) super.run(sequence);
	}

	@Override
	Result run(SequenceAlignment alignment, AbstractSequence sequence)
			throws AnalysisException {
		try {
			makeDB(alignment);
			return view(sequence);
		} catch (ApplicationException e) {
			throw new AnalysisException(getId(), sequence, e);
		}
	}
}

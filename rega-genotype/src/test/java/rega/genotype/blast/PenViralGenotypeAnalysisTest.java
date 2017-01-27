package rega.genotype.blast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import rega.genotype.AlignmentAnalyses;
import rega.genotype.BlastAnalysis;
import rega.genotype.BlastAnalysis.Result;
import rega.genotype.Constants.Mode;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.singletons.Settings;
import rega.genotype.tools.blast.BlastTool;
import rega.genotype.ui.utils.TestUtils;
import rega.genotype.utils.FileUtil;

/**
 * Tests for the pen-viral tool (blast tool)
 * 
 * Note: the test is using xmls from base-unit-test-work-dir/xml/pen-viral1
 * 
 * @author michael
 */
public class PenViralGenotypeAnalysisTest extends TestCase{
	private String fasta;
	private List<File> jobDirs = new ArrayList<File>();
	private final double DELTA = 0.01;
	private AlignmentAnalyses alignmentAnalyses = null;
	private BlastAnalysis blastAnalysis = null;

	protected void setUp() {	
		fasta = ">2I_AF100469_Mexico_1992\n" + 
				"TCCAGGCTTTACCATAATGGCCGCAATCCTGGCATACACCATAGGAACGACGCATTTCCAAAGAGTCC" + 
				"TGATATTCATCCTACTGACAGCCATCGCTCCTTCAATGACAATGCGCTGCATAGGAATATCAAATAGG" + 
				"GACTTTGTGGAAGGAGTGTCAGGAGGGAGTTGGGTTGACATAGTTTTAGAACATGGAAGTTGTGTGAC" + 
				"GACGATGGCAAAAAATAAACCAACACTGGACTTTGAACTGATAAAAACAGAAGCCAAACAACCCGCCA" + 
				"CCTTAAGGAAGTACTGTATAGAGGCTAAACTGACCAACACGACAACAGACTCGCGCTGCCCAACACAA" + 
				"GGGGAACCCACCCTGAATGAAGAGCAGGACAAAAGGTTTGTCTGCAAACATTCTATGGTAGACAGAGG" + 
				"ATGGGGAAATGGATGTGGATTGTTTGGAAAAGGAGGCATCGTGACCTGTGCTATGTTCACATGCAAAA" + 
				"AGAACATGGAAGGGAAAATTGTGCAGCCAGAAAACCTGGAATACACTGTCGTGATAACACCTCATTCA" + 
				"GGGGAAGAACATGCAGTCGGAAATGACACAGGAAAACATGGTAAAGAAGTCAAGATAACACCACAGAG" + 
				"CTCCATCACAGAGGCGGAACTGACAGGCTATGGCACTGTTACGATGGAGTGCTCTCCAAGAACGGGCC" + 
				"TCGACTTCAATGAGATGGTGTTGCTGCAAATGGAAGACAAAGCTTGGCTGGTGCACAGACAATGGTTC";
		
		File jobDir = TestUtils.setup(fasta);
    	jobDirs.add(jobDir);

    	//tool data is in base-unit-test-work-dir/xml/pen-viral1
    	ToolConfig toolConfig = Settings.getInstance().getConfig().getToolConfigById("pen-viral", "1");

		try {
			File blastFile = new File(toolConfig.getConfiguration(), "blast.xml");
			jobDir.mkdirs();
			alignmentAnalyses = new AlignmentAnalyses(blastFile, null, jobDir);
		} catch (IOException e) {
			e.printStackTrace();
			fail("could not create AlignmentAnalyses.");
		} catch (ParameterProblemException e) {
			e.printStackTrace();
			fail("could not create AlignmentAnalyses.");
		} catch (FileFormatException e) {
			e.printStackTrace();
			fail("could not create AlignmentAnalyses.");
		}
		blastAnalysis = (BlastAnalysis) alignmentAnalyses.getAnalysis("blast");

		// clear cutoffs 
		blastAnalysis.setDetailsOptions(null);
		blastAnalysis.setAbsCutoff(null);
		blastAnalysis.setAbsMaxEValue(null);
		blastAnalysis.setAbsSimilarityMinPercent(null);
		blastAnalysis.setRelativeCutoff(null);
		blastAnalysis.setRelativeMaxEValue(null);
		blastAnalysis.setRelativeSimilarityMinPercent(null);

		List<Result> analysisResults = blastAnalysis.analyze(alignmentAnalyses, fasta);
		assertTrue(analysisResults.size() == 1);
		Result result = analysisResults.get(0);
		assertFalse(result.haveSupport()); // no cutoffs specified. 
    }

	protected void tearDown() {
		TestUtils.deleteJobDirs(jobDirs);
	}

	@Test
	public void testCLI() {
		// CLI: is using some reflections therefore it is easy
		// to brake it by changing some function declarations.
		// Note: this is testing the latest build of CLI Tool
		// (dist/rega-genotype-cli-tool-alpha.jar)

		String cmd = "java -jar "
				+ " dist/rega-genotype-cli-tool-alpha.jar "
				+ " rega.genotype.viruses.generic.GenericTool "
				+ " zika xml short.fasta result.xml "
				+ " -c base-unit-test-work-dir/config.json "
				+ " -w base-unit-test-work-dir/job/ ";

		Runtime runtime = Runtime.getRuntime();
		Process p;
		try {
			p = runtime.exec(cmd, null, new File("."));

			p.waitFor();

			InputStream stderr = p.getErrorStream();
			InputStreamReader isr = new InputStreamReader(stderr);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ( (line = br.readLine()) != null)
				System.err.println(line);
			br.close();

			p.getErrorStream().close();
			p.getInputStream().close();
			p.getOutputStream().close();

		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (InterruptedException e) {
			e.printStackTrace();
			assertTrue(false);
		} finally {
			try {
				FileUtils.deleteDirectory(new File("base-unit-test-work-dir/job"));
				new File("base-unit-test-work-dir/job").mkdirs();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		assertTrue(true);
	}

	@Test
    public void testAnalysisRuntime() {
       	File jobDir = TestUtils.setup(fasta);
    	jobDirs.add(jobDir);

    	//tool data is in base-unit-test-work-dir/xml/pen-viral1
    	ToolConfig toolConfig = Settings.getInstance().getConfig().getToolConfigById("pen-viral", "1");
    	BlastTool blastTool;
		try {
			blastTool = new BlastTool(toolConfig, jobDir);
	    	blastTool.analyze(TestUtils.getFastaFile(jobDir).getAbsolutePath(),
					TestUtils.getResultFile(jobDir).getAbsolutePath());
		} catch (IOException e1) {
			e1.printStackTrace();
			fail("IOException occured during analysis runtime");
		} catch (ParameterProblemException e1) {
			e1.printStackTrace();
			fail("ParameterProblemException occured during analysis runtime");
		} catch (FileFormatException e1) {
			e1.printStackTrace();
			fail("FileFormatException occured during analysis runtime");
		}

    	GenotypeResultParser p = new GenotypeResultParser(){
			@Override
			public void endSequence() {
				String assignment = getValue("/genotype_result/sequence/conclusion/assigned/id");

				String similarityStr = getValue("/genotype_result/sequence/result[@id='blast']/cluster/absolute-similarity");
				String absoluteStr = getValue("/genotype_result/sequence/result[@id='blast']/cluster/absolute-score");
				String relativeStr = getValue("/genotype_result/sequence/result[@id='blast']/cluster/relative-score");

				double similarity = Double.parseDouble(similarityStr);
				double absolute = Double.parseDouble(absoluteStr);
				double relative = Double.parseDouble(relativeStr);

				assertEquals("Flaviviridae Flavivirus Dengue virus", assignment);
				assertTrue(Math.abs(absolute - 981.0) < DELTA);
				assertTrue(Math.abs(relative - 4.9296484) < DELTA);
				assertTrue(Math.abs(similarity - 91.31016) < DELTA);
			}

			@Override
			public boolean skipSequence() {
				return false;
			}    		
    	};
    	p.parseResultFile(jobDir, Mode.Classical);
	}

	/**
	 * Test: Assignment is made if result passes all cut offs. (if 1 
	 * cutoff is not satisfied result.haveSupport() is false else it is tyue)
	 */

	@Test
	public void testMaxCutoff() {
		blastAnalysis.setAbsCutoff(1000.0);
		check(alignmentAnalyses, false);

		blastAnalysis.setAbsCutoff(100.0);
		check(alignmentAnalyses, true);
	}

	@Test
	public void testAbsMaxEValueCutoff() {
		blastAnalysis.setAbsMaxEValue(Math.pow(Math.E, -10));// on a small database the probability of randomly getting a match is very low.
		Result result = blastAnalysis.analyze(alignmentAnalyses, 
				">HIGH_E_VALUE_SEQUENCE\n" +
				"ACTGCTGATGTTGAATTAGA").get(0);
		assertFalse(result.haveSupport());

		blastAnalysis.setAbsMaxEValue(0.0);
		check(alignmentAnalyses, true);
	}

	@Test
	public void testRelativeCutoff() {
		blastAnalysis.setRelativeCutoff(10.0);
		check(alignmentAnalyses, false);

		blastAnalysis.setRelativeCutoff(1.0);
		check(alignmentAnalyses, true);
	}

	@Test
	public void testRelativeMaxEValueCutoff() {
		blastAnalysis.setRelativeMaxEValue(Math.pow(Math.E, -33));// on a small database the probability of randomly getting a match is very low.
		Result result = blastAnalysis.analyze(alignmentAnalyses, 
				">HIGH_E_VALUE_SEQUENCE\n" +
						"ACTGCTGATGTTGAATTAGA"
				).get(0);
		assertFalse(result.haveSupport());

		blastAnalysis.setRelativeMaxEValue(0.0);
		check(alignmentAnalyses, true);
	}

	@Test
	public void testAbsSimilarityCutoff() {
		blastAnalysis.setAbsSimilarityMinPercent(95.0);
		check(alignmentAnalyses, false);

		blastAnalysis.setAbsSimilarityMinPercent(80.0);
		check(alignmentAnalyses, true);
	}

	@Test
	public void testRelativeSimilarityCutoff() {
		blastAnalysis.setRelativeSimilarityMinPercent(10.0);
		check(alignmentAnalyses, false);

		blastAnalysis.setRelativeSimilarityMinPercent(1.0);
		check(alignmentAnalyses, true);
	}

	@Test
	public void testMultiple() {
		// 2 sequences with good score should both be in the results.
		blastAnalysis.setAbsCutoff(50.0);
		String s = fasta + "\n" + fasta;
		List<Result> analysisResults =
				blastAnalysis.analyze(alignmentAnalyses, s);
		Result result = analysisResults.get(0);
		assertEquals(analysisResults.size(), 2);
		assertEquals(result.haveSupport(), true);
	}

	private void check(AlignmentAnalyses alignmentAnalyses, boolean haveSupport) {
		List<Result> analysisResults = blastAnalysis.analyze(alignmentAnalyses, fasta);
		Result result = analysisResults.get(0);
		assertEquals(result.haveSupport(), haveSupport);
	}

	@Test
    public void testBenchmark() {
    	boolean doTestBenchmark = false;
    	if (!doTestBenchmark)
    		return; // This test can slow down ant.

    	ToolConfig toolConfig = Settings.getInstance().getConfig().getToolConfigById("pen-viral", "1");

    	String longFasta = FileUtil.readFile(new File(toolConfig.getConfiguration(), "test-sequemces.fasta"));
    	File jobDir = TestUtils.setup(longFasta);
    	jobDirs.add(jobDir);

    	BlastTool blastTool;
		try {
			blastTool = new BlastTool(toolConfig, jobDir);
			
			long startTime = System.currentTimeMillis();

	    	blastTool.analyze(TestUtils.getFastaFile(jobDir).getAbsolutePath(),
					TestUtils.getResultFile(jobDir).getAbsolutePath());

	    	System.err.println("Full analysis time in ms: " + (System.currentTimeMillis() - startTime));
			assertTrue((System.currentTimeMillis() - startTime) < 7000); // can change on different computers. 
		} catch (IOException e1) {
			e1.printStackTrace();
			fail("IOException occured during analysis runtime");
		} catch (ParameterProblemException e1) {
			e1.printStackTrace();
			fail("ParameterProblemException occured during analysis runtime");
		} catch (FileFormatException e1) {
			e1.printStackTrace();
			fail("FileFormatException occured during analysis runtime");
		}
	}
}

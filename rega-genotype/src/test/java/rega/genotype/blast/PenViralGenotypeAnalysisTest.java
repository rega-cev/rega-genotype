package rega.genotype.blast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.BlastAnalysis;
import rega.genotype.BlastAnalysis.Result;
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
    }

	protected void tearDown() {
		TestUtils.deleteJobDirs(jobDirs);
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
    	p.parseFile(jobDir);
    }

	@Test
	public void testBlast() {
		File jobDir = TestUtils.setup(fasta);
    	jobDirs.add(jobDir);

    	//tool data is in base-unit-test-work-dir/xml/pen-viral1
    	ToolConfig toolConfig = Settings.getInstance().getConfig().getToolConfigById("pen-viral", "1");
		
		AlignmentAnalyses alignmentAnalyses = null;
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
		BlastAnalysis blastAnalysis = (BlastAnalysis) alignmentAnalyses.getAnalysis("blast");
		blastAnalysis.setDetailsOptions(null); // this part does not change the computation.

		// test max cutoff
		blastAnalysis.setAbsCutoff(1000.0);
		List<Result> analysisResults = blastAnalysis.analyze(alignmentAnalyses, fasta);
		assertTrue(analysisResults.size() == 1);

		Result result = analysisResults.get(0);

		assertTrue(result.getAbsScore() == -1);
		assertEquals(result.getConcludedCluster(), null);
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

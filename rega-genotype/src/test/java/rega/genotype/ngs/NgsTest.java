package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Test;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.singletons.Settings;
import rega.genotype.tools.blast.BlastTool;
import rega.genotype.ui.utils.TestUtils;

public class NgsTest  extends TestCase{
	private final double DELTA = 0.01;

	@Test
	public void testNGS() {

		File jobDir = TestUtils.setup();
		//tool data is in base-unit-test-work-dir/xml/pen-viral1
    	ToolConfig toolConfig = Settings.getInstance().getConfig().getToolConfigById("pen-viral", "1");
    	BlastTool blastTool;
		try {
			blastTool = new BlastTool(toolConfig, jobDir);
		} catch (IOException e1) {
			e1.printStackTrace();
			fail("IOException occured during analysis runtime");
			return;
		} catch (ParameterProblemException e1) {
			e1.printStackTrace();
			fail("ParameterProblemException occured during analysis runtime");
			return;
		} catch (FileFormatException e1) {
			e1.printStackTrace();
			fail("FileFormatException occured during analysis runtime");
			return;
		}

		File pe1 = new File("base-unit-test-work-dir" + File.separator + "ngs-test" + File.separator
				+ "RES166_S76_L001_R1_001.fastq");
		File pe2 = new File("base-unit-test-work-dir" + File.separator + "ngs-test" + File.separator
				+ "RES166_S76_L001_R2_001.fastq");

		NgsResultsTracer ngsResults;
		try {
			ngsResults = new NgsResultsTracer(jobDir);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
			return;
		}

		if (!NgsFileSystem.addFastqFiles(ngsResults, pe1, pe2)) {
			fail("Could not copy ngs files");
			return;
		}

		NgsAnalysis ngsAnalysis = new NgsAnalysis(ngsResults,  
				Settings.getInstance().getConfig().getNgsModule(), toolConfig);
		ngsAnalysis.analyze();

    	GenotypeResultParser p = new GenotypeResultParser(){
			@Override
			public void endSequence() {
				//String seqName = getValue("/genotype_result/@name");
				String seqLen = getValue("/genotype_result/@length");
				String assignment = getValue("/genotype_result/sequence/conclusion/assigned/id");
				String absoluteStr = getValue("/genotype_result/sequence/result[@id='blast']/cluster/absolute-score");

				double absolute = Double.parseDouble(absoluteStr);

				assertEquals("Retroviridae Lentivirus", assignment);
				assertTrue(Math.abs(absolute - 4231.0) < DELTA);
				assertEquals(seqLen, "4758"); // Note: if the sequence is longed it is probably good and we only need to change the test.
			}

			@Override
			public boolean skipSequence() {
				return false;
			}    		
    	};
    	p.parseResultFile(jobDir);
   
		assertTrue(true);
	}
}

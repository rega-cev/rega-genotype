package blast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.tools.blast.BlastTool;
import rega.genotype.ui.utils.TestUtils;

public class PenViralGenotypeAnalysisTest extends TestCase{
private String fasta;
    
    private List<File> jobDirs = new ArrayList<File>();
    
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

    public void testAnalysisRuntime() {
       	File jobDir = TestUtils.setup(fasta);
    	jobDirs.add(jobDir);

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
				assertEquals("Flaviviridae Flavivirus Dengue virus", assignment);
			}

			@Override
			public boolean skipSequence() {
				return false;
			}    		
    	};
    	p.parseFile(jobDir);
    }
}

package rega.genotype.ui.hiv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.utils.TestUtils;
import rega.genotype.viruses.hiv.HIVTool;

public class HIVGenotypeAnalysisTest extends TestCase {
    private String hiv_fasta;
    
    private List<File> jobDirs = new ArrayList<File>();
    
	protected void setUp() {
		hiv_fasta = ">gi|20136660|gb|AF493411.1\n" + 
			"CCTCAGATCACTCTTTGGCAGCGACCCTTCGTTACAATAAAAATAGGGGGACAACTAATAGAAGCCCTAT" + 
			"TAGATACAGGAGCAGATGATACAGTATTAGAAGACATAGATTTGCCAGGAAGATGGAAACCAAAAATAAT" + 
			"AGGAGGAGTTGGAGGTTTTATCAAAGTAAGACAGTATGATCAGGTACCTGTAGAAATCTGCGGACATAAA" + 
			"GTTATAACTACAGTATTAGTAGGAGCTACACCTGTCAACATAATTGGAAGAAATCTGATGACTAAGATTG" + 
			"GCTGCACTTTAAATTTTCCCATTAGTCCTATTGAAACTGTACCAGTAAAATTAAAGCCAGGAATGGATGG" + 
			"CCCAAAAGTCAAACAATGGCCATTGACAGAAGAAAAAATAAAAGCATTAATAGAAATTTGTACAGAATTG" + 
			"GARAAAGAAGGAAAAATTTCAAAAATTGGGCCTGAAAATCCATACAATACTCCAGTATTTGCCATAAAGA" + 
			"AAAAAGAAAGTTCTAGTTCTAAATGGAGAAAGGTAGTAGATTTCAGAGAACTTAATAAAAGAACTCAAGA" + 
			"CTTCTGTGAAGTCCAATTAGGAATACCACATCCTGCAGGATTAAAAAAGAACAAATCAGTAACARTACTR" + 
			"GATGTGGGTGATGCATATTTTTCAATTCCCTTAGATGAAGACTTCAGGAAGTATACTGCATTTACCATAC" + 
			"CTAGTATAAACAATGAGAAACCAGGGATTAGATATCAGTACAATGTGCTYCCACAGGGATGGAAAGGATC" + 
			"ACCAGCAATATTCCAAAGTAGCATGACAAAAATCTTAGAGCCTTATAGAAAACAAAATCCAGACATAGTT" + 
			"ATCTGTCAATACATGGATGATTTGTATGTAGCATCTGACTTAGAAATAGGGCAGCATAGAACAAAAATAG" + 
			"AGGAACTGAGACAACATTTGTGGAAGTGGGGATTCTACACACCAGACAAAAAATATCAGAAAGAACCCCC" + 
			"ATTCCTTTGGATG";
    }

	protected void tearDown() {
		TestUtils.deleteJobDirs(jobDirs);
	}

    public void testAnalysisRuntime() {
       	File jobDir = TestUtils.setup(hiv_fasta);
    	jobDirs.add(jobDir);
    	
		HIVTool hiv;
		try {
			hiv = new HIVTool(jobDir);
			hiv.analyze(TestUtils.getFastaFile(jobDir).getAbsolutePath(),
					TestUtils.getResultFile(jobDir).getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			fail("IOException occured during analysis runtime");
		} catch (ParameterProblemException e) {
			fail("ParameterProblemException occured during analysis runtime");
		} catch (FileFormatException e) {
			fail("FileFormatException occured during analysis runtime");
		}
		
    	GenotypeResultParser p = new GenotypeResultParser(){
			@Override
			public void endSequence() {
				String assignment = getValue("/genotype_result/sequence/conclusion/assigned/name");
				assertEquals(assignment, "HIV-1 Subtype B");
			}

			@Override
			public boolean skipSequence() {
				return false;
			}    		
    	};
    	p.parseFile(jobDir);
    }
}

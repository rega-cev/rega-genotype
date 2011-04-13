package rega.genotype.ui.nov;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.utils.TestUtils;
import rega.genotype.viruses.nov.NoVTool;

public class NovGenotypeAnalysisTest extends TestCase {
    private String hiv_fasta;
    private String nov_fasta;
    
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
		
		nov_fasta = ">" + "N06_303_01\n" +
		"AGTCCTCTTTGAGGCCAAGCTGCACAAGCAAGGCTTTATAACTGTGGCTCACACTGGTGA" + 
		"TAACCCAATTGTCATGCCACCAAATGGGTATTTTAGATTTGAAGCCTGGGTTAATCAGTT" +
		"TTACTCACTCGCCC";
    }
	
	protected void tearDown() {
		TestUtils.deleteJobDirs(jobDirs);
	}

    public void testQualityCheck() {
    	File jobDir = TestUtils.setup(hiv_fasta);
    	jobDirs.add(jobDir);
		
    	NoVTool nov;
		try {
			nov = new NoVTool(jobDir);
			nov.analyze(TestUtils.getFastaFile(jobDir).getAbsolutePath(),
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
				String assignment = getValue("/genotype_result/sequence/conclusion/assigned/id");
				assertEquals(assignment, "Unassigned");
			}

			@Override
			public boolean skipSequence() {
				return false;
			}    		
    	};
    	p.parseFile(jobDir);
    }
    
    public void testAnalysisRuntime() {
       	File jobDir = TestUtils.setup(nov_fasta);
    	jobDirs.add(jobDir);
    	
		NoVTool nov;
		try {
			nov = new NoVTool(jobDir);
			nov.analyze(TestUtils.getFastaFile(jobDir).getAbsolutePath(),
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
				String assignment = getValue("/genotype_result/sequence/conclusion[@id='ORF2']/assigned/id");
				assertEquals(assignment, "II.7");
			}

			@Override
			public boolean skipSequence() {
				return false;
			}    		
    	};
    	p.parseFile(jobDir);
    }
    
    public void testAsciiFastaIds() {
		final String seq = "GGCGTCGATGACGCCNCCCCATCTGATGGGTCCACAGCCAACCTCGTCCCAGAGGTCAACAATGAGGTTATGGCTTTGGA"; 
		
		int i = 0;
		StringBuffer fasta = new StringBuffer();
		for (int c=33; c<127; c++) {
			char cc = (char)c;
			if (!Character.isDigit(cc) && !Character.isLetter(cc)) {
				fasta.append(">A" + String.valueOf(i) + cc + "\n");
				fasta.append(seq + "\n");
				
				i++;
			}				
		}
		
       	File jobDir = TestUtils.setup(fasta.toString());
    	jobDirs.add(jobDir);
		
		NoVTool nov;
		try {
			nov = new NoVTool(jobDir);
			nov.analyze(TestUtils.getFastaFile(jobDir).getAbsolutePath(),
					TestUtils.getResultFile(jobDir).getAbsolutePath());
		} catch (Exception e) {
			fail("Exception occured during analysis runtime");
		}
		
    	GenotypeResultParser p = new GenotypeResultParser(){
			@Override
			public void endSequence() {

			}

			@Override
			public boolean skipSequence() {
				return false;
			}    		
    	};
    	try {
    		p.parseFile(jobDir);
    	} catch (Exception e) {
    		fail("Exception occured during XML parsing");
    	}
    }
}

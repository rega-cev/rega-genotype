package rega.genotype.ui.nov;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.utils.Utils;
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
		Utils.deleteJobDirs(jobDirs);
	}

    public void testQualityCheck() {
    	File jobDir = Utils.setup(hiv_fasta);
    	jobDirs.add(jobDir);
		
    	NoVTool nov;
		try {
			nov = new NoVTool(jobDir);
			nov.analyze(Utils.getFastaFile(jobDir).getAbsolutePath(),
					Utils.getResultFile(jobDir).getAbsolutePath());
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
       	File jobDir = Utils.setup(nov_fasta);
    	jobDirs.add(jobDir);
    	
		NoVTool nov;
		try {
			nov = new NoVTool(jobDir);
			nov.analyze(Utils.getFastaFile(jobDir).getAbsolutePath(),
					Utils.getResultFile(jobDir).getAbsolutePath());
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
}

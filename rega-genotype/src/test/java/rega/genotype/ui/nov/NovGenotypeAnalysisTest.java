package rega.genotype.ui.nov;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.Constants.Mode;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.utils.TestUtils;
import rega.genotype.viruses.generic.GenericTool;

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

    	ToolConfig toolConfig = Settings.getInstance().getConfig().getToolConfigById("nov", "1");
		try {
			GenericTool nov = new GenericTool(toolConfig, jobDir);
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
		p.parseResultFile(jobDir, Mode.Classical);
	}

	public void testAnalysisRuntime() {
		File jobDir = TestUtils.setup(nov_fasta);
		jobDirs.add(jobDir);

    	ToolConfig toolConfig = Settings.getInstance().getConfig().getToolConfigById("nov", "1");
		try {
			GenericTool nov = new GenericTool(toolConfig, jobDir);
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
				String assignment = getValue("/genotype_result/sequence/conclusion[@region='ORF2']/assigned/id");
				assertEquals(assignment, "II.7");
			}

			@Override
			public boolean skipSequence() {
				return false;
			}    		
		};
		p.parseResultFile(jobDir, Mode.Classical);
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

    	ToolConfig toolConfig = Settings.getInstance().getConfig().getToolConfigById("nov", "1");
		try {
			GenericTool nov = new GenericTool(toolConfig, jobDir);
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
			p.parseResultFile(jobDir, Mode.Classical);
		} catch (Exception e) {
			/* That's alright, we expected an exception */
			return;
		}
		fail("Did not get exception on illegal FASTA input characters?");
	}
}

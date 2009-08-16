package rega.genotype.ui.hiv;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.Settings;
import rega.genotype.viruses.hiv.HIVTool;

public class HIVGenotypeAnalysisTest extends TestCase {
    private File jobDir;
    
	private File fasta;
    private File result;
    
	protected void setUp() {
		do {
			jobDir = new File(System.getProperty("java.io.tmpdir") + File.separatorChar +"jobDir" + File.separatorChar + System.currentTimeMillis());
			System.err.println(jobDir.getAbsolutePath());
		} while (jobDir.exists());
		jobDir.mkdir();
		
		fasta = new File(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta");
		
		String fastaContent = ">gi|20136660|gb|AF493411.1\n" + 
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
		try {
			GenotypeLib.writeStringToFile(fasta, fastaContent);
		} catch (IOException e) {
			fail("Could not write fasta String to fasta file");
		}
		
		result = new File(jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
		
		GenotypeLib.initSettings(Settings.getInstance());
    }

    protected void tearDown() {
        try {
			FileUtils.deleteDirectory(jobDir);
		} catch (IOException e) {
			fail("Could not delete the jobDir directory");
		}
    }

    public void testAnalysisRuntime() {
		HIVTool hiv;
		try {
			hiv = new HIVTool(jobDir);
			hiv.analyze(fasta.getAbsolutePath(),
					result.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			fail("IOException occured during analysis runtime");
		} catch (ParameterProblemException e) {
			fail("ParameterProblemException occured during analysis runtime");
		} catch (FileFormatException e) {
			fail("FileFormatException occured during analysis runtime");
		}
		
    	SaxParser p = new SaxParser(){
			@Override
			public void endSequence() {
				String assignment = getValue("genotype_result.sequence.conclusion.assigned.major.assigned.name");
				assertEquals(assignment, "HIV-1 Subtype B");
			}    		
    	};
    	p.parseFile(jobDir);
    }
}

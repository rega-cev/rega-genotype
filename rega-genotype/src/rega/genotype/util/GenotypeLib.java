package rega.genotype.util;

import java.io.File;
import java.io.IOException;

import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.SequenceAlign;
import rega.genotype.viruses.hiv.HIVTool;

public class GenotypeLib {
	public static void initSettings(Settings s) {
		PhyloClusterAnalysis.paupCommand = s.getPaupCmd();
		SequenceAlign.clustalWPath = s.getClustalWCmd();
		GenotypeTool.setXmlBasePath(s.getXmlPath().getAbsolutePath());
		BlastAnalysis.blastPath = s.getBlastPath().getAbsolutePath();
		PhyloClusterAnalysis.puzzleCommand = s.getTreePuzzleCmd();
	}
	
	public static void startAnalysis(File jobDir, Class analysis, Settings settings) {
		
	}
	
	public static void main(String [] args) {
		Settings s = Settings.getInstance();
		
		initSettings(s);
		
		try {
			HIVTool hiv = new HIVTool(new File("/home/plibin0/projects/utrecht/genotype"));
			hiv.analyze("/home/plibin0/projects/utrecht/genotype/test-small.fasta", "/home/plibin0/projects/utrecht/genotype/result.xml");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParameterProblemException e) {
			e.printStackTrace();
		} catch (FileFormatException e) {
			e.printStackTrace();
		}
	}
}

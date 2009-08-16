package rega.genotype.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;

import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

public class AlignmentResource extends WResource {

	private int fileType, sequenceType;
	private File nexusFile;

	public AlignmentResource(File nexusFile, int sequenceType, int fileType) {
		this.sequenceType = sequenceType;
		this.fileType = fileType;
		this.nexusFile = nexusFile;
	}

	@Override
	protected void handleRequest(WebRequest request, WebResponse response)
			throws IOException {
		response.setContentType("application/txt");
		

		try {
			SequenceAlignment a = new SequenceAlignment(new FileInputStream(nexusFile), SequenceAlignment.FILETYPE_NEXUS, sequenceType);
			a.writeOutput(response.getOutputStream(), fileType);
		} catch (ParameterProblemException e) {
			e.printStackTrace();
		} catch (FileFormatException e) {
			e.printStackTrace();
		}

	}

}

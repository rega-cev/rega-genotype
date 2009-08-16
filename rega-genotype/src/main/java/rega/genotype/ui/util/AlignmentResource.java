package rega.genotype.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;

import eu.webtoolkit.jwt.WResource;

public class AlignmentResource extends WResource {

	private int fileType, sequenceType;
	private File nexusFile;

	public AlignmentResource(File nexusFile, int sequenceType, int fileType) {
		this.sequenceType = sequenceType;
		this.fileType = fileType;
		this.nexusFile = nexusFile;
	}

	@Override
	protected String resourceMimeType() {
		return "application/txt";
	}

	@Override
	protected boolean streamResourceData(OutputStream stream,
			HashMap<String, String> arguments) throws IOException {

		try {
			SequenceAlignment a = new SequenceAlignment(new FileInputStream(nexusFile), SequenceAlignment.FILETYPE_NEXUS, sequenceType);
			a.writeOutput(stream, fileType);
		} catch (ParameterProblemException e) {
			e.printStackTrace();
		} catch (FileFormatException e) {
			e.printStackTrace();
		}

		return true;
	}

}

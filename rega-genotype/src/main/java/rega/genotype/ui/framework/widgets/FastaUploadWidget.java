package rega.genotype.ui.framework.widgets;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

import rega.genotype.FileFormatException;
import rega.genotype.SequenceAlignment;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.Utils;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

/**
 * General fasta upload contains text area, file upload and verification for fasta sequences.
 * 
 * @author michael
 */
//TODO: use it every where.
public class FastaUploadWidget extends WContainerWidget{
	private WTextArea sequenceTA;
	private FileUpload fileUpload;
	private WText errorText;

	public FastaUploadWidget() {
		new WText("<div>A) Paste nucleotide sequence(s) in FASTA format:</div>", this);
		
		sequenceTA = new WTextArea(this);
		sequenceTA.setObjectName("seq-input-fasta");
		sequenceTA.setRows(15);
		sequenceTA.setColumns(83);
		sequenceTA.setStyleClass("fasta-ta");
		sequenceTA.setText(tr("sequenceInput.example").toString());

		Utils.removeSpellCheck(sequenceTA);

		new WText("<div>B) Or, upload a FASTA with nucleotide sequences:</div>", this);
		fileUpload = new FileUpload();
		addWidget(fileUpload);

		errorText = new WText(this);
		errorText.setStyleClass("error-text");

		fileUpload.uploadedFile().addListener(this, new Signal1.Listener<File>() {
			public void trigger(File f) {                
				try {
					if (f.exists()) {
						String fasta = GenotypeLib.readFileToString(f);
						sequenceTA.setText(fasta);
						CharSequence error = verifyFasta(fasta);
						validateInput(error);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public String getText() {
		return sequenceTA.getText();
	}

	private CharSequence verifyFasta(String fastaContent) {
		int sequenceCount = 0;

		LineNumberReader r = new LineNumberReader(new StringReader(fastaContent));

		try {
			while (true) {
				if (SequenceAlignment.readFastaFileSequence(r, SequenceAlignment.SEQUENCE_DNA, true)
						== null)
					break;
				++sequenceCount;
			}

			if(sequenceCount == 0) {
				return tr("startForm.noSequence");
			} else if (sequenceCount > Settings.getInstance().getMaxAllowedSeqs()) {
				return tr("startForm.tooManySequences");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return tr("startForm.ioError");
		} catch (FileFormatException e) {
			return e.getMessage();
		}

		return null;
	}

	private void validateInput(CharSequence error) {
		if (error == null) {
			sequenceTA.setStyleClass("edit-valid");
			errorText.setHidden(true);
		} else {
			sequenceTA.setStyleClass("edit-invalid");
			errorText.setHidden(false);
			errorText.setText(error);
		}
	}

}

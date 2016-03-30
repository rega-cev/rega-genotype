package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.io.IOException;

import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.utils.FileUtil;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextEdit;

/**
 * Simple xml editor with save option.
 * 
 * @author michael
 */
public class FileEditor extends WContainerWidget {
	public FileEditor(final File file) {

		new WText(file.getName(), this);
		final WTextEdit edit = new WTextEdit(this);
		final WPushButton saveB = new WPushButton("Save", this);
		final WText infoT = new WText(this);
		
		String fileText = FileUtil.readFile(file);

		if (fileText != null) {
			edit.setText(fileText);
		} else {
			infoT.setText("Could not read file.");
		}

		saveB.clicked().addListener(this, new Signal.Listener() {
			public void trigger() {
				try {
					FileUtil.writeStringToFile(file, edit.getText());
					infoT.setText("Saved.");
				} catch (IOException e) {
					e.printStackTrace();
					new MsgDialog("Error", "Write to file failed.");
				}
			}
		});
		
	}

}

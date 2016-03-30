package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.io.IOException;

import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.utils.FileUtil;
import eu.webtoolkit.jwt.EventSignal;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

/**
 * Simple xml editor with save option.
 * 
 * @author michael
 */
public class FileEditor extends WContainerWidget {
	private final WTextArea edit = new WTextArea(this);
	private Signal saved = new Signal();
	private File file;

	public FileEditor(final File file) {

		this.file = file;
		final WPushButton saveB = new WPushButton("Save", this);
		final WText infoT = new WText(this);
		
		edit.setWidth(new WLength(600));
		edit.setHeight(new WLength(300));
		
		String fileText = FileUtil.readFile(file);

		if (fileText != null) {
			edit.setText(fileText);
		} else {
			infoT.setText("Could not read file.");
		}

		saveB.clicked().addListener(this, new Signal.Listener() {
			public void trigger() {
				save();
			}
		});
	}

	public void save() {
		try {
			file.delete();
			FileUtil.writeStringToFile(file, edit.getText());
			saved().trigger();
		} catch (IOException e) {
			e.printStackTrace();
			new MsgDialog("Error", "Write to file failed.");
		}
	}
	
	public EventSignal changed() {
		return edit.changed();
	}

	public Signal saved() {
		return saved;
	}
}

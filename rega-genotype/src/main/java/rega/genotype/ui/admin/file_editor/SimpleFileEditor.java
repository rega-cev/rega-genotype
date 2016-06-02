package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.io.IOException;

import rega.genotype.ui.framework.widgets.Dialogs;
import rega.genotype.utils.FileUtil;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

/**
 * File content editor with save option.
 * 3 modes:
 * - xml editor 
 * - text editor
 * - Image viewer 
 * Mode is chosen by file extension.
 * 
 * @author michael
 */
public class SimpleFileEditor extends WContainerWidget {
	private static final int MAX_DISPLAY_FILE_SIZE = 500000; //0.5MB
	public enum Mode {TextEditor, ImageViewer}
	private Mode mode;

	private WText infoT = null;
	private WTextArea edit = null;
	private Signal changed = new Signal();

	private File file;

	public SimpleFileEditor(final File file) {

		this.file = file;

		String extension = FileUtil.getFileExtension(file);
		if(extension.equals("png") || extension.equals("gif")
				|| extension.equals("jpg"))
			mode = Mode.ImageViewer;
		else 
			mode = Mode.TextEditor;

		if (mode == Mode.ImageViewer) {
			WImage img = new WImage(new WFileResource("image", file.getAbsolutePath()),
					file.getName());
			
			img.setMaximumSize(new WLength("100%"), new WLength(350));

			addWidget(img);
		} else {
			infoT = new WText(this);

			edit = new WTextArea(this);
			edit.setWidth(new WLength("100%"));
			edit.setHeight(new WLength(300));

			rereadFile();

			edit.changed().addListener(edit, new Signal.Listener() {
				public void trigger() {
					save();
					changed.trigger();
				}
			});
		}

	}

	public void save() {
		if (mode != Mode.ImageViewer && file.length() <= MAX_DISPLAY_FILE_SIZE)
			try {
				file.delete();
				FileUtil.writeStringToFile(file, edit.getText());
			} catch (IOException e) {
				e.printStackTrace();
				Dialogs.infoDialog("Error", "Write to file failed.");
			}
	}

	public Signal changed() {
		return changed;
	}

	public void rereadFile() {
		if (mode != Mode.TextEditor)
			return;

		if (file.length() > MAX_DISPLAY_FILE_SIZE) {
			edit.setText("");
			infoT.setText("Large file, content is not displayed.");
			return;
		}

		String fileText = FileUtil.readFile(file);

		if (fileText != null) {
			edit.setText(fileText);
		} else {
			edit.setText("");
			infoT.setText("Could not read file.");
		}
	}
}

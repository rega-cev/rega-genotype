package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.io.IOException;

import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.utils.FileUtil;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPushButton;
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
public class FileEditor extends WContainerWidget {
	public enum Mode {TextEditor, ImageViewer}
	private Mode mode;

	private WTextArea edit = null;
	private Signal saved = new Signal();
	private Signal changed = new Signal();

	private File file;

	public FileEditor(final File file) {

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
			img.setWidth(new WLength("100%"));
			addWidget(img);
		} else {
			edit = new WTextArea(this);
			edit.setWidth(new WLength(600));
			edit.setHeight(new WLength(300));

			final WPushButton saveB = new WPushButton("Save", this);
			final WText infoT = new WText(this);

			String fileText = FileUtil.readFile(file);

			if (fileText != null) {
				edit.setText(fileText);
			} else {
				infoT.setText("Could not read file.");
			}

			edit.changed().addListener(edit, new Signal.Listener() {
				public void trigger() {
					changed.trigger();
				}
			});
			saveB.clicked().addListener(this, new Signal.Listener() {
				public void trigger() {
					save();
				}
			});
		}

	}

	public void save() {
		if (mode != Mode.ImageViewer)
			try {
				file.delete();
				FileUtil.writeStringToFile(file, edit.getText());
				saved().trigger();
			} catch (IOException e) {
				e.printStackTrace();
				new MsgDialog("Error", "Write to file failed.");
			}
	}

	public Signal changed() {
		return changed;
	}

	public Signal saved() {
		return saved;
	}
}

package rega.genotype.ui.admin.file_editor.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;

import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlWriter;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlWriter.CssTheme;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlWriter.Genome;
import rega.genotype.ui.admin.file_editor.xml.XmlUtils;
import rega.genotype.ui.admin.file_editor.xml.XmlUtils.ValueType;
import rega.genotype.ui.admin.file_editor.xml.XmlUtils.XmlMsgNode;
import rega.genotype.ui.framework.widgets.DirtyHandler;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.framework.widgets.ObjectListComboBox;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileServlet;
import rega.genotype.ui.util.FileUpload;
import eu.webtoolkit.jwt.Coordinates;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal.Listener;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.WBrush;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WIntValidator;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPaintDevice;
import eu.webtoolkit.jwt.WPaintedWidget;
import eu.webtoolkit.jwt.WPainter;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WPainter.Image;
import eu.webtoolkit.jwt.WPainterPath;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTabWidget;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WTableRow;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;
import eu.webtoolkit.jwt.WTextEdit;
import eu.webtoolkit.jwt.WValidator;

/**
 * Smart Editor resources.xml and config.xml
 * 
 * @author michael
 *
 */
public class UiFileEditor extends FormTemplate{

	private FileUpload fileUpload = new FileUpload();
	private File workDir;
	private WLineEdit genomeColor = new WLineEdit("#53b808");;
	private WLineEdit imageStartLE = new WLineEdit();
	private WLineEdit imageEndLE = new WLineEdit();
	private WLineEdit genomeStartLE = new WLineEdit();
	private WLineEdit genomeEndLE = new WLineEdit();;

	private WTextArea exapmleSequenceT = new WTextArea();
	private WTextArea authorsT = new WTextArea();
	private WTextArea introductionT = new WTextArea();
	private PlaceholdersWidget placeholdersWidget;
	private PainterImage painterImage;
	private ObjectListComboBox<CssTheme> themeCb;

	public UiFileEditor(final File workDir, DirtyHandler dirtyHandler) {
		super(tr("admin.config.ui-file-editor"));
		this.workDir = workDir;

		// style

		CssTheme theme = CssTheme.Detault;
		try {
			theme = ConfigXmlReader.readtheme(workDir);
		} catch (JDOMException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		themeCb = new ObjectListComboBox<ConfigXmlWriter.CssTheme>(CssTheme.values()) {
			@Override
			protected WString render(CssTheme t) {
				return new WString(t.name());
			}
		};
		themeCb.setCurrentObject(theme);

		// genome

		painterImage = new PainterImage(workDir);

		File imageFile = new File(workDir, "genome_0.png");
		if (imageFile.exists()) {
			painterImage.setImageFile(imageFile);
		}

		fileUpload.getWFileUpload().setFilters(".png");
		fileUpload.uploadedFile().addListener(fileUpload, new Signal1.Listener<File>() {
			public void trigger(File file) {
				File genomeFile = new File(UiFileEditor.this.workDir, "genome_0.png");
				genomeFile.delete();
				try {
					FileUtils.copyFile(file, genomeFile);
				} catch (IOException e) {
					e.printStackTrace();
				}

				painterImage.setImageFile(genomeFile);
				painterImage.update();
			}
		});

		Genome genome = ConfigXmlReader.readGenome(workDir);
		if (genome != null) {
			setValue(genomeColor, genome.color);
			setValue(genomeStartLE, genome.genomeStart);
			setValue(genomeEndLE, genome.genomeEnd);
			setValue(imageStartLE, genome.imageStart);
			setValue(imageEndLE, genome.imageEnd);
			painterImage.setValues(genome.imageStart, genome.imageEnd);
		}

		imageStartLE.setValidator(createIntValidator());
		imageEndLE.setValidator(createIntValidator());
		genomeStartLE.setValidator(createIntValidator());
		genomeEndLE.setValidator(createIntValidator());

		imageStartLE.changed().addListener(imageStartLE, new Signal.Listener() {
			public void trigger() {
				if (!imageStartLE.getText().isEmpty()
						&& !imageEndLE.getText().isEmpty()) {
					painterImage.setValues(Integer.valueOf(imageStartLE.getText()),
							Integer.valueOf(imageEndLE.getText()));
				}
			}
		});
		imageEndLE.changed().addListener(imageEndLE, new Signal.Listener() {
			public void trigger() {
				if (!imageStartLE.getText().isEmpty()
						&& !imageEndLE.getText().isEmpty()) {
					painterImage.setValues(Integer.valueOf(imageStartLE.getText()),
							Integer.valueOf(imageEndLE.getText()));
				}
			}
		});

		Map<String, XmlMsgNode> messages = new HashMap<String, XmlUtils.XmlMsgNode>();
		try {
			messages = XmlUtils.readMessages(new File(workDir, XmlUtils.STRINGS_XML_FILE_NAME));
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// file not always there.
			//e.printStackTrace(); 
		}
		placeholdersWidget = new PlaceholdersWidget(messages, dirtyHandler);

		bindWidget("theme", themeCb);
		bindWidget("image", painterImage);
		bindWidget("image-start", imageStartLE);
		bindWidget("image-end", imageEndLE);
		bindWidget("genome-start", genomeStartLE);
		bindWidget("genome-end", genomeEndLE);
		bindWidget("genome-color", genomeColor);
		bindWidget("example-sequence", exapmleSequenceT);
		bindWidget("authors", authorsT);
		bindWidget("introduction", introductionT);

		bindWidget("file-upload", fileUpload);
		bindWidget("placeholders", placeholdersWidget);

		String allGenomes = "";
		for(File f: workDir.listFiles())
			if (f.getName().startsWith("genome_"))
				allGenomes += f.getName() + " ";
		bindString("uploaded-genomes", allGenomes);

		painterImage.cordinatesChanged.addListener(painterImage, new Signal2.Listener<Integer, Integer>() {
			public void trigger(Integer start, Integer end) {
				imageStartLE.setText(start.toString());
				imageEndLE.setText(end.toString());
			}
		});
	}

	private WIntValidator createIntValidator() {
		WIntValidator validator = new WIntValidator(0, Integer.MAX_VALUE);
		validator.setMandatory(true);
		return validator;
	}

	public boolean save() {
		if (validate() && placeholdersWidget.validate()) {
			Genome genome = new Genome();
			genome.imageStart = Integer.valueOf(imageStartLE.getText());
			genome.imageEnd = Integer.valueOf(imageEndLE.getText());
			genome.genomeStart = Integer.valueOf(genomeStartLE.getText());
			genome.genomeEnd = Integer.valueOf(genomeEndLE.getText());
			genome.color = genomeColor.getText();

			try {
				ConfigXmlWriter.writeGenome(workDir, genome);
				ConfigXmlWriter.writeTheme(workDir, themeCb.getCurrentObject());
			} catch (JDOMException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}

			placeholdersWidget.save(workDir);
		} else {
			return false;
		}

		return true;
	}

	// Classes:

	private static class PlaceholdersWidget extends WTable {
		private List<PlaceholderWidget> placeholderWidgets = new ArrayList<UiFileEditor.PlaceholderWidget>();
		private DirtyHandler dirtyHandler;

		public PlaceholdersWidget(final Map<String, XmlMsgNode> messages, final DirtyHandler dirtyHandler) {
			this.dirtyHandler = dirtyHandler;

			WPushButton add = new WPushButton("Add", getElementAt(0, 0));

			add.clicked().addListener(add, new Listener() {
				public void trigger() {
					addVaribale(new XmlMsgNode());
					dirtyHandler.increaseDirty();
				}
			});

			for (XmlMsgNode m: messages.values()) {
				addVaribale(m);
			}
		}

		public boolean save(File workDir) {
			if(!validate())
				return false;

			Map<String, XmlMsgNode> messages = new HashMap<String, XmlUtils.XmlMsgNode>();
			for (PlaceholderWidget pw: placeholderWidgets) 
				messages.put(pw.node.id, pw.node);

			try {
				XmlUtils.writeMessages(new File(workDir, XmlUtils.STRINGS_XML_FILE_NAME),
						messages);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (JDOMException e) {
				e.printStackTrace();
				return false;
			}
			
			return true;
		}

		public boolean validate() {
			return true;
		}

		private void addVaribale(final XmlMsgNode node) {
			final WTableRow row = insertRow(getRowCount() - 1);
			final PlaceholderWidget placeholder = new PlaceholderWidget(row, node);
			dirtyHandler.connect(placeholder.infoT);
			placeholderWidgets.add(placeholder);

			final WPushButton editB = new WPushButton("Edit", row.elementAt(1));
			final WPushButton removeB = new WPushButton("X", row.elementAt(2));

			editB.clicked().addListener(editB, new Signal.Listener() {				
				public void trigger() {
					final StandardDialog d = new StandardDialog("Edit placeholder value", true);
					Template template = new Template(tr("admin.config.ui-file-editor.placeholders"), 
							d.getContents());

					final NodeValueEditor valueT = new NodeValueEditor(node);
					final WLineEdit idLE = new WLineEdit(node.id);

					idLE.setValidator(new WValidator(true));

					template.bindWidget("id", idLE);
					template.bindWidget("value", valueT);

					valueT.resize(600, 390);
					d.resize(650, 550);
					d.finished().addListener(d, new Signal1.Listener<WDialog.DialogCode>() {
						public void trigger(WDialog.DialogCode arg) {
							if (arg == WDialog.DialogCode.Accepted) {
								placeholder.node.value = valueT.getText();
								placeholder.node.id = idLE.getText();
								placeholder.node.valueType = valueT.getValueType();
								placeholder.infoT.setText(idLE.getText());
								dirtyHandler.increaseDirty();
							}
						}
					});
				}
			});

			removeB.clicked().addListener(removeB, new Listener() {
				public void trigger() {
					placeholderWidgets.remove(placeholder);
					row.getTable().deleteRow(row.getRowNum());
					dirtyHandler.increaseDirty();
				}
			});
		}
	}

	private static class PlaceholderWidget {
		WText infoT;
		XmlMsgNode node;
		public PlaceholderWidget(WTableRow row, XmlMsgNode node) {
			this.node = node;
			infoT = new WText(node.id.isEmpty() ? "(Empty)" : node.id, 
					row.elementAt(0));
		}
	}

	private static class NodeValueEditor extends WTabWidget {
		WTextEdit xmlEditor = new WTextEdit();
		WTextArea fastaEditor = new WTextArea();
		public NodeValueEditor(XmlMsgNode node) {
			WContainerWidget fastaEditorC = new WContainerWidget();
			fastaEditorC.addWidget(fastaEditor);
			fastaEditorC.addWidget(new WText("Fasta sequences are automatically html encoded, do not warry about that. " +
					"You can not use xml editor for fasta file because it will replace \\n to <p>"));
			
			xmlEditor.setText(node.value);
			fastaEditor.setText(node.value);

			addTab(xmlEditor, "XML Editor");
			addTab(fastaEditorC, "FASTA Editor");

			setCurrentIndex(node.valueType.getCode());

			xmlEditor.resize(600, 380);
			fastaEditor.resize(600, 380);

			currentChanged().addListener(this, new Signal1.Listener<Integer>() {
				public void trigger(Integer tab) {
					if (tab == 0) // move from fasta to xml
						xmlEditor.setText(fastaEditor.getText());
					else if (tab == 1)
						fastaEditor.setText(xmlEditor.getText());
				}
			});

			fastaEditor.changed().addListener(fastaEditor, new Signal.Listener() {
				public void trigger() {
					XMLOutputter xmlOutput = new XMLOutputter();
					fastaEditor.setText(xmlOutput.
							escapeElementEntities(fastaEditor.getText()));
				}
			});
		}

		String getText() {
			int tab = getCurrentIndex();
			if (tab == 0)
				return xmlEditor.getText();
			else if (tab == 1) 
				return fastaEditor.getText();
			else
				return null;
		}

		ValueType getValueType() {
			return ValueType.fromCode(getCurrentIndex());
		}
	}
	private static class PainterImage extends WPaintedWidget {
		private File imageFile;
		private WPainterPath path = new WPainterPath();
		private Coordinates coordinatesStart;
		private Coordinates coordinatesEnd;
		// arg1 = image start arg2 = image end.
		private Signal2<Integer, Integer> cordinatesChanged = new Signal2<Integer, Integer>();
		private File workDir;

		public PainterImage(File workDir) {
			this.workDir = workDir;
			resize(639, 200);

			mouseWentDown().addListener(this,
					new Signal1.Listener<WMouseEvent>() {
				public void trigger(WMouseEvent e) {
					coordinatesStart = e.getWidget();
				}
			});
			mouseWentUp().addListener(this,
					new Signal1.Listener<WMouseEvent>() {
				public void trigger(WMouseEvent e) {
					path = new WPainterPath();
					coordinatesEnd = e.getWidget();
					if (coordinatesStart == null || coordinatesEnd == null
							|| coordinatesStart.x > coordinatesEnd.x)
						return;
					path.addRect(coordinatesStart.x, 0, 
							coordinatesEnd.x - coordinatesStart.x, 200);
					path.closeSubPath();
					update();
					cordinatesChanged.trigger(coordinatesStart.x, coordinatesEnd.x);
				}
			});
		}

		public void setImageFile(File imageFile) {
			this.imageFile = imageFile;
		}

		public void setValues(int start, int end) {
			path = new WPainterPath();
			coordinatesStart = new Coordinates(start, 0);
			coordinatesEnd = new Coordinates(end, 0);
			path.addRect(coordinatesStart.x, 0, 
					coordinatesEnd.x - coordinatesStart.x, 200);
			update();
		}

		// FIXME:HACK: counter is used to create a unique url for every image request so FileServlet::doGet is called. 
		int counter = 0;
		@Override
		protected void paintEvent(WPaintDevice paintDevice) {
			WPainter painter = new WPainter(paintDevice);

			if (imageFile != null) {
				String url = FileServlet.getFileEditorUrl(
						workDir.getName()) + imageFile.getName() 
						+ "&" + "counter=" + counter;
				counter++;
				Image image = new Image(url, imageFile.getAbsolutePath());
				painter.drawImage(0.0, 0.0, image);
			}
			painter.setBrush(new WBrush(new WColor(50, 150, 50, 50)));
			painter.drawPath(path);

		}
	}
}

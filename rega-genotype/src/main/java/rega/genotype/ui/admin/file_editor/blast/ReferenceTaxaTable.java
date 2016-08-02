package rega.genotype.ui.admin.file_editor.blast;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses.Taxus;
import rega.genotype.BlastAnalysis;
import rega.genotype.BlastAnalysis.ReferenceTaxus;
import rega.genotype.BlastAnalysis.Region;
import rega.genotype.ui.framework.widgets.Dialogs;
import rega.genotype.ui.framework.widgets.DirtyHandler;
import rega.genotype.ui.framework.widgets.InPlaceEdit;
import rega.genotype.ui.framework.widgets.ObjectListComboBox;
import eu.webtoolkit.jwt.ItemFlag;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WDialog.DialogCode;
import eu.webtoolkit.jwt.WIntValidator;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WTableCell;
import eu.webtoolkit.jwt.WTableColumn;
import eu.webtoolkit.jwt.WTableRow;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WValidator;
import eu.webtoolkit.jwt.WValidator.State;

/**
 * Edit blast.xml reference taxa
 * 
 * @author michael
 */
public class ReferenceTaxaTable extends WTable {
	// model
	// <column, regionId> horizontal header data
	private Map<WTableColumn, String> regionHeaderMap = new HashMap<WTableColumn, String>();
	// vertical header data
	private Map<WTableRow, ReferenceTaxus> taxusHeaderMap = new HashMap<WTableRow, ReferenceTaxus>();
	private Map<WTableCell, Region> regionMap = new HashMap<WTableCell, Region>();
	private BlastAnalysis analysis;

	// view
	private Map<WTableRow, ObjectListComboBox<Taxus>> taxusWidgetMap =
			new HashMap<WTableRow, ObjectListComboBox<Taxus>>();

	private WPushButton addTaxusB = new WPushButton("+");
	private WPushButton addRegionB = new WPushButton("+");
	private DirtyHandler dirtyHandler = new DirtyHandler();

	public ReferenceTaxaTable(final BlastAnalysis analysis) {
		this.analysis = analysis;

		setHeaderCount(1, Orientation.Horizontal);
		setHeaderCount(1, Orientation.Vertical);

		setStyleClass("reference-taxa-table");

		getElementAt(0, 0).addWidget(new WText());

		getElementAt(0, 1).addWidget(addRegionB);
		getElementAt(1, 0).addWidget(addTaxusB);

		// read
		for (ReferenceTaxus taxus: analysis.getSortedReferenceTaxus()){
			WTableRow row = getTaxusRow(taxus.getTaxus());
			if (row == null)
				row = createRowHeader(taxus);

			for(Region region : taxus.getRegions()) {
				WTableColumn column = getRegionColumn(region.getName());
				if (column == null) {
					column = insertColumn(getColumnCount() - 1);
					getElementAt(0, column.getColumnNum()).addWidget(
							createRegionHeaderWidget(region.getName(), column));
					regionHeaderMap.put(column, region.getName());
				}

				createRegionWidget(row.getRowNum(), column.getColumnNum(), region);
			}
		}
		// fill empty cells
		for (int r = 1; r < getRowCount() - 1; ++r)
			for (int c = 1; c < getColumnCount() -1; ++c)
				if (!regionMap.containsKey(getElementAt(r, c)))
					createRegionWidget(r, c, 
							new Region(regionHeaderMap.get(getColumnAt(c)), 0, 0));

		//signals
		addRegionB.clicked().addListener(addRegionB, new Signal.Listener() {
			public void trigger() {
				WTableColumn column = insertColumn(getColumnCount() - 1);
				getElementAt(0, column.getColumnNum()).addWidget(createRegionHeaderWidget("", column));

				for (int r = 1; r < getRowCount() -1; ++r){
					createRegionWidget(r, column.getColumnNum(),
							new Region(regionHeaderMap.get(column), 0, 0));
				}
				dirtyHandler.increaseDirty();
			}
		});
		
		addTaxusB.clicked().addListener(addTaxusB, new Signal.Listener() {
			public void trigger() {
				if (analysis.getOwner().getAllTaxa().isEmpty()) {
					Dialogs.infoDialog("Info",
							"There is no taxa in current tool. Add taxa before creating reference taxus.");
					return;
				}
				ReferenceTaxus refTaxus = new ReferenceTaxus(nextTaxus(), taxusHeaderMap.size());
				WTableRow row = createRowHeader(refTaxus);
				for (int c = 1; c < getColumnCount() -1; ++c){
					createRegionWidget(row.getRowNum(), c,
							new Region(regionHeaderMap.get(getColumnAt(c)), 0, 0));
				}

				if(nextTaxus() == null) // all taxa has ref taxus.
					addTaxusB.disable();
				dirtyHandler.increaseDirty();
			}
		});
	}

	private WTableColumn getRegionColumn(String regionName) {
		for ( Map.Entry<WTableColumn, String> e: regionHeaderMap.entrySet()) 
			if(e.getValue().equals(regionName))
				return e.getKey();

		return null;
	}

	private WTableRow getTaxusRow(String taxusId) {
		for ( Map.Entry<WTableRow, ReferenceTaxus> e: taxusHeaderMap.entrySet()) 
			if(e.getValue().getTaxus().equals(taxusId))
				return e.getKey();

		return null;
	}
	private WContainerWidget createRegionHeaderWidget(String regionName, final WTableColumn column) {
		WContainerWidget c = new WContainerWidget();

		// regionNameE
		final InPlaceEdit regionNameE = new InPlaceEdit(regionName, new WLineEdit(), c);
		regionNameE.setInline(true);
		regionNameE.getEdit().setWidth(new WLength(80));
		regionNameE.setEmptyText("(Empty)");
		regionNameE.setButtonsEnabled(false);

		//signals
		regionNameE.getEdit().setValidator(new WValidator() {
			@Override
			public Result validate(String input) {
				if (input.isEmpty())
					return new Result(State.InvalidEmpty);
				// check unique.
				for (Map.Entry<WTableColumn, String> e: regionHeaderMap.entrySet())
					if (e.getValue().equals(input) && !e.getKey().equals(column))
						return new Result(State.Invalid);

				return new Result(State.Valid);
			}
		});

		regionNameE.valueChanged().addListener(regionNameE, new Signal.Listener() {
			public void trigger() {
				if (regionNameE.getEdit().validate() == State.Valid) {
					regionHeaderMap.put(column, regionNameE.getEdit().getValueText());
					dirtyHandler.increaseDirty();
				} else 
					regionNameE.getTextWidget().clicked().trigger(null);
			}
		});

		//remove button
		WPushButton removeB = new WPushButton("-" ,c);
		removeB.clicked().addListener(removeB, new Signal.Listener() {
			public void trigger() {
				// remove a column.
				for (int r = 1; r < getRowCount() - 1; r++)
					regionMap.remove(getElementAt(r, column.getColumnNum()));
				regionHeaderMap.remove(column);
				deleteColumn(column.getColumnNum());
				dirtyHandler.increaseDirty();
			}
		});

		return c;
	}

	private void createRegionWidget(int row, int column, final Region region) {
		final WText text = new WText(
				region.getBegin() + " -> " + region.getEnd());

		final WTableCell cell = getElementAt(row, column);
		regionMap.put(cell, region);
		cell.addWidget(text);

		cell.clicked().addListener(this, new Signal.Listener() {
			public void trigger() {
				final WDialog d = new WDialog("Edit region");
				d.show();

				String regionName = regionHeaderMap.get(cell.getTableColumn());
				final ReferenceTaxus referenceTaxus = taxusHeaderMap.get(cell.getTableRow());
				final AbstractSequence s = analysis.getOwner().
						getAlignment().getSequence(referenceTaxus.getTaxus());
				final String sequence = s != null ? s.getSequence().toUpperCase() : "";

				new WText("Enter start and end of the region " + regionName 
						+ " in taxus " + referenceTaxus.getTaxus(),
						d.getContents()).setPadding(new WLength(10), Side.Top);

				String start = "" + region.getBegin();
				String end = "" + region.getEnd();
				final WLineEdit startLE = new WLineEdit(start);
				final WLineEdit endLE = new WLineEdit(end);
				startLE.setValidator(createRegionValidator());
				endLE.setValidator(createRegionValidator());

				// show the sequence 
				final WTable sequenceTable = new WTable(d.getContents());
				sequenceTable.addStyleClass("sequence-table");
				int lineWidth = 50;
				for (int r = 0; r < sequence.length(); r += lineWidth){
					sequenceTable.getElementAt(r, 0).addWidget(
							new WText((r + 1) + "" ));
					sequenceTable.getElementAt(r, 1).addWidget(
							new WText(sequence.substring(r, 
									Math.min((r + lineWidth) - 1, sequence.length()))));
				}
				sequenceTable.setHeight(new WLength(200));
				
				// choose start, end.
				final WTable layout = new WTable(d.getContents());
				layout.setStyleClass("form-table");
				layout.getElementAt(0, 0).addWidget(new WText("Start"));
				layout.getElementAt(0, 1).addWidget(startLE);
				layout.getElementAt(1, 0).addWidget(new WText("End"));
				layout.getElementAt(1, 1).addWidget(endLE);

				final WPushButton okB = new WPushButton("OK", d.getFooter());
				final WPushButton cancelB = new WPushButton("Cancel", d.getFooter());

				startLE.keyWentUp().addListener(startLE, new Signal.Listener() {
					public void trigger() {
						okB.setEnabled(startLE.validate() == WValidator.State.Valid 
								&& endLE.validate() == WValidator.State.Valid);
					}
				});

				endLE.keyWentUp().addListener(endLE, new Signal.Listener() {
					public void trigger() {
						okB.setEnabled(startLE.validate() == WValidator.State.Valid 
								&& endLE.validate() == WValidator.State.Valid);
					}
				});

				okB.clicked().addListener(okB, new Signal.Listener() {
					public void trigger() {
						d.accept();
					}
				});

				cancelB.clicked().addListener(cancelB, new Signal.Listener() {
					public void trigger() {
						d.reject();
					}
				});

				d.finished().addListener(d,  new Signal1.Listener<WDialog.DialogCode>() {
					public void trigger(DialogCode arg) {
						if (arg == DialogCode.Accepted) {
							region.setBegin(Integer.parseInt(startLE.getText()));
							region.setEnd(Integer.parseInt(endLE.getText()));
							text.setText(region.getBegin() + " -> " + region.getEnd());

							dirtyHandler.increaseDirty();
						}
						d.remove();
					}
				});

			}
		});
	}

	private WTableRow createRowHeader(final ReferenceTaxus refTaxus) {
		final WTableRow row = insertRow(getRowCount() - 1);

		// taxusCB
		Taxus currentSelectedTaxus = null;
		List<Taxus> taxa = analysis.getOwner().getAllTaxa();
		for (Taxus t: taxa) 
			if (refTaxus != null && t.getId().equals(refTaxus.getTaxus()))
				currentSelectedTaxus = t;

		final ObjectListComboBox<Taxus> taxusCB = new ObjectListComboBox<Taxus>(taxa) {
			@Override
			protected WString render(Taxus t) {
				return new WString(t.getId());
			}
		};
		if (currentSelectedTaxus != null) 
			taxusCB.setCurrentObject(currentSelectedTaxus);
		else 
			taxusCB.setCurrentIndex(0);

		taxusCB.changed().addListener(taxusCB, new Signal.Listener() {
			public void trigger() {
				Taxus selectedTaxus = taxusCB.getCurrentObject();
				refTaxus.setTaxus(selectedTaxus.getId());
				updateTaxaCBs();
				dirtyHandler.increaseDirty();
			}
		});
		updateTaxaCBs();

		// layout
		WContainerWidget c = new WContainerWidget();
		c.addWidget(taxusCB);
		// remove row
		WPushButton removeB = new WPushButton("-" ,c);
		removeB.clicked().addListener(removeB, new Signal.Listener() {
			public void trigger() {
				for (int c = 1; c < getColumnCount() - 1; ++c)
					regionMap.remove(getElementAt(row.getRowNum(), c));
				taxusHeaderMap.remove(row);
				deleteRow(row.getRowNum());
				addTaxusB.enable();
				dirtyHandler.increaseDirty();
			}
		});

		getElementAt(row.getRowNum(), 0).addWidget(c);
		taxusHeaderMap.put(row, refTaxus);
		taxusWidgetMap.put(row, taxusCB);

		return row;
	}

	private WIntValidator createRegionValidator() {
		final WIntValidator validator = new WIntValidator(0, Integer.MAX_VALUE);
		validator.setMandatory(true);
		return validator;
	}

	boolean contains(Collection<ReferenceTaxus> refTaxa, String taxusId){
		for(ReferenceTaxus refTaxus: refTaxa)
			if (refTaxus.getTaxus().equals(taxusId)) 
				return true;

		return false;
	}

	private String nextTaxus() {
		Collection<ReferenceTaxus> referenceTaxa = taxusHeaderMap.values();
		for (Taxus t: analysis.getOwner().getAllTaxa())
			if (!contains(referenceTaxa, t.getId()))
				return t.getId();
		return null;
	}

	private void updateTaxaCBs() {
		for (ObjectListComboBox<Taxus> cb: taxusWidgetMap.values()) {
			updateTaxaCB(cb);
		}
	}

	void updateTaxaCB(ObjectListComboBox<Taxus> taxusCB) {
		int currentIndex = taxusCB.getCurrentIndex();
		Collection<ReferenceTaxus> referenceTaxa = taxusHeaderMap.values();
		for (int r = 0; r < taxusCB.getCount(); r++) {
			if (currentIndex != r 
					&& contains(referenceTaxa, taxusCB.getModel().getObject(r).getId())) {
				taxusCB.getModel().setFlags(r, EnumSet.noneOf(ItemFlag.class));
			} else {
				taxusCB.getModel().setFlags(r, EnumSet.of(ItemFlag.ItemIsSelectable));
			}
		}
		taxusCB.getModel().refresh();
		taxusCB.setCurrentIndex(currentIndex);
	}

	public DirtyHandler getDirtyHandler() {
		return dirtyHandler;
	}

	public void save() {
		analysis.clearReferenceTaxus();

		for (int r = 1; r < getRowCount() - 1; ++r) {
			ReferenceTaxus referenceTaxus = taxusHeaderMap.get(getRowAt(r));
			referenceTaxus.clearRegions();
			for (int c = 1; c < getColumnCount() - 1; ++c) {
				String regionId = regionHeaderMap.get(getColumnAt(c));
				Region region = regionMap.get(getElementAt(r, c));
				region.setName(regionId);
				referenceTaxus.addRegion(region);
			}
			referenceTaxus.setPriority(r); // priority is determined by the order of reference taxa in blast.xml
			analysis.addReferenceTaxus(referenceTaxus);
		}
	}
}

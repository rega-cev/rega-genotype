package rega.genotype.ui.framework.widgets;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.Signal3;
import eu.webtoolkit.jwt.StringUtils;
import eu.webtoolkit.jwt.ViewItemRenderFlag;
import eu.webtoolkit.jwt.WAbstractItemDelegate;
import eu.webtoolkit.jwt.WAbstractItemModel;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WCompositeWidget;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WItemDelegate;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WTableView;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;

/**
 * A container widget which provides a view implementation of a {@link WTable}
 * <p>
 * 
 * The {@link WTableView} operates on a {@link WAbstractItemModel} provided via
 * {@link WTableView#setModel(WAbstractItemModel model) setModel()}. Data in the
 * model is rendered using an HTML <code>&lt;table&gt;</code>, and the model
 * reacts to any model changes. You may use CSS stylesheets for
 * <code>&lt;table&gt;</code>, <code>&lt;tr&gt;</code>, and
 * <code>&lt;td&gt;</code> elements to provide style to the table.
 * <p>
 * <p>
 * <i><b>Note: </b>This view widget is a work-in-progress. Support for column
 * resizing, smart rendering, and proper vertical scroll bar support is still
 * lacking. When support for these features is added in the future, an HTML
 * <code>&lt;table&gt;</code> element will still be used. </i>
 * </p>
 */
public class TableView extends WCompositeWidget {
	/**
	 * Constructor.
	 */
	public TableView(WContainerWidget parent) {
		super(parent);
		this.columns_ = new ArrayList<TableView.ColumnInfo>();
		this.setImplementation(this.table_ = new WTable());
		this.itemDelegate_ = new WItemDelegate();
		this.model_ = null;
	}

	/**
	 * Constructor.
	 * <p>
	 * Calls {@link #WTableView(WContainerWidget parent)
	 * this((WContainerWidget)null)}
	 */
	public TableView() {
		this((WContainerWidget) null);
	}

	/**
	 * Destructor.
	 */
	public void remove() {
		super.remove();
	}

	/**
	 * Sets the model.
	 * <p>
	 * If a previous model was set, it is not deleted.
	 */
	public void setModel(WAbstractItemModel model) {
		this.table_.clear();
		if (!(model != null)) {
			return;
		}
		this.model_ = model;
		for (int r = 0; r < this.model_.getRowCount(); r++) {
			this.table_.insertRow(r);
		}
		for (int c = 0; c < model.getColumnCount(); c++) {
			this.table_.insertColumn(c);
		}
		for (int c = 0; c < model.getColumnCount(); c++) {
			Object header = model.getHeaderData(c);
			this.table_.getElementAt(0, c).addWidget(
					new WText(StringUtils.asString(header)));
		}
		this.table_.setHeaderCount(1);
		for (int r = 0; r < model.getRowCount(); r++) {
			for (int c = 0; c < model.getColumnCount(); c++) {
				WWidget w = this.getWidget(r, c);
				if (w != null) {
					this.table_.getElementAt(r + this.table_.getHeaderCount(),
							c).addWidget(w);
				}
			}
		}
		this.model_.columnsInserted().addListener(this,
				new Signal3.Listener<WModelIndex, Integer, Integer>() {
					public void trigger(WModelIndex e1, Integer e2, Integer e3) {
						TableView.this.columnsInserted(e1, e2, e3);
					}
				});
		this.model_.columnsRemoved().addListener(this,
				new Signal3.Listener<WModelIndex, Integer, Integer>() {
					public void trigger(WModelIndex e1, Integer e2, Integer e3) {
						TableView.this.columnsRemoved(e1, e2, e3);
					}
				});
		this.model_.rowsInserted().addListener(this,
				new Signal3.Listener<WModelIndex, Integer, Integer>() {
					public void trigger(WModelIndex e1, Integer e2, Integer e3) {
						TableView.this.rowsInserted(e1, e2, e3);
					}
				});
		this.model_.rowsRemoved().addListener(this,
				new Signal3.Listener<WModelIndex, Integer, Integer>() {
					public void trigger(WModelIndex e1, Integer e2, Integer e3) {
						TableView.this.rowsRemoved(e1, e2, e3);
					}
				});
		this.model_.dataChanged().addListener(this,
				new Signal2.Listener<WModelIndex, WModelIndex>() {
					public void trigger(WModelIndex e1, WModelIndex e2) {
						TableView.this.dataChanged(e1, e2);
					}
				});
		this.model_.headerDataChanged().addListener(this,
				new Signal3.Listener<Orientation, Integer, Integer>() {
					public void trigger(Orientation e1, Integer e2, Integer e3) {
						TableView.this.headerDataChanged(e1, e2, e3);
					}
				});
	}

	/**
	 * Sets the default item delegate.
	 * <p>
	 * The previous delegate is removed but not deleted.
	 * <p>
	 * The default item delegate is a {@link WItemDelegate}.
	 */
	public void setItemDelegate(WAbstractItemDelegate delegate) {
		this.itemDelegate_ = delegate;
	}

	/**
	 * Returns the default item delegate.
	 * <p>
	 * 
	 * @see TableView#setItemDelegate(WAbstractItemDelegate delegate)
	 */
	public WAbstractItemDelegate getItemDelegate() {
		return this.itemDelegate_;
	}

	/**
	 * Sets the delegate for a column.
	 * <p>
	 * The previous delegate is removed but not deleted.
	 * <p>
	 * 
	 * @see TableView#setItemDelegate(WAbstractItemDelegate delegate)
	 */
	public void setItemDelegateForColumn(int column,
			WAbstractItemDelegate delegate) {
		this.columnInfo(column).itemDelegate_ = delegate;
	}

	/**
	 * Returns the delegate for a column.
	 * <p>
	 * 
	 * @see TableView#setItemDelegateForColumn(int column,
	 *      WAbstractItemDelegate delegate)
	 */
	public WAbstractItemDelegate getItemDelegateForColumn(int column) {
		return this.columnInfo(column).itemDelegate_;
	}

	/**
	 * Returns the delegate for rendering an item.
	 * <p>
	 * 
	 * @see TableView#setItemDelegateForColumn(int column,
	 *      WAbstractItemDelegate delegate)
	 * @see TableView#setItemDelegate(WAbstractItemDelegate delegate)
	 */
	public WAbstractItemDelegate getItemDelegate(WModelIndex index) {
		WAbstractItemDelegate result = this.getItemDelegateForColumn(index
				.getColumn());
		return result != null ? result : this.itemDelegate_;
	}

	/**
	 * Returns the table used for rendering the model.
	 */
	protected WTable getTable() {
		return this.table_;
	}

	private WTable table_;

	static class ColumnInfo {
		public WAbstractItemDelegate itemDelegate_;

		public ColumnInfo(TableView view, WApplication app, int column) {
			this.itemDelegate_ = null;
		}
	}

	private WAbstractItemDelegate itemDelegate_;
	private List<TableView.ColumnInfo> columns_;
	private WAbstractItemModel model_;

	private void columnsInserted(WModelIndex index, int firstColumn,
			int lastColumn) {
		for (int i = firstColumn; i <= lastColumn; i++) {
			this.table_.insertColumn(i);
		}
	}

	private void columnsRemoved(WModelIndex index, int firstColumn,
			int lastColumn) {
		for (int i = lastColumn; i >= firstColumn; i--) {
			this.table_.deleteColumn(i);
		}
	}

	private void rowsInserted(WModelIndex index, int firstRow, int lastRow) {
		for (int i = firstRow; i <= lastRow; i++) {
			this.table_.insertRow(i + this.table_.getHeaderCount());
		}
	}

	private void rowsRemoved(WModelIndex index, int firstRow, int lastRow) {
		for (int i = lastRow; i >= firstRow; i--) {
			this.table_.deleteRow(i - this.table_.getHeaderCount());
		}
	}

	private void dataChanged(WModelIndex topLeft, WModelIndex bottomRight) {
		for (int i = topLeft.getRow(); i <= bottomRight.getRow(); i++) {
			for (int j = topLeft.getColumn(); j <= bottomRight.getColumn(); j++) {
				this.table_.getElementAt(i + this.table_.getHeaderCount(), j)
						.clear();
				WWidget w = this.getWidget(i, j);
				if (w != null) {
					this.table_.getElementAt(i + this.table_.getHeaderCount(),
							j).addWidget(w);
				}
			}
		}
	}

	private void headerDataChanged(Orientation orientation, int first, int last) {
		for (int c = first; c <= last; c++) {
			Object header = this.model_.getHeaderData(c);
			this.table_.getElementAt(0, c).clear();
			if (!(header == null)) {
				this.table_.getElementAt(0, c).addWidget(
						new WText(StringUtils.asString(header)));
			}
		}
	}

	private TableView.ColumnInfo columnInfo(int column) {
		while (column >= (int) this.columns_.size()) {
			this.columns_.add(new TableView.ColumnInfo(this, WApplication
					.getInstance(), column));
		}
		return this.columns_.get(column);
	}

	private WWidget getWidget(int row, int column) {
		WAbstractItemDelegate itemDelegate = this
				.getItemDelegateForColumn(column);
		if (!(itemDelegate != null)) {
			itemDelegate = this.itemDelegate_;
		}
		return itemDelegate.update((WWidget) null, this.model_.getIndex(row,
				column), EnumSet.noneOf(ViewItemRenderFlag.class));
	}
}

package rega.genotype.ui.framework.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.Key;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.WAbstractItemModel;
import eu.webtoolkit.jwt.WFormWidget;
import eu.webtoolkit.jwt.WKeyEvent;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLength.Unit;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WWebWidget;
import eu.webtoolkit.jwt.WWidget;

public class TableViewWithSearchHeader extends StandardTableView {

	// Fields:

	/** column, search str */
	private final Signal2<Integer, String> search = new Signal2<Integer, String>();
	private final HashMap<Integer, WFormWidget> headerWidgets = new HashMap<Integer, WFormWidget>();
	private final Signal1<WKeyEvent> keyWentUpInHeaderWidget = new Signal1<WKeyEvent>();

	// Constructor:

	public TableViewWithSearchHeader() {
		setHeaderHeight(new WLength(50));
	}

	public TableViewWithSearchHeader(boolean enabled) {
		if(enabled)
			setHeaderHeight(new WLength(50));
	}

	// Methods:

	/**
	 * @param model - SortFilterProxyForSearchHeaders enable filtering, 
	 * the src model is expected to be ShopTableModel to be able to call refresh. 
	 * 
	 */
	@Override
	public void setModel(WAbstractItemModel model) {
		super.setModel(model);
		// on search
		this.search().addListener(this, new Signal2.Listener<Integer, String>() {
			public void trigger(Integer column, String searchStr) {
				if(getModel() instanceof SortFilterProxyForSearchHeaders) {
					SortFilterProxyForSearchHeaders pModel = (SortFilterProxyForSearchHeaders)getModel();
					pModel.setHeaders(getHeaders());
					//pModel.sort(getSortColumn());
					if (getSelectedIndexes().isEmpty() && pModel.getRowCount() > 0) {
						select(pModel.getIndex(0, 0));
					}
				}
			}
		});
		for (int col = 0; col < model.getColumnCount(); ++col)
			setHeaderAlignment(col, AlignmentFlag.AlignTop, AlignmentFlag.AlignCenter);
	}

	/**
	 * set the width of the inner search widget 95% of the containing column
	 * and stretch with the column width changes.
	 * @param widget
	 */
	public static void setSearchWidgetWidth(WWebWidget widget) {
		widget.setWidth(new WLength(85, Unit.Percentage));
		widget.setInline(false);
	}

	/**
	 * default createExtraHeaderWidget generates a WLineEdit search line for the column.
	 * Note: this automatically adds the widget to HeaderWidgets Map (headerWidgets.put(column, edit)).
	 */
	@Override
	protected WWidget createExtraHeaderWidget(final int column) {
		final WLineEdit edit = new WLineEdit();
		return createExtraHeaderWidget(column, edit);
	}

	public WWidget createExtraHeaderWidget(final int column, final WFormWidget w) {
		headerWidgets.put(column, w);
		TableViewWithSearchHeader.setSearchWidgetWidth(w);
		w.clicked().preventPropagation();
		// search event
		w.keyWentUp().addListener(w, new Signal1.Listener<WKeyEvent>() {
			public void trigger(WKeyEvent arg) {
				if (arg.getKey() == Key.Key_Tab || arg.getKey() == Key.Key_Up ||
						arg.getKey() == Key.Key_Down) {
				} else {
					search.trigger(column, w.getValueText());
				}
				keyWentUpInHeaderWidget.trigger(arg);
			}
		});
		focusFirst();

		return w;
	}

	public Signal1<WKeyEvent> keyWentUpInHeaderWidget() {
		return keyWentUpInHeaderWidget;
	}

	public Signal2<Integer, String> search(){
		return search;
	}

	public HashMap<Integer, WFormWidget> getHeaderWidgets() {	
		return headerWidgets;
	}

	public List<String> getHeaders(){
		List<String> ans = new ArrayList<String>();
		for (int c = 0; c < getModel().getColumnCount(); ++c)
			if (headerWidgets.get(c) != null) {
				ans.add(headerWidgets.get(c).getValueText());
			} else
				ans.add("");

		return ans;
	}

	public void focusFirst(){
		if (!headerWidgets.isEmpty()){
			headerWidgets.get(0).setFocus();
			headerWidgets.get(0).setTabIndex(1);
		}
	}

}
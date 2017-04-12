package rega.genotype.ui.framework.widgets;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.List;

import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.ItemFlag;
import eu.webtoolkit.jwt.StringUtils;
import eu.webtoolkit.jwt.Utils;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WSortFilterProxyModel;

public class SortFilterProxyForSearchHeaders extends WSortFilterProxyModel{
	List<String> headers = null;

	public void setHeaders(List<String> headers) {
		if (headers.size() != getColumnCount())
			throw new RuntimeException("Must have a header per column!");
		this.headers = headers;
		invalidate();
	}

	@Override
	protected int compare(WModelIndex lhs, WModelIndex rhs) {
		
		if (headers != null) {
			 // Exact matches first.
			Object lo = lhs.getData(this.getSortRole());
			Object ro = rhs.getData(this.getSortRole());
			if (ro instanceof String && lo instanceof String){
				if (lo != null &&  headers.get(lhs.getColumn())!= null && 
						headers.get(lhs.getColumn()).toUpperCase().
						equals(lo.toString().toUpperCase()))
					return -1;
				else if (ro != null &&  headers.get(rhs.getColumn())!= null && 
						headers.get(rhs.getColumn()).toUpperCase().
						equals(ro.toString().toUpperCase()))
					return 1;
			}
		}
		return super.compare(lhs, rhs);
	}

	@Override
	protected boolean filterAcceptRow(int sourceRow, WModelIndex sourceParent) {
		if (headers != null) {
			for (int c = 0; c < headers.size(); c++){
				if (!headers.get(c).isEmpty()){
					String[] split = headers.get(c).split(" ");
					Object data = this.getSourceModel()
							.getIndex(sourceRow, c, sourceParent)
							.getData(getFilterRole());
					String dataString = null;
					if (data instanceof Number)
						// try to avoid filtering on dots etc, which StringUtils.asString() may introduce
						dataString = data.toString();
					else
						dataString = StringUtils.asString(data).toString();
					String dataUpperCase = normalizeToUpper(dataString);
					for (String h: split)
						if (!dataUpperCase.contains(normalizeToUpper(h)))
							return false;
				}
			}
		}

		return true;
	}

	private static String normalizeToUpper(String string) {
		return normalize(string).toUpperCase();
	}

	private static String normalize(String string) {
		return Normalizer.normalize(string, Normalizer.Form.NFD);
	}

	private static String styleOccurrences(String longDesc, String shortDesc){
		int current = 0;

		while(current != -1){
			current = longDesc.toUpperCase().indexOf(shortDesc, current);
			if( current != -1){
				longDesc = longDesc.substring(0, current) + "<b>" + longDesc.substring(current, longDesc.length());
				current+=shortDesc.length() + 3;
				longDesc = longDesc.substring(0, current) + "</b>" + longDesc.substring(current, longDesc.length());
				current+= 4;
			}
		}
		return longDesc;
	}

	public static String styleSelectedText(String longDesc, String shortDesc){
		if (longDesc == null | shortDesc == null)
			return "";

		longDesc = Utils.htmlEncode(longDesc);
		shortDesc = Utils.htmlEncode(shortDesc);
		String[] shortDescArr = shortDesc.split(" ");
		for (String s: shortDescArr){
			if (s.isEmpty())
				continue;
			longDesc = styleOccurrences(normalize(longDesc), normalizeToUpper(s));
		}

		return longDesc;
	}

	@Override
	public EnumSet<ItemFlag> getFlags(WModelIndex index) {
		EnumSet<ItemFlag> set = super.getFlags(index);
		set.add(ItemFlag.ItemIsXHTMLText);
		return set;
	}

	@Override
	public Object getData(WModelIndex index, int role) {
		int column = index.getColumn();
		if (role == ItemDataRole.DisplayRole){
			Object data = super.getData(index, role);
			if (headers != null && 
					!headers.get(column).isEmpty()){
				if (data instanceof String)
					return styleSelectedText((String) data,
							headers.get(column));
				else if (data instanceof Number)
					return styleSelectedText(String.valueOf(data), headers.get(column));
			} else if(data instanceof String && itemIsEncodable(index))
				return Utils.htmlEncode((String) super.getData(index, role));
		}

		return super.getData(index, role);
	}

	protected boolean itemIsEncodable(WModelIndex index) {
		return true;
	}
}

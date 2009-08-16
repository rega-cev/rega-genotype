/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.framework.widgets;

import java.util.ArrayList;

import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WWidget;

/**
 * Emulates an html unsorted list with WContainerWidgets
 * 
 * @author simbre1
 *
 */
public class WListContainerWidget extends WContainerWidget{
	ArrayList<WContainerWidget> list = new ArrayList<WContainerWidget>(); 

	public WListContainerWidget(WContainerWidget parent){
		super(parent);
		setList(true);
		setStyleClass("listContainer");
	}	
	
	public WContainerWidget addItem(WWidget widget) {
		WContainerWidget cw = addItem();
		widget.setId("");
		cw.addWidget(widget);
		return cw;
	}

	public WContainerWidget addItem() {
		return addItem(new WContainerWidget(this));
	}

	public WContainerWidget addItem(WContainerWidget cw){
		cw.setId("");
		list.add(cw);
		return cw;
	}
}

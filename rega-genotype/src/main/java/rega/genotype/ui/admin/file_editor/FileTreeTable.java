/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.TextFormat;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WIconPair;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTreeNode;
import eu.webtoolkit.jwt.WTreeNode.ChildCountPolicy;
import eu.webtoolkit.jwt.WTreeTable;
import eu.webtoolkit.jwt.WTreeTableNode;

public class FileTreeTable extends WTreeTable {

	private File path;
	private boolean showSize;
	private boolean showModifier;

	public FileTreeTable(File path, boolean showSize, 
			 boolean showModifier) {
		this(path, showSize, showModifier, null);
	}

	public FileTreeTable(File path, boolean showSize, 
			 boolean showModifier, WContainerWidget parent) {
		super(parent);
		this.path = path;
		this.showSize = showSize;
		this.showModifier = showModifier;

		if (showSize){
			addColumn("Size", new WLength(80));
			header(1).setStyleClass("fsize");
		}
		if (showModifier) {
			addColumn("Modified", new WLength(110));
			header(getColumnCount() -1).setStyleClass("date");
		}

		getTree().setSelectionMode(SelectionMode.SingleSelection);

		refresh();
	}

	public void refresh() {
		setTreeRoot(new FileTreeTableNode(path, showSize, showModifier), "File");
		
		getTreeRoot().expand();
		getTreeRoot().setNodeVisible(false);
		getTreeRoot().setChildCountPolicy(ChildCountPolicy.Enabled);
	}
	
	public FileTreeTableNode findNodeByPath(String path, FileTreeTableNode current){
		if (path.equals(current.getPath().getPath()))
			return current;
		if (!path.contains(current.getPath().getPath()))
			return null;

		current.expand();

		for(WTreeNode node: current.getChildNodes()){
			FileTreeTableNode fNode = (FileTreeTableNode)node;
			FileTreeTableNode next = findNodeByPath(path, fNode);
			if(next != null)
				return next;
		}

		return null;
	}

	public void setSelectedFile(String path) {
		if(path == null)
			return;

		FileTreeTableNode selected = findNodeByPath(path, (FileTreeTableNode)getTreeRoot());
		if(selected != null)
			getTree().select(selected);
		else
			System.err.println("File " + path + " not found.");
	}

	public File getCurrentFile() {
		for(WTreeNode node: getTree().getSelectedNodes()){
			FileTreeTableNode fNode = (FileTreeTableNode)node;
			return fNode.getPath(); // Assume SingleSelection
		}
		
		return null;
	}

	public Signal selctionChanged() {
		return getTree().itemSelectionChanged();
	}

	// classes

	public static class FileTreeTableNode extends WTreeTableNode {

		private boolean showSize;
		private boolean showModifier;

		public FileTreeTableNode(File path, final boolean showSize, final boolean showModifier) {
			super("", createIcon(path));
			path_ = path;
			this.showSize = showSize;
			this.showModifier = showModifier;

			getLabel().setTextFormat(TextFormat.PlainText);
			getLabel().setText(path_.getName());

			
			if (path.exists()) {
				if (!path.isDirectory()) {
					if (showSize) {
						long fsize = path.length();
						setColumnWidget(1, new WText(fsize + ""));
						getColumnWidget(1).setStyleClass("fsize");
					}
				} else
					setSelectable(false);

				if (showModifier) {
					SimpleDateFormat formatter = new SimpleDateFormat("M dd yyyy");
					setColumnWidget(2, new WText(formatter.format(new Date(path
							.lastModified()))));
					getColumnWidget(2).setStyleClass("date");
				}
			}
		}

		private File path_;

		public File getPath() {
			return path_;
		}

		@Override
		protected void populate() {
			if (path_.isDirectory()) {
				File[] files = path_.listFiles();
				if(files != null)// That can happen if the user has no permissions to use the file.
					for (File f : files)
						addChildNode(new FileTreeTableNode(f, showSize, showModifier));
			}
		}

		@Override
		protected boolean isExpandable() {
			if (!isPopulated()) {
				return path_.isDirectory();
			} else
				return super.isExpandable();
		}

		 private static WIconPair createIcon(File path) {
			 if (path.exists() && path.isDirectory())
				 return new WIconPair("pics/yellow-folder-closed.png",
						 "pics/yellow-folder-open.png", false);
			 else
				 return new WIconPair("pics/document.png",
						 "pics/yellow-folder-open.png", false);
		 }
	}
}
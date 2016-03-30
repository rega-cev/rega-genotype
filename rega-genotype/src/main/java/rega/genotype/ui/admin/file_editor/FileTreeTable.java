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

	public FileTreeTable(File path) {
		this(path, null);
	}

	public FileTreeTable(File path, WContainerWidget parent) {
		super(parent);

		addColumn("Size", new WLength(80));
		addColumn("Modified", new WLength(110));

		header(1).setStyleClass("fsize");
		header(2).setStyleClass("date");

		setTreeRoot(new FileTreeTableNode(path), "File");

		getTree().setSelectionMode(SelectionMode.SingleSelection);
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

		public FileTreeTableNode(File path) {
			super("", createIcon(path));
			path_ = path;

			getLabel().setTextFormat(TextFormat.PlainText);
			getLabel().setText(path_.getName());

			if (path.exists()) {
				if (!path.isDirectory()) {
					long fsize = path.length();
					setColumnWidget(1, new WText(fsize + ""));
					getColumnWidget(1).setStyleClass("fsize");
				} else
					setSelectable(false);

				SimpleDateFormat formatter = new SimpleDateFormat("M dd yyyy");

				setColumnWidget(2, new WText(formatter.format(new Date(path
						.lastModified()))));
				getColumnWidget(2).setStyleClass("date");
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
						addChildNode(new FileTreeTableNode(f));
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
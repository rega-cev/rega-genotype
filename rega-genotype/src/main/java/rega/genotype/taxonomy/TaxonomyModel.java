package rega.genotype.taxonomy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.WStandardItem;
import eu.webtoolkit.jwt.WStandardItemModel;

/**
 * Singleton, taxonomy hierarchy tree, created from uniprot taxonomy file: 
 * http://www.uniprot.org/taxonomy/?query=Viruses&format=tab
 * 
 * @author michael
 */
public class TaxonomyModel extends WStandardItemModel {
	public enum DataType{Taxon, Lineage}

	private static TaxonomyModel instance = null;

	private TaxonomyModel(){
		instance = new TaxonomyModel();
		File taxonomyFile = UpdateTaxonomyFileService.taxonomyFile();
		if (taxonomyFile.exists())
			instance.read(taxonomyFile);
	}
	public static synchronized TaxonomyModel getInstance() {
		if (instance == null) {
			new TaxonomyModel();
		}

		return instance;
	}

	private WStandardItem findChild(WStandardItem parent, String text) {
		for (int i = 0; i < parent.getRowCount(); ++i) {
			if (parent.getChild(i).getText().equals(text))
				return parent.getChild(i);
		}

		return null;
	}
	
	private void addItem(String taxon, String[] linage) {
		WStandardItem parent = getInvisibleRootItem();
		for(String l: linage) {
			WStandardItem nextParent = findChild(parent, l);
			if (nextParent == null
					|| nextParent.getData(ItemDataRole.UserRole).equals(DataType.Taxon)) { 
				// create path
				WStandardItem item = new WStandardItem(l);
				item.setData(DataType.Lineage, ItemDataRole.UserRole);
				List<WStandardItem> items = new ArrayList<WStandardItem>();
				items.add(item);
				parent.appendRow(items);
			} else { // continue searching
				parent = nextParent;
			}
		}
		WStandardItem item = new WStandardItem(taxon);
		item.setData(DataType.Taxon, ItemDataRole.UserRole);
		List<WStandardItem> items = new ArrayList<WStandardItem>();
		items.add(item);
		parent.appendRow(items);
	}

	public void read(File csvFile) {
		clear();

		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = "\t"; // tab

		try {

			boolean header = true;
			br = new BufferedReader(new FileReader(csvFile));
			while ((line = br.readLine()) != null) {

				String[] cells = line.split(cvsSplitBy);

				if (header) { // check header
					assert(cells[0].equals("Taxon"));
					assert(cells[8].equals("Lineage"));
					header = false;
				}
				if (cells.length < 9)
					continue; // TODO?

				String taxon = cells[0];
				String[] linage = cells[8].split(";");
				addItem(taxon, linage);

				System.out.println(cells[8]);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	} 
}
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
	public static int SCIENTIFIC_NAME_ROLE = ItemDataRole.UserRole;
	public static int TAXONOMY_ID_ROLE = ItemDataRole.UserRole + 1;
	public static int DEPTH = 7; // Kingdom,Phylum/Division,Class,Legion,Order,Family,Tribe,Genus,Species

	private static TaxonomyModel instance = null;

	private TaxonomyModel(){
	}
	public static synchronized TaxonomyModel getInstance() {
		if (instance == null) {
			instance = new TaxonomyModel();

			File taxonomyFile = UpdateTaxonomyFileService.taxonomyFile();
			if (taxonomyFile.exists())
				instance.read(taxonomyFile);
		}

		return instance;
	}

	private WStandardItem findChild(WStandardItem parent, String scientificName) {
		for (int i = 0; i < parent.getRowCount(); ++i) {
			if (parent.getChild(i).getData(SCIENTIFIC_NAME_ROLE).equals(scientificName))
				return parent.getChild(i);
		}

		return null;
	}

	private void append(WStandardItem parent, WStandardItem child) {
		List<WStandardItem> items = new ArrayList<WStandardItem>();
		items.add(child);
		parent.appendRow(items);
	}

	private void addItem(String taxon, String scientificName, String[] linage) {
		scientificName = scientificName.trim();
		WStandardItem parent = getInvisibleRootItem();
		for(String l: linage) {
			l = l.trim();
			WStandardItem nextParent = findChild(parent, l);
			if (nextParent == null) {
				// create path
				nextParent = new WStandardItem(l);
				nextParent.setData(l, SCIENTIFIC_NAME_ROLE);
				append(parent, nextParent);
			}
			parent = nextParent;// continue searching
		}

		if (linage.length < DEPTH) {
			WStandardItem child = findChild(parent, scientificName);
			if (child != null) {
				// item is parent -> only add taxon id
				child.setData(taxon, TAXONOMY_ID_ROLE);
				child.setText(scientificName + " (" + taxon + ")");
				return;
			}
		}

		if (scientificName.equals("Alphacoronavirus"))
			System.err.println();

		WStandardItem item = new WStandardItem(scientificName + " (" + taxon + ")");
		item.setData(scientificName, SCIENTIFIC_NAME_ROLE);
		item.setData(taxon, TAXONOMY_ID_ROLE);
		append(parent, item);
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
					assert(cells[2].equals("Scientific name"));
					assert(cells[8].equals("Lineage"));
					header = false;
					continue;
				}

				String taxon = cells[0];
				String scientificName = cells[2];
				String[] linage = new String[0];
				
				if (cells.length < 9) { // linage can be empty
					addItem(taxon, scientificName, linage);
					continue;
				}

				linage = cells[8].split(";");

				addItem(taxon, scientificName, linage);

				System.out.println(cells[8] + " -> " + cells[2]);
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

		if (getColumnCount() > 0)
			setHeaderData(0, "Taxonomy");
	} 
}
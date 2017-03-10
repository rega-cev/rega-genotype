package rega.genotype.taxonomy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.WStandardItem;
import eu.webtoolkit.jwt.WStandardItemModel;

/**
 * Singleton, taxonomy hierarchy tree, created from uniprot taxonomy file: 
 * http://www.uniprot.org/taxonomy/?query=Viruses&format=tab
 * 
 * @author michael
 */
public class TaxonomyModel {
	public static final String VIRUSES_TAXONOMY_ID = "10239";
	
	public static int SCIENTIFIC_NAME_ROLE = ItemDataRole.UserRole;
	public static int TAXONOMY_ID_ROLE = ItemDataRole.UserRole + 1;
	public static int MNEMENIC_ROLE = ItemDataRole.UserRole + 2;
	public static int DEPTH = 7; // Kingdom,Phylum/Division,Class,Legion,Order,Family,Tribe,Genus,Species

	public static int TAXON_COL = 0;
	public static int MNEMENIC_COL = 1;
	public static int SCIENTIFIC_NAME_COL = 2;
	public static int COMMON_NAME_COL = 3;
	public static int SYNONYMS_COL = 4;
	public static int OTHER_NAME_COL = 5;
	public static int REVIEWED_COL = 6;
	public static int RANK_COL = 7;
	public static int LINEAGE_COL = 8;
	public static int PARENT_COL = 9;
	public static int VIRUS_HOST_COL = 10;

	// cache for improving read speed.
	// <Taxon_col, all columns>  from uniprot taxonomy.
	private Map<String, String[]> taxons = new HashMap<String, String[]>();
	private WStandardItem root = new WStandardItem();
	private Map<String, WStandardItem> items = new HashMap<String, WStandardItem>();

	private static TaxonomyModel instance = null;
	
	private TaxonomyModel(){
	}

	public static synchronized TaxonomyModel getInstance() {
		if (instance == null) {
			instance = new TaxonomyModel();
			instance.refreshCache();
		}

		return instance;
	}

	public WStandardItemModel createModel() {
		refreshCache();
		WStandardItemModel ans = new WStandardItemModel();
		List<WStandardItem> rootItems = new ArrayList<WStandardItem>();
		for (int i = 0; i < root.getRowCount(); ++i)
			rootItems.add(root.getChild(i));

		if (rootItems.size() > 0)
			ans.getInvisibleRootItem().appendRows(rootItems);

		if (root.getColumnCount() > 0)
			ans.setHeaderData(0, "Taxonomy");

		return ans;
	}

	private void refreshCache() {
		if (root.getRowCount() == 0) {
			// make sure that if taxonomy file exists it was read (can happen if the server failed)
			File taxonomyFile = RegaSystemFiles.taxonomyFile();
			if (taxonomyFile.exists())
				read(taxonomyFile);
		}
	}

	private WStandardItem findChild(WStandardItem parent, String taxonomyId) {
		for (int i = 0; i < parent.getRowCount(); ++i) {
			if (parent.getChild(i).getData(TAXONOMY_ID_ROLE).equals(taxonomyId))
				return parent.getChild(i);
		}

		return null;
	}

	private void append(WStandardItem parent, WStandardItem child) {
		List<WStandardItem> items = new ArrayList<WStandardItem>();
		items.add(child);
		parent.appendRow(items);
	}

	private WStandardItem createItem(String[] row) {
		WStandardItem item = new WStandardItem(
				row[SCIENTIFIC_NAME_COL] + ", " + row[TAXON_COL] + "");
		item.setData(row[SCIENTIFIC_NAME_COL], SCIENTIFIC_NAME_ROLE);
		item.setData(row[TAXON_COL], TAXONOMY_ID_ROLE);
		item.setData(row[MNEMENIC_COL], MNEMENIC_ROLE);

		items.put(row[TAXON_COL], item);

		return item;
	}
	public String getMnemenic(String taxonomyId) {
		 WStandardItem item = items.get(taxonomyId);
		 return item == null ? null : item.getData(MNEMENIC_ROLE).toString();
	}
	
	public String getScientificName(String taxonomyId) {
		 WStandardItem item = items.get(taxonomyId);
		 return item == null ? null : item.getData(SCIENTIFIC_NAME_ROLE).toString();
	}

	public String getHirarchy(String taxonomyId, int howFar) {		
		String ans = "";
		 WStandardItem item = items.get(taxonomyId);
		 if (item == null)
			 return null;

		 int i = 0;
		 while (item.getParent() != null && i < howFar) {
			 String name = (String) item.getData(SCIENTIFIC_NAME_ROLE);
			 if (name != null)
				 ans = "__" + name.replace(" ", "_").replace(",", "_") + ans;
			 item = item.getParent();
			 i++;
		 }
		 return ans;
	}

	/**
	 * @param taxonomyId
	 * @return all ancestors taxonomy ids ordered rank (viruses is first)
	 */
	public List<String> getHirarchyTaxonomyIds(String taxonomyId) {
		List<String> ans = new ArrayList<String>();
		WStandardItem item = items.get(taxonomyId);
		if (item == null)
			return ans;

		while (item.getParent() != null) {
			ans.add(0, (String) item.getData(TAXONOMY_ID_ROLE));
			item = item.getParent();
		}
		return ans;
	}

	public List<String> getDescendantsTaxonomyIds(String taxonomyId){
		WStandardItem item = items.get(taxonomyId);
		List<String> results = new ArrayList<String>();
		getDescendantsTaxonomyIds(item, results);
		return results;
	}

	private void getDescendantsTaxonomyIds(WStandardItem item,
			List<String> results) {
		results.add((String) item.getData(TAXONOMY_ID_ROLE));
		for (int r = 0; r < item.getRowCount(); r++)
			getDescendantsTaxonomyIds(item.getChild(r), results);
	}

	public String getLowesCommonAncestor(String taxonmyId1, String taxonmyId2) {
		if (items.get(taxonmyId1) == null || items.get(taxonmyId2) == null)
			return null;

		if (taxonmyId1.equals(taxonmyId2))
			return taxonmyId1;
		List<String> hirarchyTaxonomyIds1 = getHirarchyTaxonomyIds(taxonmyId1);
		List<String> hirarchyTaxonomyIds2 = getHirarchyTaxonomyIds(taxonmyId2);

		int i = 1;
		for (; i < Math.min(hirarchyTaxonomyIds1.size(), hirarchyTaxonomyIds2.size()); ++i)
			if (!hirarchyTaxonomyIds1.get(i).equals(hirarchyTaxonomyIds2.get(i)))
				return hirarchyTaxonomyIds1.get(i -1);

		return hirarchyTaxonomyIds1.get(i -1);
	}

	public List<String> getAllChildrenTaxonomy(String parentTaxonomyId){
		List<String> ans = new ArrayList<String>();
		WStandardItem item = items.get(parentTaxonomyId);
		if (item == null)
			return ans;

		getAllChildrenTaxonomy(item);

		return ans;
	}

	private static List<String> getAllChildrenTaxonomy(WStandardItem parentItem){
		List<String> ans = new ArrayList<String>();

		ans.add((String) parentItem.getData(TAXONOMY_ID_ROLE));

		for (int i = 0; i < parentItem.getRowCount(); ++i) {
			WStandardItem child = parentItem.getChild(i);
			ans.add((String) child.getData(TAXONOMY_ID_ROLE));
			ans.addAll(getAllChildrenTaxonomy(child));
		}
		return ans;
	}

	public void read(File csvFile) {
		//clear();
		taxons.clear();
		items.clear();

		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = "\t"; // tab

		try {
			boolean header = true;
			br = new BufferedReader(new FileReader(csvFile));
			while ((line = br.readLine()) != null) {
				String[] row = line.split(cvsSplitBy);

				if (header) { // check header
					header = false;
					continue;
				}

				taxons.put(row[TAXON_COL], row);
			}

			System.err.println("reading finished");

			for (String[] row: taxons.values()) {
				addItem(row);
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

	private void addItem(String[] row) {
		if (row == null)
			assert(false);

		if (items.containsKey(row[TAXON_COL]))
			return;

		if (row.length <= PARENT_COL) {
			if (findChild(root, row[TAXON_COL]) == null)
				append(root, createItem(row)); // add viruses item
			return;
		}

		if (items.containsKey(row[PARENT_COL])) {
			append(items.get(row[PARENT_COL]), createItem(row));
			return;
		} else {
			String[] parentRow = taxons.get(row[PARENT_COL]);
			if (parentRow == null) {
				System.err.println(row[SCIENTIFIC_NAME_COL] + " parent not found !!");
				System.err.println(row[LINEAGE_COL]); 
				append(root, createItem(row)); // add viruses item
				return; 
			} else {
				addItem(parentRow);
				WStandardItem parentItem = items.get(row[PARENT_COL]);
				if (parentItem != null)
					append(items.get(row[PARENT_COL]), createItem(row));
				else {
					assert(false);
				}
			}
		}
	}
	public Map<String, String[]> getTaxons() {
		return taxons;
	}
}
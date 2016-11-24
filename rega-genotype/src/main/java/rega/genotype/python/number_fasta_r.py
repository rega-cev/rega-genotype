from Bio import SeqIO
import re
import sys
import csv

p = ".*Tax=(.*) RepID=.*"
prog = re.compile(p)

taxons = {}
taxids = {}

Taxon_col = 0
Mnemonic_col = 1
Scientific_name_col = 2
Common_name_col = 3
Synonym_col = 4
Other_Names_col = 5
Reviewed_col = 6
Rank_col = 7
Lineage_col = 8
Parent_col = 9
Virus_hosts_col = 10

print ', '.join(sys.argv)

out = open(sys.argv[3], "w")
out.truncate()

with open(sys.argv[2], 'rb') as csvfile:
    reader = csv.reader(csvfile, delimiter='\t', quotechar='"')
    for row in reader:
        if row[Taxon_col] != 'Taxon':
            taxons[int(row[Taxon_col])] = row
            taxids[row[Scientific_name_col]] = int(row[Taxon_col])

print len(taxons)

handle = open(sys.argv[1], "rU")

for record in SeqIO.parse(handle, "fasta") :
    m = prog.match(record.description)
    if m:
        if m.group(1) in taxids:
            taxon = taxons[taxids[m.group(1)]]
            rank = taxon[Rank_col]
            while rank not in ['Genus']:
                taxon = taxons[int(taxon[Parent_col])]
                rank = taxon[Rank_col]
            if rank != 'Superkingdom':
                record.id = taxon[Taxon_col] + '_' + taxon[Scientific_name_col] + '_' + record.id
                out.write(record.format('fasta')),

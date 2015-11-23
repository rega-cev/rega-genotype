package rega.genotype.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class PerformanceAnalyse {
	
	private static String directory;
	
	private static List<String> subtyping(String virus){
		
		List<String> subtyping = null;
		
		if (virus.equalsIgnoreCase("hcv")){
			String[] HCVtypes = {"1a","1b","1c","1d","1e","1g","1h","1i","1j","1k","1l","1m","1n","2q","2a","2r","2b","2c","2t","2d","2e","2f","2i","2j","2k","2m","3a","3b","3d","3e","3g","3h","3i","3k","4a","4b","4c","4d","4f","4g","4k","4l","4m","4n","4o","4p","4q","4r","4t","4v","4w","5a","6a","6b","6c","6d","6e","6f","6g","6h","6xa","6i","6xb","6j","6xc","6k","6l","6xe","6m","6n","6o","6p","6q","6r","6s","6t","6u","6v","6w","7a"};
			subtyping = Arrays.asList(HCVtypes);
			return subtyping;
		}
		
		if (virus.equalsIgnoreCase("htlv")){
			String[] HTLVtypes = {"1aa","1ab","1ac","1ad","1ae","1b","1c","1d","1e","1f","2a","2b","2c","2d"};
			subtyping = Arrays.asList(HTLVtypes);
			return subtyping;
		}
		
		if (virus.equalsIgnoreCase("dengue")){
			String[] Denguetypes = {"1i","1ii","1iii","1iv","1v","2i","2ii","2iii","2iv","2v","2vi","3i","3ii","3iii","3v","4i","4ii","4iii","4iv"};
			subtyping = Arrays.asList(Denguetypes);
			return subtyping;
		}
		
		return subtyping;
	}

	private static void checkDirectorys(String directory) {
		
		File diretorio;
		
		diretorio = new File("performanceAnalyse/" + directory);
		if (!diretorio.exists()) {  
		   diretorio.mkdirs();
		}
		
		diretorio = new File("performanceAnalyse/" + directory + "/result");
		if (!diretorio.exists()) {  
		   diretorio.mkdirs();
		}
		
	}

	public static void main(String[] args) throws BiffException, IOException{
		
		if (args.length < 1){
			System.err.println("Usage: arguments invalid");
			return;
		}
		directory = args[0];
		
		checkDirectorys(directory);
		
		List<String> mySubtyping = subtyping(directory);
		
		String[] arrayCelName = new String[10];
		
		FileWriter myWriter = new FileWriter("performanceAnalyse/" + directory + "/result/" + directory + "_resultPerformance.csv");
		FileWriter resultSensSpec = new FileWriter("performanceAnalyse/" + directory + "/result/" + directory + "_resultSensSpec.csv");
		FileWriter resultSensSpecAll = new FileWriter("performanceAnalyse/" + directory + "/result/" + directory + "_resultSensSpecJuntos.csv");
		FileWriter newResultSensSpec = new FileWriter("performanceAnalyse/" + directory + "/result/" + directory + "_justSensSpec.csv");
		FileWriter newResultSensSpecAll = new FileWriter("performanceAnalyse/" + directory + "/result/" + directory + "_justSensSpecJuntos.csv");
		
		File f = null;
	    File[] paths;
		ArrayList<String> allSubtyping = new ArrayList<String>();
		ArrayList<String> files = new ArrayList<String>();
		Set<String> uniqueSubtyping = new LinkedHashSet<String>();
		ArrayList<String[]> sensSpecFinal = new ArrayList<String[]>();
		ArrayList<String[]> sensSpecFinalUnico = new ArrayList<String[]>();
		
		/* Inicio do cabeçalho do arquivo */
		f = new File("performanceAnalyse/" + directory + "/");
		paths = f.listFiles();
		for(File path:paths){
			String ext[] = path.getName().split("\\.");
			int end = ext.length - 1;
			if (ext[end].equalsIgnoreCase("xls")){				
				Workbook workbook = Workbook.getWorkbook(new File("performanceAnalyse/" + directory + "/" + path.getName()));
				Sheet sheet = workbook.getSheet(0);
				ArrayList<String> lineGenotypes = new ArrayList<String>();
				
				int linhas = sheet.getRows();
		        for(int i = 0; i < linhas; i++){
		        	if (i > 0){
		        		Cell celulaName = sheet.getCell(0, i);
		        		Cell celulaTypeSubtype = sheet.getCell(1, i);
		        		String originalGenotypo = "";
		        		String originalSubtype = "";
			        	arrayCelName = celulaName.getContents().toLowerCase().split("\\.");
			        	originalGenotypo = arrayCelName[0].replaceAll("[^0-9]", "");
			        	originalSubtype = arrayCelName[0].replaceAll("\\d", "");
			        	if (mySubtyping.contains(originalGenotypo + originalSubtype)){
			        		allSubtyping.add(originalGenotypo + originalSubtype);
			        	}
		        	}
		        }
		        Collections.sort(lineGenotypes);
			}
        }
		
		Collections.sort(allSubtyping);
		for (String valor: allSubtyping) {  
			uniqueSubtyping.add(valor);  
    	}
		
		ArrayList<String> copyUnique = new ArrayList<String>();
        copyUnique.addAll(mySubtyping);
        copyUnique.removeAll(uniqueSubtyping);
        for (String valor: copyUnique) {  
        	uniqueSubtyping.add(valor);
    	}
       
		myWriter.append("File;");
        myWriter.append("Total CDS;");
        myWriter.append("Correct;");
        myWriter.append("Error");
        for (String valor: uniqueSubtyping) {  
        	myWriter.append(";" + valor);
    	}
        myWriter.append("\n");
		/*Final do cabeçalho do arquivo*/
        
        /* Inicio do conteudo do arquivo */
		for(File path:paths){
			String ext[] = path.getName().split("\\.");
			int end = ext.length - 1;
			if (ext[end].equalsIgnoreCase("xls")){		
				Workbook workbook = Workbook.getWorkbook(new File("performanceAnalyse/" + directory + "/" + path.getName()));
				Sheet sheet = workbook.getSheet(0);
				ArrayList<String> fileLineGenotypes = new ArrayList<String>();
				ArrayList<String> foundLineGenotypes = new ArrayList<String>();
				ArrayList<String[]> sensSpecProv = new ArrayList<String[]>();
				ArrayList<String[]> sensSpec = new ArrayList<String[]>();
				ArrayList<String> otherClass = new ArrayList<String>();
				int linhas = sheet.getRows();
		        int countCorrect = 0;
		        int countError = 0;
		        int countSeq = 0;
		        int tp = 0;
		        int fp = 0;
		        int tn = 0;
		        int fn = 0;
		        float se = 0;
		        float sp = 0;
		        float ppv = 0;
		        float npv = 0;
		        for(int i = 0; i < linhas; i++){
		        	if (i > 0){
		        		Cell celulaName = sheet.getCell(0, i);
		        		Cell celulaTypeSubtype = sheet.getCell(1, i);
		        		Cell celulaLength = sheet.getCell(2, i);
		        		String originalGenotypo = "";
		        		String originalSubtype = "";
			        	arrayCelName = celulaName.getContents().toLowerCase().split("\\.");
			        	String arrayCelTypeSubtype = celulaTypeSubtype.getContents().toLowerCase();
			        	//System.out.println(path.getName() + "-" + celulaLength.getContents() + "-" + arrayCelName[0]);
			        	countSeq += Integer.parseInt(celulaLength.getContents());
			        	
			        	originalGenotypo = arrayCelName[0].replaceAll("[^0-9]", "");
			        	originalSubtype = arrayCelName[0].replaceAll("\\d", "");
			        	
			        	if (mySubtyping.contains(originalGenotypo + originalSubtype)){
			        		fileLineGenotypes.add(originalGenotypo + originalSubtype);
			        		if (arrayCelName[0].equalsIgnoreCase(arrayCelTypeSubtype)){
		        				countCorrect++;
		        				foundLineGenotypes.add(arrayCelTypeSubtype);
		        				sensSpecProv.add(new String[] {path.getName(), arrayCelTypeSubtype, "1", "0"});
			        		}else{
			        			if (mySubtyping.contains(arrayCelTypeSubtype)){
			        				otherClass.add(arrayCelTypeSubtype);
			        			}
			        			sensSpecProv.add(new String[] {path.getName(), arrayCelName[0], "0", "1"});
		        				countError++;
		        			}
			        	}
			        }
		        }
		        Collections.sort(fileLineGenotypes);
		        Collections.sort(otherClass);
		        ArrayList<String> copyUniqueFP = new ArrayList<String>();
		        ArrayList<String> copyUniqueOC = new ArrayList<String>();
		        ArrayList<String[]> fileCellFP = new ArrayList<String[]>();
		        ArrayList<String[]> fileCellOC = new ArrayList<String[]>();
		        copyUniqueFP.addAll(uniqueSubtyping);
		        copyUniqueOC.addAll(uniqueSubtyping);
		        copyUniqueFP.removeAll(fileLineGenotypes);
		        copyUniqueOC.removeAll(otherClass);
		        
		        Set<String> x = new HashSet<String>(fileLineGenotypes);
		        for(String s: x){
		        	fileCellFP.add(new String[] {path.getName() , s, Integer.toString(Collections.frequency(fileLineGenotypes,s))});
		        }
		        for (String valor: copyUniqueFP) {  
		        	fileCellFP.add(new String[] {path.getName() , valor , "0"});
		    	}
		        
		        Set<String> y = new HashSet<String>(otherClass);
		        for(String s: x){
		        	fileCellOC.add(new String[] {path.getName() , s, Integer.toString(Collections.frequency(otherClass,s))});
		        }
		        for (String valor: copyUniqueOC) {  
		        	fileCellOC.add(new String[] {path.getName() , valor , "0"});
		    	}
		        
		        Collections.sort(fileCellFP, new Comparator<String[]>() {
		            public int compare(final String[] entry1, final String[] entry2) {
		                final String time1 = entry1[1];
		                final String time2 = entry2[1];
		                return time1.compareTo(time2);
		            }
		        });
		        
		        Collections.sort(fileCellOC, new Comparator<String[]>() {
		            public int compare(final String[] entry1, final String[] entry2) {
		                final String time1 = entry1[1];
		                final String time2 = entry2[1];
		                return time1.compareTo(time2);
		            }
		        });

		        Collections.sort(sensSpecProv, new Comparator<String[]>() {
		            public int compare(final String[] entry1, final String[] entry2) {
		                final String time1 = entry1[1];
		                final String time2 = entry2[1];
		                return time1.compareTo(time2);
		            }
		        });
		        
		        int total = 0;
		        String hostVarFile = "";
		        String hostVarSubt = "";
		        String[] valorTempSensSpec;
		        for(int aux=0; aux<sensSpecProv.size(); aux++){
		        	valorTempSensSpec = sensSpecProv.get(aux);
		        	if ((!(valorTempSensSpec[0].equalsIgnoreCase(hostVarFile))) && (!(valorTempSensSpec[1].equalsIgnoreCase(hostVarSubt)))){
		        		hostVarFile = valorTempSensSpec[0];
		        		hostVarSubt = valorTempSensSpec[1];
	        		}
		        	if ((valorTempSensSpec[0].equalsIgnoreCase(hostVarFile)) && (!(valorTempSensSpec[1].equalsIgnoreCase(hostVarSubt)))){
		        		sensSpec.add(new String[] {hostVarFile, hostVarSubt, Integer.toString(tp), Integer.toString(fn), Integer.toString(total)});
		        		hostVarSubt = valorTempSensSpec[1];
		        		tp = 0;
		        		fn = 0;
		        		total = 0;
	        		}
		        	if ((valorTempSensSpec[0].equalsIgnoreCase(hostVarFile)) && (valorTempSensSpec[1].equalsIgnoreCase(hostVarSubt))){
		        		if(valorTempSensSpec[2].equalsIgnoreCase("1")){
		        			tp++;
		        		}
		        		if(valorTempSensSpec[3].equalsIgnoreCase("1")){
		        			fn++;
		        		}
		        		total++;
		        	}
		        }
		        sensSpec.add(new String[] {hostVarFile, hostVarSubt, Integer.toString(tp), Integer.toString(fn), Integer.toString(total)});
		        
		        copyUniqueFP = new ArrayList<String>();
		        copyUniqueFP.addAll(uniqueSubtyping);
		        copyUniqueFP.removeAll(fileLineGenotypes);
		        
		        for (String valor: copyUniqueFP) {  
		        	sensSpec.add(new String[] {path.getName(), valor, "NA", "NA", "NA"});
		    	}
		        		        
		        Collections.sort(sensSpec, new Comparator<String[]>() {
		            public int compare(final String[] entry1, final String[] entry2) {
		                final String time1 = entry1[1];
		                final String time2 = entry2[1];
		                return time1.compareTo(time2);
		            }
		        });
		        
		        se = 0;
		        sp = 0;
		        
		        for(int aux=0; aux<sensSpec.size(); aux++){
		        	String[] valor = sensSpec.get(aux);
		        	String[] valor1 = fileCellOC.get(aux);
		        	String dataSet = "";
	        		if (mySubtyping.contains(valor[1]) == true){
	        			dataSet = "YES";
	        		}else{
	        			dataSet = "NO";
	        		}
		        	if (!(valor[4].equalsIgnoreCase("NA"))){
		        		String ppvS;
		        		String npvS;
		        		tp = Integer.parseInt(valor[2]);
		        		fn = Integer.parseInt(valor[3]);
		        		tn = Math.abs(Integer.parseInt(valor[4]) - Integer.parseInt(valor1[2]));
		        		fp = Integer.parseInt(valor1[2]);
		        		se = ((float)tp/((float)tp+(float)fn)) * 100;
				        sp = ((float)tn/((float)tn+(float)fp)) * 100; 
				        ppv = ((float)tp/((float)tp+(float)fp)) * 100;
				        npv = ((float)tn/((float)tn+(float)fn)) * 100;
				        if (Float.isNaN(ppv)) {
				        	ppvS = "--";
				        }else{
				        	ppvS = Float.toString(ppv).replace('.', ',');
				        }
				        if (Float.isNaN(npv)) {
				        	npvS = "--";
				        }else{
				        	npvS = Float.toString(npv).replace('.', ',');
				        }
		        		sensSpecFinal.add(new String[] {valor[0], valor[1], valor[4], Integer.toString(tp), Integer.toString(tn), Integer.toString(fp), Integer.toString(fn), Float.toString(se), Float.toString(sp), ppvS, npvS, dataSet});
		        	}else{
		        		sensSpecFinal.add(new String[] {valor[0], valor[1], "NA", "NA", "NA", "NA", "NA", "NA", "NA", "NA", "NA", dataSet});
		        	}
		        }
		        
		        Collections.sort(fileLineGenotypes);
		        Collections.sort(foundLineGenotypes);
		        String coletionsFile = "";
		        coletionsFile += path.getName() + ";" + countSeq + ";" + countCorrect + ";" + countError;
		        	        
		        ArrayList<String> copyUniqueP = new ArrayList<String>();
		        ArrayList<String> copyUniqueF = new ArrayList<String>();
		        ArrayList<String> fileCellP = new ArrayList<String>();
		        ArrayList<String> fileCellF = new ArrayList<String>();
		        
		        Set<String> fileCellPUnique = new LinkedHashSet<String>();
		        for (String valor: fileLineGenotypes) {  
		        	fileCellPUnique.add(valor);  
		    	}
		        
		        Set<String> fileCellFUnique = new LinkedHashSet<String>();
		        for (String valor: foundLineGenotypes) {  
		        	fileCellFUnique.add(valor);  
		    	}
		        
		        copyUniqueP.addAll(uniqueSubtyping);
		        copyUniqueF.addAll(uniqueSubtyping);
		        
		        copyUniqueP.removeAll(fileCellPUnique);
		        copyUniqueF.removeAll(fileCellFUnique);
		        
		        Set<String> mySet1 = new HashSet<String>(fileLineGenotypes);
		        for(String s: mySet1){
		        	fileCellP.add(s + "_" + Collections.frequency(fileLineGenotypes,s));
		        }
		        Set<String> mySet2 = new HashSet<String>(foundLineGenotypes);
		        for(String s: mySet2){
		        	fileCellF.add(s + "_" + Collections.frequency(foundLineGenotypes,s));
		        }

		        for (String valor: copyUniqueP) {  
		        	fileCellP.add(valor + "_0");
		    	}
		        
		        for (String valor: copyUniqueF) {  
		        	fileCellF.add(valor + "_0");
		    	}
		        
		        Collections.sort(fileCellP);
		        Collections.sort(fileCellF);
		        
		        for(int aux=0; aux<fileCellF.size(); aux++){
		        	String[] auxP = fileCellP.get(aux).split("_");
		        	String[] auxF = fileCellF.get(aux).split("_");
		        	coletionsFile += ";" + auxP[1] + "/" + auxF[1];
		        }
		        files.add(coletionsFile);
		        //System.exit(0);
			}
        }
		for (String valor: files) {  
        	myWriter.append(valor);
        	myWriter.append("\n");
    	}
        //myWriter.append("\n");
		/*Final do conetudo do arquivo*/
        myWriter.flush();
        myWriter.close();
        
        resultSensSpec.append("File;");
        resultSensSpec.append("Subtyping;");
        resultSensSpec.append("Total;");
        resultSensSpec.append("TP;");
        resultSensSpec.append("TN;");
        resultSensSpec.append("FP;");
        resultSensSpec.append("FN;");
        resultSensSpec.append("SENS;");
        resultSensSpec.append("SPEC;");
        resultSensSpec.append("PPV;");
        resultSensSpec.append("NPV;");
        resultSensSpec.append("\n");
        
        //sensSpecFinal.add(new String[] {valor[0], valor[1], valor[4], Integer.toString(tp), Integer.toString(tn), Integer.toString(fp), Integer.toString(fn), Float.toString(se), Float.toString(sp), ppvS, npvS, dataSet});
        
        
        for (String[] valor: sensSpecFinal) {
        	int aux=0;
        	for (String valor1: valor) {
        		if (aux == 0){
        			resultSensSpec.append(valor1);
        		}else{
        			/**/
        			if (aux >= 7 && aux <=8){
        				resultSensSpec.append(";" + valor1.replace('.', ','));
        			}else{
        				resultSensSpec.append(";" + valor1);
        			}
        			/**
        			resultSensSpec.append(";" + valor1);
        			/**/
        		}
        		aux++;
        	}
        	resultSensSpec.append("\n");
    	}
        resultSensSpec.flush();
        resultSensSpec.close();
        
        String arquivoFinal = ";";
        String arquivoFinal1 = ";";
        int qtd = 0;
        int qtd1 = 0;
        String file = "";
        
        for (String[] valor: sensSpecFinal) {
        	if (!(file.equalsIgnoreCase(valor[0]))){
        		qtd++;
        		file = valor[0];
        		arquivoFinal += ";" + valor[0] + ";";
        		if (qtd == 1){
        			qtd1++;
        		}
    		}else{
	        	if (qtd == 1){
	        		qtd1++;
	        		//System.out.println(qtd1 + " - " + valor[1]);
	        	}
    		}
        }
        
        arquivoFinal += "\n";
        arquivoFinal += "Subtyping;Dataset";
        arquivoFinal1 += "\n";
        arquivoFinal1 += "Subtyping;Dataset";
        for (int i=0;i<(qtd);i++) {
        	arquivoFinal += ";SENS;SPEC";
        }
        arquivoFinal1 += ";SENS;SPEC";
        arquivoFinal += "\n";
        arquivoFinal1 += "\n";
        int aux = 0;
        file = "";
        /**
        String[] subtipos = new String[qtd1];

        for (String[] valor: sensSpecFinal) {
        	if (!(file.equalsIgnoreCase(valor[1]))){
        		file = valor[1];
        		subtipos[aux] = valor[1];
        		aux++;
        	}
        }
        /**/
        
        for (int i=0;i<qtd1;i++){
        	int tp1 = 0;
        	int fp1 = 0;
        	int tn1 = 0;
        	int fn1 = 0;
        	int total = 0;
        	float se1 = 0;
        	float sp1 = 0;
        	float ppv1 = 0;
        	float npv1 = 0;
        	String ppvS = "";
    		String npvS = "";
        	String tipo = "";
        	String dataSet = "";
        	for (int j=0;j<qtd;j++){
        		String[] valor = sensSpecFinal.get((qtd1 * j) + i);
        		if (j == 0){
        			arquivoFinal += valor[1] + ";" + valor[11] + ";" + valor[7].replace('.', ',') + ";" + valor[8].replace('.', ',');
        		}else{
        			arquivoFinal += ";" + valor[7].replace('.', ',') + ";" + valor[8].replace('.', ',');
        		}
        		//sensSpecFinal.add(new String[] {valor[0], valor[1], valor[4], Integer.toString(tp), Integer.toString(tn), Integer.toString(fp), Integer.toString(fn), Float.toString(se), Float.toString(sp), ppvS, npvS, dataSet});
        		if (!(valor[3].equalsIgnoreCase("NA"))){
        			tp1 += Integer.parseInt(valor[3]);
        		}
        		if (!(valor[4].equalsIgnoreCase("NA"))){
        			tn1 += Integer.parseInt(valor[4]);
        		}
        		if (!(valor[5].equalsIgnoreCase("NA"))){
        			fp1 += Integer.parseInt(valor[5]);
        		}
        		if (!(valor[6].equalsIgnoreCase("NA"))){
        			fn1 += Integer.parseInt(valor[6]);
        		}
        		if (!(valor[2].equalsIgnoreCase("NA"))){
        			total += Integer.parseInt(valor[2]);
        		}
        		tipo = valor[1];
        		dataSet = valor[11];
        		//System.out.print(valor[1] + " ");
        	}
        	se1 = ((float)tp1/((float)tp1+(float)fn1)) * 100;
	        sp1 = ((float)tn1/((float)tn1+(float)fp1)) * 100; 
	        ppv1 = ((float)tp1/((float)tp1+(float)fp1)) * 100;
	        npv1 = ((float)tn1/((float)tn1+(float)fn1)) * 100;
	        if (Float.isNaN(ppv1)) {
	        	ppvS = "--";
	        }else{
	        	ppvS = Float.toString(ppv1).replace('.', ',');
	        }
	        if (Float.isNaN(npv1)) {
	        	npvS = "--";
	        }else{
	        	npvS = Float.toString(npv1).replace('.', ',');
	        }
	        if (total > 0){
	        	sensSpecFinalUnico.add(new String[] {tipo, Integer.toString(total), Integer.toString(tp1), Integer.toString(tn1), Integer.toString(fp1), Integer.toString(fn1), Float.toString(se1).replace('.', ','), Float.toString(sp1).replace('.', ','), ppvS, npvS, dataSet});
	        }else{
	        	sensSpecFinalUnico.add(new String[] {tipo, "NA", "NA", "NA", "NA", "NA", "NA", "NA", "NA", "NA", dataSet});
	        }
        	arquivoFinal += "\n";
        	//System.out.println("");
        }
        /**/
        newResultSensSpec.append(arquivoFinal);
        newResultSensSpec.flush();
        newResultSensSpec.close();
        //System.out.println(arquivoFinal);    
        
        resultSensSpecAll.append("Subtyping;");
        resultSensSpecAll.append("Total;");
        resultSensSpecAll.append("TP;");
        resultSensSpecAll.append("TN;");
        resultSensSpecAll.append("FP;");
        resultSensSpecAll.append("FN;");
        resultSensSpecAll.append("SENS;");
        resultSensSpecAll.append("SPEC;");
        resultSensSpecAll.append("PPV;");
        resultSensSpecAll.append("NPV;");
        resultSensSpecAll.append("DataSet;");
        resultSensSpecAll.append("\n");
        newResultSensSpecAll.append("Subtyping;");
        newResultSensSpecAll.append("DataSet;");
        newResultSensSpecAll.append("Total;");
        newResultSensSpecAll.append("SENS;");
        newResultSensSpecAll.append("SPEC;");
        newResultSensSpecAll.append("\n");
        for (String[] row : sensSpecFinalUnico) {
        	int controle = 0;
            for(String value : row){
            	if (controle == 0){
            		resultSensSpecAll.append(value);
            	}else{
            		resultSensSpecAll.append(";" + value);
            	}
            	controle++;
            }
            resultSensSpecAll.append("\n");
            //sensSpecFinalUnico.add(new String[] {tipo, Integer.toString(total), Integer.toString(tp1), Integer.toString(tn1), Integer.toString(fp1), Integer.toString(fn1), Float.toString(se1), Float.toString(sp1), ppvS, npvS, dataSet});
            newResultSensSpecAll.append(row[0] + ";");
            newResultSensSpecAll.append(row[10] + ";");
            newResultSensSpecAll.append(row[1] + ";");
            newResultSensSpecAll.append(row[6] + ";");
            newResultSensSpecAll.append(row[7] + ";");
            newResultSensSpecAll.append("\n");
        }
        resultSensSpecAll.flush();
        resultSensSpecAll.close();
        newResultSensSpecAll.flush();
        newResultSensSpecAll.close();
        System.out.println("Concluido");
	}
	
}
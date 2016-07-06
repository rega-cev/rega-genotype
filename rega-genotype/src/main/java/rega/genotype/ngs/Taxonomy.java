package rega.genotype.ngs;

import java.io.File;

import rega.genotype.singletons.Settings;

public class Taxonomy {
	private int id;
	private String name;
	
	public Taxonomy(String str){
		File f = new File(str);
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public int getId(){
		return this.id;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}
	
}

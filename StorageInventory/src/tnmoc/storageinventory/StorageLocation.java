package tnmoc.storageinventory;

import java.io.Serializable;


public class StorageLocation implements Comparable<StorageLocation>, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String label = null;
	String parent= "";
	int id = -1;
	int parentid = -1;
	public StorageLocation(){}
	public StorageLocation(
			String label, 
			String parent, 
			int id, 
			int parentid){
		this.label = label;
		this.parent = parent;
		this.id = id;
		this.parentid = parentid;
	}
	
	public int compareTo(StorageLocation arg0) {
		return new String(parent + "/" +label).compareTo(parent + "/" +arg0.label);
	}
	public String toString(){
		return label + "(" + parent + ")";
	}
}
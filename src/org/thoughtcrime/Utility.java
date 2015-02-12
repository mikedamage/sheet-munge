package org.thoughtcrime;

import java.util.Iterator;
import java.io.File;
import java.io.FileInputStream;
import org.apache.poi.*;
import org.apache.commons.io.FileUtils;

public class Utility {

	private String[] exts;
	private File root;
	private Iterator rootIterator;
	
	public Utility(String rootDir) {
		exts         = new String[] { "xls" };
		root         = new File(rootDir);
		rootIterator = FileUtils.iterateFiles(root, exts, true);
	}

	public File getRoot() {
		return root;
	}
	
	public void printFiles() {
		while (rootIterator.hasNext()) {
			System.out.println(((File) rootIterator.next()).getName());
		}
	}
}


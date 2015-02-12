package org.thoughtcrime;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import java.util.Iterator;
import java.io.File;
import java.io.FileInputStream;

import org.apache.poi.*;
import org.apache.commons.io.FileUtils;

public class Utility {

	private static final Logger logger = Logger.getLogger(Utility.class.getName());
	private String[] rawArgs           = null;
	private Options options            = new Options();
	private String[] exts              = { "xls" };
	private File root;
	private Iterator rootIterator;

	public Utility(String[] args) {
		this.rawArgs = args;

		options.addOption("h", "help", false, "Show usage info");
		options.addOption("n", "dry-run", false, "Just print which files would be processed");
		options.addOption("d", "directory", true, "The directory to process");
		options.addOption("i", "image", true, "The image to insert");
		options.addOption("x", "width", true, "Image width");
		options.addOption("y", "height", true, "Image height");
		// image needs to be 70px tall
	}

	public void parse() {
		CommandLineParser parser = new BasicParser();
		CommandLine cmd          = null;
		
		try {
			cmd = parser.parse(options, rawArgs);
			
			if (cmd.hasOption("h")) {
				help();
				System.exit(0);
			}

			if (cmd.hasOption("d")) {
				boolean dryRun = cmd.hasOption("n");
				root           = new File(cmd.getOptionValue("d")).getAbsoluteFile();
				rootIterator   = FileUtils.iterateFiles(root, exts, true);

				processFiles(dryRun);
				System.exit(0);
			}
			
			logger.severe("Missing required option -d. Please specify a directory to process.");
			System.exit(1);
		} catch (ParseException e) {
			logger.log(Level.SEVERE, "Failed to parse command line arguments", e);
			help();
		}
	}

	private void help() {
		HelpFormatter formatter = new HelpFormatter();
		System.out.println("SheetMunge - Replace images in a directory of Excel spreadsheets\n");
		formatter.printHelp("java -jar SheetMunge.jar OPTIONS", options);
		System.exit(0);
	}

	private void processFiles(boolean dryRun) {
		while (rootIterator.hasNext()) {
			File currentFile     = (File) rootIterator.next();
			String absPath       = currentFile.getAbsolutePath();
			boolean readable     = currentFile.canRead();
			File mungedFile      = makeMungedFile(currentFile);
			String mungedPath    = mungedFile.getAbsolutePath();
			boolean mungedExists = mungedFile.isFile();

			logger.info("Found spreadsheet: " + absPath);
			logger.info("Munged filename: " + mungedFile.getAbsolutePath());

			if (mungedExists) {
				logger.info("Munged file already exists. Will overwrite.");
			}

			if (dryRun) {
				continue;
			}
		}
	}

	private File makeMungedFile(File file) {
		File parentFolder   = file.getParentFile();
		String fileName     = file.getName();
		int extensionIdx    = fileName.lastIndexOf(".xls");
		String strippedName = fileName.substring(0, extensionIdx);
		String mungedName   = strippedName + ".munged.xls";
		File mungedFile     = new File(parentFolder, mungedName);

		return mungedFile;
	}
}

// vim: set ts=4 sw=4 :

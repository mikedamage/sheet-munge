package org.thoughtcrime;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.BasicConfigurator;

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
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class Utility {

	private static final Logger logger = LogManager.getLogger(Utility.class.getName());
	private String[] rawArgs           = null;
	private Options options            = new Options();
	private String[] exts              = { "xls" };
	private String fileSuffix          = ".updated";
	private File root;
	private Iterator rootIterator;
	private byte[] newPicture;

	public Utility(String[] args) {
		this.rawArgs = args;
		
		// Setup logging
		BasicConfigurator.configure();

		options.addOption("h", "help", false, "Show usage info");
		options.addOption("n", "dry-run", false, "Just print which files would be processed");
		options.addOption("d", "directory", true, "The directory to process");
		options.addOption("i", "image", true, "The image to insert");
		//options.addOption("x", "width", true, "Image width");
		//options.addOption("y", "height", true, "Image height");
		options.addOption("s", "suffix", true, "Updated filename suffix");
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

			if (cmd.hasOption("s")) {
				fileSuffix = cmd.getOptionValue("s");
			}

			if (cmd.hasOption("d")) {
				if (!cmd.hasOption("i")) {
					logger.fatal("Missing required -i argument");
					System.exit(1);
				}
				
				File newPictureFile = new File(cmd.getOptionValue("i")).getAbsoluteFile();
				
				if (!isImagePng(newPictureFile)) {
					logger.fatal("PNG images only! Come back with a PNG.");
					System.exit(1);
				}
				
				try {
					FileInputStream newPictureIn = new FileInputStream(newPictureFile);
					newPicture = IOUtils.toByteArray(newPictureIn);
				} catch (FileNotFoundException e) {
					logger.fatal("New picture file not found!");
					System.exit(1);
				} catch (IOException e) {
					logger.fatal("Error loading new picture file!");
					System.exit(1);
				}
				
				boolean dryRun = cmd.hasOption("n");
				root           = new File(cmd.getOptionValue("d")).getAbsoluteFile();
				rootIterator   = FileUtils.iterateFiles(root, exts, true);

				processFiles(dryRun);
				System.exit(0);
			}
			
			logger.fatal("Missing required option -d. Please specify a directory to process.");
			System.exit(1);
		} catch (ParseException e) {
			logger.fatal("Failed to parse command line arguments!");
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
			
			if (absPath.indexOf(fileSuffix) != -1) {
				logger.debug("Ignoring previously munged file: " + absPath);
				continue;
			}
			
			boolean readable     = currentFile.canRead();
			File mungedFile      = makeMungedFile(currentFile);
			String mungedPath    = mungedFile.getAbsolutePath();
			boolean mungedExists = mungedFile.isFile();

			logger.debug("Found spreadsheet: " + absPath);
			logger.info("Converting spreadsheet: " + absPath);
			logger.debug("Munged filename: " + mungedPath);

			if (mungedExists) {
				logger.info("Munged file already exists. Will overwrite.");
			}
			
			if (!readable) {
				logger.warn(absPath + " is not readable. Skipping.");
				continue;
			}
			
			logger.debug("Opening HSSF workbook");
			try {
				FileInputStream inStream     = new FileInputStream(currentFile);
				HSSFWorkbook currentWorkbook = new HSSFWorkbook(inStream);
				HSSFSheet templateSheet      = currentWorkbook.getSheet("template");
				
				if (templateSheet == null) {
					logger.warn("Could not find Template sheet in workbook. Skipping.");
					continue;
				}
				
				logger.debug("Found Template sheet in workbook");
				
				HSSFPatriarch drawingPatriarch = templateSheet.getDrawingPatriarch();
				List allChildren               = drawingPatriarch.getChildren();
				int numDrawings                = allChildren.size();
				logger.debug("Found " + String.valueOf(numDrawings) + " drawings");
				
				if (numDrawings > 0) {
					logger.info("Removing first image from worksheet");
					HSSFShape shape = (HSSFShape) allChildren.get(0);
					drawingPatriarch.removeShape(shape);
				}
				
				logger.debug("Making creation helper");
				CreationHelper helper = currentWorkbook.getCreationHelper();
				
				logger.debug("Actually adding image to workbook");
				int pictureIdx        = currentWorkbook.addPicture(newPicture, Workbook.PICTURE_TYPE_PNG);
				
				logger.debug("Creating client anchor");
				ClientAnchor anchor   = helper.createClientAnchor();
				
				anchor.setCol1(0);
				anchor.setRow1(0);
				
				logger.debug("Creating Picture object");
				Picture logo = drawingPatriarch.createPicture(anchor, pictureIdx);
				
				logger.debug("Resizing Picture to native dimensions");
				logo.resize();
				
				if (!dryRun) {
					FileOutputStream outStream = new FileOutputStream(mungedFile);
					logger.info("Saving new workbook to " + mungedPath);
					currentWorkbook.write(outStream);
					outStream.close();
				}
				
			} catch (FileNotFoundException e) {
				logger.warn("File not found: " + absPath);
				continue;
			} catch (IOException e) {
				logger.warn("Error reading file: " + absPath);
				continue;
			}
			
		}
	}

	private File makeMungedFile(File file) {
		File parentFolder   = file.getParentFile();
		String fileName     = file.getName();
		int extensionIdx    = fileName.lastIndexOf(".xls");
		String strippedName = fileName.substring(0, extensionIdx);
		String mungedName   = strippedName + fileSuffix + ".xls";
		File mungedFile     = new File(parentFolder, mungedName);

		return mungedFile;
	}
	
	private boolean isImagePng(File img) {
		if (img.getName().indexOf(".png") == -1) {
			return false;
		}
		
		return true;
	}
}

// vim: set ts=4 sw=4 :

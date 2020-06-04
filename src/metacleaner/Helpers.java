package metacleaner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Helpers {
	public static Settings parseCLIArgs(String[] args) throws FileNotFoundException {
		Options options = new Options();

        Option input = new Option("i", "input", true, "input file / directory path *");
        input.setRequired(true);
        options.addOption(input);
        
        Option suffix = new Option("s", "suffix", true, "suffix of output file");
        options.addOption(suffix);
        
        Option filetype = new Option("f", "filetype", true, "override file ending in file path with this parameter");
        options.addOption(filetype);
        
        Option recursive = new Option("r", "recursive", false, "search for files recursively if input path is a directory");
        options.addOption(recursive);

        Option overwrite = new Option("o", "overwrite", false, "overwrite input file");
        options.addOption(overwrite);
        
        Option harsh = new Option("h", "harsh", false, "remove ALL metadata, such as potentially harmless data (i.E. title and subject)");
        options.addOption(harsh);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar metacleaner.jar", options);
            System.exit(1);
        }
        String suffixVal="";
        String inputFilePath = cmd.getOptionValue("input");
        boolean recursiveVal = cmd.hasOption("recursive");
        boolean overwriteVal = cmd.hasOption("overwrite");
        if(overwriteVal) {
        	System.out.println("\\_ will overwrite input file(s)");
        }else {
        	suffixVal = cmd.getOptionValue("suffix");
        	if(suffixVal!=null) {
        		System.out.println("\\_ using custom suffix '"+suffixVal+"'");
        	}else {
        		suffixVal="_cleaned";
        	}
        }
        boolean harshVal = cmd.hasOption("harsh");
        if(harshVal) {
        	System.out.println("\\_ using harsh mode");
        }
        
        String fileEndingForcedVal = cmd.getOptionValue("filetype");
        if(fileEndingForcedVal!=null) {
        	System.out.println("\\_ forcing file type '"+fileEndingForcedVal+"'");
        }
        
        boolean isDirectory=false;
		File file = new File(inputFilePath);
		if(file.isDirectory()) {
			isDirectory=true;
			System.out.println("\\_ input path is a directory - "+(recursiveVal?"will scan recursively for files":"won't scan recursively"));
		}else if (!file.exists()) {
			throw new FileNotFoundException("File / Folder '" + inputFilePath + "' not found or readable");
		}else {
			System.out.println("\\_ input path is a file");
		}
        
        return new Settings(inputFilePath, isDirectory, recursiveVal, overwriteVal, suffixVal, harshVal, fileEndingForcedVal);
	}
	
	static void matchedEnding(String fileEnding, boolean printSupported) {
		if(printSupported) {
			System.out.println("  |_ supporting '" + fileEnding + "' files - continuing");			
		}
	}
	
	static String appendSlashIfNone(String path) {
		if(!path.endsWith(File.separator)) {
			path=path+File.separator;
		}
		return path;
	}

	static boolean isAlreadyClean(Settings settings, String pathS) {
		if(settings.isOverwrite()) {
			return false;
		}
		int lastDotPos = pathS.lastIndexOf(".");
		String fileEnding = pathS.substring(lastDotPos + 1);
		String endsWith="";
		if(lastDotPos == -1) {
			endsWith = settings.getSuffix()+fileEnding;				
		}else {
			endsWith = settings.getSuffix()+"."+fileEnding;
		}
		return pathS.toLowerCase().endsWith(endsWith.toLowerCase());
	}
	
	static double getFileSizeB(String path) {
		File file = new File(path);
		return file.length();
	}
	
	public static String readLbL(String filePath) {
		StringBuilder contentBuilder = new StringBuilder();
		try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
			stream.forEach(s -> contentBuilder.append(s).append("\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentBuilder.toString();
	}
}

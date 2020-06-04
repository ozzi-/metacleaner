package metacleaner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;


public class Cleaner {

	// TODO add ZIP support - https://github.com/KittyHawkCorp/stripzip/ ?
	// TODO add mysql dump support
	
	public static int cleanedFiles;
	public static int totalFiles;
		
	public static void main(String[] args) {
		System.out.println("metacleaner 1.0 - github.com/ozzi-/metacleaner");
		System.out.println("----------------------------------------------");
		try {
			Settings settings = Helpers.parseCLIArgs(args);
			boolean cleanHadErrors = clean(settings);
			if(cleanHadErrors) {
				System.out.println("\\_ Exiting with error code (1)");
				System.exit(1);				
			}
		} catch (FileNotFoundException e) {
			System.out.println("\\_ "+e.getMessage());
			System.out.println("\\_ Exiting with error code (2)");
			System.exit(2);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("\\_ Exiting with error code (3)");
			System.exit(3);
		}
		System.out.println("\\_ cleaned "+cleanedFiles+" of "+totalFiles+" files");
	}

	public static boolean clean(Settings settings) throws Exception {
		boolean cleanHasErrors = false;
		String path = settings.getPath();
		if(settings.isDirectory()) {
			if(settings.isRecursive()) {
				doCleanDirectoryRecursively(settings, path);
			}else {
				doCleanDirectory(settings, path);
			}
		}else {
			boolean result = doClean(settings, path, true);
			if(!result) {
				cleanHasErrors=true;
			}
		}
		return cleanHasErrors;
	}

	private static void doCleanDirectory(Settings settings, String path)
			throws IOException, MalformedURLException, Exception {
		path = Helpers.appendSlashIfNone(path);
		File[] files = new File(path).listFiles();
		for (int i = 0; i < files.length; i++) {
			if(files[i].isFile()) {
				String pathS = files[i].getAbsolutePath();
				doClean(settings, pathS, false);
			}
		}
	}

	private static void doCleanDirectoryRecursively(Settings settings, String path)
			throws IOException, MalformedURLException, Exception {
		Stream<Path> pathStream = Files.walk(Paths.get(path))
			.filter(Files::isRegularFile);
		Iterator<Path> pathIterator = pathStream.iterator();
		
		while(pathIterator.hasNext()) {
			Path pathP = pathIterator.next();
			doClean(settings, pathP.toString(), false);
		}
	}
	
	private static boolean doClean(Settings settings, String path, boolean escalateUnsupported)
			throws IOException, MalformedURLException, Exception {
		totalFiles++;
		String filePathRelative = settings.isDirectory()?path.replace(settings.getPath(), ""):path;
		String filePathRelativePrintable = filePathRelative.startsWith(File.separator)?filePathRelative.substring(1):filePathRelative;
		System.out.println("  |_ '"+filePathRelativePrintable+"'");
		if(Helpers.isAlreadyClean(settings, path)) {
			System.out.println("    |_ skipping '"+path+"' as assumed it was already cleaned (ends with suffix '"+settings.getSuffix()+"')");
			return true;
		}
		
		String pathLower = path.toLowerCase();
		int lastDot = path.lastIndexOf(".") + 1;
		String fileEnding = pathLower.substring(lastDot);
		if (lastDot == 0 && !settings.isFileEndingForced()) {
			if(escalateUnsupported) {
				System.out.println("    |_ requiring a file ending (no dot found)");
				return false;
			}else {
				System.out.println("    |_ skipping '"+path+"' - requiring a file ending (no dot found)");
				return true;
			}
		}
		
		if(settings.isFileEndingForced()) {
			fileEnding=settings.getFileEndingForced();
		}
		
		double fileSizeBefore = Helpers.getFileSizeB(path);
		
		if (fileEnding.equals("jpg") || fileEnding.equals("jpeg")) {
			Strip.image(path, "jpg", settings);
		} else if (fileEnding.equals("png")) {
			Strip.image(path, "png", settings);
		} else if (fileEnding.equals("pdf")) {
			Strip.pdf(path, settings);
		} else if (fileEnding.equals("odt") || fileEnding.equals("ods") || fileEnding.equals("odp")) {
			Strip.openDoc(path, settings);
		} else if (fileEnding.equals("doc") || fileEnding.equals("xls") || fileEnding.equals("ppt")) {
			Strip.msDoc97(path, settings);
		} else if (fileEnding.equals("docx")) {
			Strip.msDocOpenXML(path, settings);
		} else if (fileEnding.equals("xlsx")) {
			Strip.msSheetOpenXML(path, settings);
		} else if (fileEnding.equals("xml")) {
			Strip.xmlComments(path, settings);
		} else {
			System.out.println("    |_ metacleaner does not (yet - feel free to open a GitHub issue) support the file type '"
					+ fileEnding + "'. If the file ending differs the actual file type, use the -f option.");
			return !escalateUnsupported;
		}
		Helpers.matchedEnding(fileEnding,escalateUnsupported);
		double fileSizeAfter = Helpers.getFileSizeB(settings.getOutputPath(path));
		double fileSizeDiff = fileSizeBefore-fileSizeAfter;
		String fileSizeDiffTrimmed = new java.text.DecimalFormat("0").format(fileSizeDiff);
		cleanedFiles++;
		System.out.println("    |_ cleaned document (stripped "+fileSizeDiffTrimmed+" B) successfully written to \'" + settings.getOutputPath(path)+"\'");
		return true;
	}
}

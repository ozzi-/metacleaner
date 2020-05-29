package metacleaner;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;


public class Cleaner {

	// TODO add ZIP support - https://github.com/KittyHawkCorp/stripzip/ ?
	// TODO check filesize before and after to determine if any meta was actually removed
	// TODO nicer ascii tree structure 
	
	public static void main(String[] args) {
		System.out.println("metacleaner - github.com/ozzi-/metacleaner");
		System.out.println("------------------------------------------");
		try {
			Settings settings = Helpers.parseCLIArgs(args);
			clean(settings);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exiting (1)");
			System.exit(1);
		}
		System.out.println("\\_ done");
	}

	public static void clean(Settings settings) throws Exception {
		String path = settings.getPath();
		if(settings.isDirectory()) {
			if(settings.isRecursive()) {
				doCleanDirectoryRecursively(settings, path);
			}else {
				doCleanDirectory(settings, path);
			}
		}else {
			doClean(settings, path, true);			
		}
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
	
	private static void doClean(Settings settings, String path, boolean throwUnsupported)
			throws IOException, MalformedURLException, Exception {
		String filePathRelative = settings.isDirectory()?path.replace(settings.getPath(), ""):path;
		System.out.println("  |_ '"+filePathRelative+"'");
		if(Helpers.isAlreadyClean(settings, path)) {
			System.out.println("    |_ skipping '"+path+"' as assumed it was already cleaned (ends with suffix '"+settings.getSuffix()+"')");
			return;
		}
		
		String pathLower = path.toLowerCase();
		int lastDot = path.lastIndexOf(".") + 1;
		String fileEnding = pathLower.substring(lastDot);
		if (lastDot == 0) {
			if(throwUnsupported) {
				throw new UnsupportedOperationException("    |_ requiring a file ending (no dot found)");				
			}else {
				System.out.println("    |_ skipping '"+path+"' - requiring a file ending (no dot found)");
				return;
			}
		}

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
			if (throwUnsupported) {
				throw new UnsupportedOperationException("    |_ metacleaner does not (yet) support the file type '"
						+ fileEnding + "'. feel free to open a GitHub issue!");
			} else {
				System.out.println("    |_ skipping file '" + path + "' due to unsupported file type.");
				return;
			}
		}
		Helpers.matchedEnding(fileEnding,throwUnsupported);
		System.out.println("    |_ cleaned document successfully written to " + settings.getOutputPath(path));
	}


}

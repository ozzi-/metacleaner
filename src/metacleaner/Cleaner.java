package metacleaner;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.odftoolkit.simple.*;
import org.odftoolkit.simple.meta.Meta;


public class Cleaner {

	public static void main(String[] args) {
		// TODO parse args
		Overwrite ov = new Overwrite(true, "_cleaned");

		try {
			clean("C:\\Users\\ozzi\\Desktop\\example.odt", ov);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void clean(String path, Overwrite overwrite) throws Exception {
		File file = new File(path);
		if (!file.exists()) {
			throw new FileNotFoundException("File '" + path + "' not found or readable");
		}

		String pathLower = path.toLowerCase();
		int lastDot = path.lastIndexOf(".") + 1;
		String fileEnding = pathLower.substring(lastDot);

		// TODO MS Office
		// TODO ZIP
		// TODO PSD
		// TODO XSD
		
		if (fileEnding.equals("jpg") || fileEnding.equals("jpeg")) {
			System.out.println("File ending found: JPG");
			stripImage(path, "jpg", overwrite);
		} else if (fileEnding.equals("png")) {
			System.out.println("File ending found: PNG");
			stripImage(path, "png", overwrite);
		} else if (fileEnding.equals("pdf")) {
			System.out.println("File ending found: PDF");
			stripPDF(path, overwrite);
		} else if (fileEnding.equals("odt") || fileEnding.equals("ods") || fileEnding.equals("odp")) {
			System.out.println("File ending found: ODT/ODS/ODP");
			stripOpenDoc(path, overwrite);
		} else {
			throw new UnsupportedOperationException("File type '" + fileEnding + "' is not implemented");
		}
		System.out.println("Written clean document to " + overwrite.getPath(path));
	}
	
	private static void stripOpenDoc(String path, Overwrite overwrite) throws MalformedURLException, IOException, Exception {
		TextDocument odfDoc = TextDocument.loadDocument(new File(path));
		Meta metadata = odfDoc.getOfficeMetadata();
			
		List<String> userDefinedNames = metadata.getUserDefinedDataNames();
		for (String userDefinedName : userDefinedNames) {
			metadata.removeUserDefinedDataByName(userDefinedName);
		}
	
		metadata.setCreationDate(Calendar.getInstance());
		metadata.setCreator("");
		metadata.setDcdate(Calendar.getInstance());
		metadata.setDescription("");
		metadata.setEditingCycles(0);
		metadata.setGenerator("");
		metadata.setInitialCreator("");
		metadata.setKeywords(new ArrayList<String>());
		metadata.setLanguage("");
		metadata.setPrintDate(Calendar.getInstance());
		metadata.setPrintedBy("");
		metadata.setSubject("");
		metadata.setTitle("");
		
		odfDoc.save(overwrite.getPath(path));
		odfDoc.close();
	}

	private static void stripImage(String path, String outType, Overwrite overwrite) {
		BufferedImage image;
		try {
			image = ImageIO.read(new File(path));
			String writeToPath = overwrite.getPath(path);
			ImageIO.write(image, outType, new File(writeToPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void stripPDF(String path, Overwrite overwrite) throws IOException {
		PDDocument document = PDDocument.load(new File(path));
		// clean basic
		PDDocumentInformation empty = new PDDocumentInformation();
		document.setDocumentInformation(empty);
		// clean XMP
		PDDocumentCatalog catalog = document.getDocumentCatalog();
		PDMetadata newMetadata = new PDMetadata(document, new ByteArrayInputStream("".getBytes()));
		catalog.setMetadata(newMetadata);
		
		
		document.save(overwrite.getPath(path));
	}
}


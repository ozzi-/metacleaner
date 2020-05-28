package metacleaner;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hpsf.CustomProperties;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.odftoolkit.simple.*;
import org.odftoolkit.simple.meta.Meta;

public class Cleaner {

	public static void main(String[] args) {
		// TODO parse args
		Overwrite ov = new Overwrite(true, "_cleaned",true);
		
		try {
			clean("C:\\Users\\ozzi\\Desktop\\Grobplanung v4.xlsx", ov);
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
		} else if (fileEnding.equals("doc") || fileEnding.equals("xls") || fileEnding.equals("ppt")) {
			System.out.println("File ending found: DOC/XLS/PPT");
			stripMSDoc(path, overwrite);
		} else if (fileEnding.equals("docx")) {
			System.out.println("File ending found: DOCX");
			stripMSDocNew(path, overwrite);
		} else if (fileEnding.equals("xlsx")) {
			System.out.println("File ending found: XLSX");
			stripMSDocExcelNew(path, overwrite);
		} else {
			throw new UnsupportedOperationException("File type '" + fileEnding + "' is not implemented");
		}
		System.out.println("Written clean document to " + overwrite.getPath(path));
	}
	
	private static void stripMSDocExcelNew(String path, Overwrite overwrite) throws EncryptedDocumentException, IOException {
		XSSFWorkbook wb = (XSSFWorkbook) WorkbookFactory.create(new File(path));
		System.out.println(wb.getActiveSheetIndex()+" "+wb.getAllNames().toString());
        POIXMLProperties props = wb.getProperties();

        POIXMLProperties.CoreProperties coreProp = props.getCoreProperties();
        props.clea
        coreProp.setCreator("Thinktibits");
        coreProp.setDescription("set Metadata using Apache POI / Java");
        coreProp.setCategory("Programming"); // category

        POIXMLProperties.CustomProperties custProp = props.getCustomProperties();
        custProp.addProperty("Author", "Thinktibits");// String
        custProp.addProperty("Year", 2014); // Number Property
        custProp.addProperty("Published", true); // Yes No Property
        custProp.addProperty("Typist", "tika");
        FileOutputStream fos = new FileOutputStream(overwrite.getPath(path));
        wb.write(fos);
        fos.close();		
	}

	private static void stripMSDocNew(String path, Overwrite overwrite) throws Exception {
		XWPFDocument doc = new XWPFDocument(OPCPackage.open(path));
		System.out.println(doc.getPart());
		doc.close();
	}

	private static void stripMSDoc(String path, Overwrite overwrite) throws Exception {
		InputStream is = new FileInputStream(path);
		POIFSFileSystem poifs = new POIFSFileSystem(is);
		is.close();
		DirectoryEntry dir = poifs.getRoot();
		clearEntries(dir,overwrite);
		OutputStream out = new FileOutputStream(overwrite.getPath(path));
		poifs.writeFilesystem(out);
		out.close();
		poifs.close();
	}

	private static void clearEntries(DirectoryEntry dir, Overwrite overwrite) throws Exception {
		DocumentSummaryInformation dsi;
		try {
			Set<String> entryNamesSet = dir.getEntryNames();
			ArrayList<String> entryNames = new ArrayList<>(entryNamesSet);
			for (String entryName : entryNames) {
				Entry entry = dir.getEntry(entryName);
				if (entry.isDocumentEntry()) {
					DocumentEntry dsiEntry = (DocumentEntry) dir.getEntry(entry.getName());
						DocumentInputStream dis = new DocumentInputStream(dsiEntry);
						PropertySet ps = new PropertySet();
						// try to set summary information
						try {
							ps = new PropertySet(dis);						
							SummaryInformation si = null;
							try {
								si = new SummaryInformation(ps);								
							}catch (Exception e) {}
							if(si!=null) {
								si.setApplicationName("");
								System.out.println(si.getAuthor()+"PRE");
								si.setAuthor("");
								si.setCreateDateTime(Calendar.getInstance().getTime());
								si.setEditTime(0);
								si.setKeywords("");
								si.setLastAuthor("");
								si.setLastPrinted(Calendar.getInstance().getTime());
								si.setLastSaveDateTime(Calendar.getInstance().getTime());
								si.setOSVersion(0);
								si.setTemplate("");
								if(overwrite.isHarshMode()) {
									si.setSubject("");
									si.setTitle("");						    	
								}
								si.write(dir, entry.getName());
							}
						}catch (Exception e) {
							dis.close();
						}
						// try to set document summary information
						boolean success = false;
						try {
							ps = new PropertySet(dis);
							success = true;
						}catch (Exception e) {
							dis.close();
						}
						if(ps!=null && success) {
							dis.close();						    
							if(ps.isDocumentSummaryInformation()) {
								dsi = new DocumentSummaryInformation(ps);
								dsi.setCustomProperties(new CustomProperties());
								dsi.setApplicationVersion(0);
								dsi.setCategory("");
								dsi.setCompany("lmao");
								dsi.setContentStatus("");
								dsi.setContentType("");
								dsi.setDocumentVersion("");
								if(overwrite.isHarshMode()) {
									dsi.setLanguage("");									
								}
								dsi.setManager("");
								dsi.setOSVersion(0);
								dsi.write(dir, entry.getName());
							}
						}				
				} else if (entry.isDirectoryEntry()) {
					DirectoryEntry b = (DirectoryEntry) entry;
					clearEntries(b, overwrite);
				}
			}
		} catch (FileNotFoundException ex) {
		}
	}

	private static void stripOpenDoc(String path, Overwrite overwrite)
			throws MalformedURLException, IOException, Exception {
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
		if(overwrite.isHarshMode()) {
			metadata.setSubject("");
			metadata.setTitle("");			
		}

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
		// TODO harshmode, don't strip title? does pdf have those fields?
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

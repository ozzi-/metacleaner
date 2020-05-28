package metacleaner;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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
import org.apache.poi.ooxml.POIXMLProperties.CoreProperties;
import org.apache.poi.ooxml.POIXMLProperties.ExtendedProperties;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.odftoolkit.simple.*;
import org.odftoolkit.simple.meta.Meta;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperties;

public class Cleaner {

	public static void main(String[] args) {
		// TODO parse args
		Overwrite ov = new Overwrite(true, "_cleaned", true);

		try {
			clean("C:\\Users\\ozzi\\Desktop\\custom_properties.xlsx", ov);
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

		// TODO ZIP - https://github.com/KittyHawkCorp/stripzip/ ?

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
		} else if (fileEnding.equals("xml")) {
			System.out.println("File ending found: XML");
			stripXMLComments(path, overwrite);
		} else {
			throw new UnsupportedOperationException("File type '" + fileEnding + "' is not implemented");
		}
		System.out.println("Written clean document to " + overwrite.getPath(path));
	}

	private static void stripXMLComments(String path, Overwrite overwrite) throws IOException {
		String xml = readLbL(path);
		xml = xml.replaceAll("(?s)<!--.*?-->", "");
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(overwrite.getPath(path)), StandardCharsets.UTF_8));
		try {
			out.write(xml);
		} finally {
			out.close();
		}
	}

	private static String readLbL(String filePath) {
		StringBuilder contentBuilder = new StringBuilder();
		try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
			stream.forEach(s -> contentBuilder.append(s).append("\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentBuilder.toString();
	}

	private static void stripMSDocExcelNew(String path, Overwrite overwrite)
			throws EncryptedDocumentException, IOException {
		
		XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(path));
		POIXMLProperties props = wb.getProperties();

		POIXMLProperties.CoreProperties coreProp = props.getCoreProperties();
		coreProp.setCategory("");
		coreProp.setContentStatus("");
		coreProp.setContentType("");
		coreProp.setCreated("");
		coreProp.setCreator("");
		coreProp.setDescription("");
		coreProp.setIdentifier("");
		coreProp.setKeywords("");
		coreProp.setLastModifiedByUser("");
		coreProp.setLastPrinted("");
		coreProp.setModified("");
		coreProp.setRevision("");
		coreProp.setSubjectProperty("");
		// TODO harshmode excludes
		coreProp.setTitle("");

		POIXMLProperties.CustomProperties custProp = props.getCustomProperties();

		removeUnderlyingProps(custProp,0);
	
		// custProp.addProperty("Author", "test");

		ExtendedProperties extendedProperties = props.getExtendedProperties();
		extendedProperties.setApplication("");
		extendedProperties.setAppVersion("");
		extendedProperties.setCharacters(1);
		extendedProperties.setCompany("");
		extendedProperties.setManager("");
		extendedProperties.setTemplate("");
		extendedProperties.setTotalTime(0);

		FileOutputStream fos = new FileOutputStream(overwrite.getPath(path));
		wb.write(fos);
		fos.close();
		wb.close();
	}


	
	private static void removeUnderlyingProps(POIXMLProperties.CustomProperties custProp, int depth) {
		CTProperties underlying = custProp.getUnderlyingProperties();
		for (int i = 0; i < underlying.sizeOfPropertyArray(); i++) {
			underlying.removeProperty(i);
		}
		// this needs to be done as XLSX seem to have trouble removing all custom properties in the first go
		// DOCX was fine however.
		if(underlying.sizeOfPropertyArray()>0) {
			depth++;
			if(depth>10) {
				System.out.println("Could not remove all underlying custom properties as reached max recursion depth");
			}
			removeUnderlyingProps(custProp,depth);
		}
	}

	private static void stripMSDocNew(String path, Overwrite overwrite) throws Exception {
		XWPFDocument docx = new XWPFDocument(new FileInputStream(path));
		POIXMLProperties properties = docx.getProperties();
		
		CoreProperties coreProperties = properties.getCoreProperties();
		coreProperties.setCategory("");
		coreProperties.setContentStatus("");
		coreProperties.setContentType("");
		coreProperties.setCreated("");
		coreProperties.setCreator("");
		coreProperties.setDescription("");
		coreProperties.setIdentifier("");
		coreProperties.setKeywords("");
		coreProperties.setLastModifiedByUser("");
		coreProperties.setLastPrinted("");
		coreProperties.setModified("");
		coreProperties.setRevision("");
		coreProperties.setSubjectProperty("");
		// TODO harshmode excludes
		coreProperties.setTitle("");
		
		org.apache.poi.ooxml.POIXMLProperties.CustomProperties customProperties = properties.getCustomProperties();
		removeUnderlyingProps(customProperties,0);

		ExtendedProperties extendedProperties = properties.getExtendedProperties();
		extendedProperties.setApplication("");
		extendedProperties.setAppVersion("");
		extendedProperties.setCharacters(1);
		extendedProperties.setCompany("");
		extendedProperties.setManager("");
		extendedProperties.setTemplate("");
		extendedProperties.setTotalTime(0);
				
		FileOutputStream fos = new FileOutputStream(overwrite.getPath(path));
		docx.write(fos);
		fos.close();
		docx.close();

	}

	private static void stripMSDoc(String path, Overwrite overwrite) throws Exception {
		InputStream is = new FileInputStream(path);
		POIFSFileSystem poifs = new POIFSFileSystem(is);
		is.close();
		DirectoryEntry dir = poifs.getRoot();
		clearEntries(dir, overwrite);
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
						} catch (Exception e) {
						}
						if (si != null) {
							si.setApplicationName("");
							System.out.println(si.getAuthor() + "PRE");
							si.setAuthor("");
							si.setCreateDateTime(Calendar.getInstance().getTime());
							si.setEditTime(0);
							si.setKeywords("");
							si.setLastAuthor("");
							si.setLastPrinted(Calendar.getInstance().getTime());
							si.setLastSaveDateTime(Calendar.getInstance().getTime());
							si.setOSVersion(0);
							si.setTemplate("");
							if (overwrite.isHarshMode()) {
								si.setSubject("");
								si.setTitle("");
							}
							si.write(dir, entry.getName());
						}
					} catch (Exception e) {
						dis.close();
					}
					// try to set document summary information
					boolean success = false;
					try {
						ps = new PropertySet(dis);
						success = true;
					} catch (Exception e) {
						dis.close();
					}
					if (ps != null && success) {
						dis.close();
						if (ps.isDocumentSummaryInformation()) {
							dsi = new DocumentSummaryInformation(ps);
							dsi.setCustomProperties(new CustomProperties());
							dsi.setApplicationVersion(0);
							dsi.setCategory("");
							dsi.setCompany("lmao");
							dsi.setContentStatus("");
							dsi.setContentType("");
							dsi.setDocumentVersion("");
							if (overwrite.isHarshMode()) {
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
		if (overwrite.isHarshMode()) {
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

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

import java.util.ArrayList;
import java.util.Calendar;
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
import org.apache.poi.ooxml.POIXMLProperties.ExtendedProperties;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.odftoolkit.simple.*;
import org.odftoolkit.simple.meta.Meta;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperties;

public class Cleaner {

	
	// TODO add ZIP support - https://github.com/KittyHawkCorp/stripzip/ ?
	// TODO add directory support
	
	public static void main(String[] args) {
		System.out.println("metacleaner - github.com/ozzi-/metacleaner");
		System.out.println("------------------------------------------");
        Settings settings = Helpers.parseCLIArgs(args);
		try {
			clean(settings);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}


	public static void clean(Settings settings) throws Exception {
		String path = settings.getPath();
		File file = new File(path);
		if (!file.exists()) {
			throw new FileNotFoundException("File '" + path + "' not found or readable");
		}

		String pathLower = path.toLowerCase();
		int lastDot = path.lastIndexOf(".") + 1;
		String fileEnding = pathLower.substring(lastDot);
		if(lastDot==0) {
			throw new UnsupportedOperationException("\\_ i require a file ending (no dot found)");
		}

		if (fileEnding.equals("jpg") || fileEnding.equals("jpeg")) {
			matchedEnding(fileEnding);
			stripImage("jpg", settings);
		} else if (fileEnding.equals("png")) {
			matchedEnding(fileEnding);
			stripImage("png", settings);
		} else if (fileEnding.equals("pdf")) {
			matchedEnding(fileEnding);
			stripPDF(settings);
		} else if (fileEnding.equals("odt") || fileEnding.equals("ods") || fileEnding.equals("odp")) {
			matchedEnding(fileEnding);
			stripOpenDoc(settings);
		} else if (fileEnding.equals("doc") || fileEnding.equals("xls") || fileEnding.equals("ppt")) {
			matchedEnding(fileEnding);
			stripMSDoc(settings);
		} else if (fileEnding.equals("docx")) {
			matchedEnding(fileEnding);
			stripMSDocNew(settings);
		} else if (fileEnding.equals("xlsx")) {
			matchedEnding(fileEnding);
			stripMSDocExcelNew(settings);
		} else if (fileEnding.equals("xml")) {
			matchedEnding(fileEnding);
			stripXMLComments(settings);
		} else {
			throw new UnsupportedOperationException("\\_ metacleaner does not (yet) support the file type '" + fileEnding + "'. feel free to open a GitHub issue!");
		}
		System.out.println("\\_ cleaned document successfully written to " + settings.getOutputPath());
	}


	private static void matchedEnding(String fileEnding) {
		System.out.println("\\_ supporting '"+fileEnding+"' files - continuing");
	}

	private static void stripXMLComments(Settings settings) throws IOException {
		String xml = Helpers.readLbL(settings.getPath());
		xml = xml.replaceAll("(?s)<!--.*?-->", "");
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(settings.getOutputPath()), StandardCharsets.UTF_8));
		try {
			out.write(xml);
		} finally {
			out.close();
		}
	}

	private static void stripMSDocExcelNew(Settings settings)
			throws EncryptedDocumentException, IOException {
		
		XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(settings.getPath()));
		POIXMLProperties props = wb.getProperties();

		nullCoreProperties(props);

		POIXMLProperties.CustomProperties custProp = props.getCustomProperties();

		removeUnderlyingProps(custProp,0);
	
		// custProp.addProperty("Author", "test");

		nullExtendedProperties(props);

		FileOutputStream fos = new FileOutputStream(settings.getOutputPath());
		wb.write(fos);
		fos.close();
		wb.close();
	}


	private static void nullExtendedProperties(POIXMLProperties props) {
		ExtendedProperties extendedProperties = props.getExtendedProperties();
		extendedProperties.setApplication("");
		extendedProperties.setAppVersion("");
		extendedProperties.setCharacters(1);
		extendedProperties.setCompany("");
		extendedProperties.setManager("");
		extendedProperties.setTemplate("");
		extendedProperties.setTotalTime(0);
	}


	private static void nullCoreProperties(POIXMLProperties props) {
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

	private static void stripMSDocNew(Settings settings) throws Exception {
		XWPFDocument docx = new XWPFDocument(new FileInputStream(settings.getPath()));
		POIXMLProperties properties = docx.getProperties();
		
		nullCoreProperties(properties);
		
		org.apache.poi.ooxml.POIXMLProperties.CustomProperties customProperties = properties.getCustomProperties();
		removeUnderlyingProps(customProperties,0);

		nullExtendedProperties(properties);
				
		FileOutputStream fos = new FileOutputStream(settings.getOutputPath());
		docx.write(fos);
		fos.close();
		docx.close();

	}

	private static void stripMSDoc(Settings settings) throws Exception {
		InputStream is = new FileInputStream(settings.getPath());
		POIFSFileSystem poifs = new POIFSFileSystem(is);
		is.close();
		DirectoryEntry dir = poifs.getRoot();
		clearEntries(dir, settings);
		OutputStream out = new FileOutputStream(settings.getOutputPath());
		poifs.writeFilesystem(out);
		out.close();
		poifs.close();
	}

	private static void clearEntries(DirectoryEntry dir, Settings settings) throws Exception {
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
							if (settings.isHarshMode()) {
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
							if (settings.isHarshMode()) {
								dsi.setLanguage("");
							}
							dsi.setManager("");
							dsi.setOSVersion(0);
							dsi.write(dir, entry.getName());
						}
					}
				} else if (entry.isDirectoryEntry()) {
					DirectoryEntry b = (DirectoryEntry) entry;
					clearEntries(b, settings);
				}
			}
		} catch (FileNotFoundException ex) {
		}
	}

	private static void stripOpenDoc(Settings settings) throws MalformedURLException, IOException, Exception {
		TextDocument odfDoc = TextDocument.loadDocument(new File(settings.getPath()));
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
		if (settings.isHarshMode()) {
			metadata.setSubject("");
			metadata.setTitle("");
		}

		odfDoc.save(settings.getOutputPath());
		odfDoc.close();
	}

	private static void stripImage(String outType, Settings settings) {
		BufferedImage image;
		try {
			image = ImageIO.read(new File(settings.getPath()));
			String writeToPath = settings.getOutputPath();
			ImageIO.write(image, outType, new File(writeToPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void stripPDF(Settings settings) throws IOException {
		// TODO harshmode, don't strip title? does pdf have those fields?
		PDDocument document = PDDocument.load(new File(settings.getPath()));
		// clean basic
		PDDocumentInformation empty = new PDDocumentInformation();
		document.setDocumentInformation(empty);
		// clean XMP
		PDDocumentCatalog catalog = document.getDocumentCatalog();
		PDMetadata newMetadata = new PDMetadata(document, new ByteArrayInputStream("".getBytes()));
		catalog.setMetadata(newMetadata);

		document.save(settings.getOutputPath());
	}
}

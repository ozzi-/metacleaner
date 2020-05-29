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
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.meta.Meta;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperties;

public class Strip {
	static void xmlComments(String path, Settings settings) throws IOException {
		String xml = Helpers.readLbL(path);
		xml = xml.replaceAll("(?s)<!--.*?-->", "");
		Writer out = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(settings.getOutputPath(path)), StandardCharsets.UTF_8));
		try {
			out.write(xml);
		} finally {
			out.close();
		}
	}

	static void msSheetOpenXML(String path, Settings settings) throws EncryptedDocumentException, IOException {

		XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(path));
		POIXMLProperties props = wb.getProperties();

		nullCoreProperties(props);

		POIXMLProperties.CustomProperties custProp = props.getCustomProperties();

		removeUnderlyingProps(custProp, 0);
		nullExtendedProperties(props);

		// custProp.addProperty("Author", "test");

		FileOutputStream fos = new FileOutputStream(settings.getOutputPath(path));
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
		// this needs to be done as XLSX seem to have trouble removing all custom
		// properties in the first go
		// DOCX was fine however.
		if (underlying.sizeOfPropertyArray() > 0) {
			depth++;
			if (depth > 10) {
				System.out.println("    |_ could not remove all underlying custom properties as reached max recursion depth");
			}
			removeUnderlyingProps(custProp, depth);
		}
	}

	static void msDocOpenXML(String path, Settings settings) throws Exception {
		XWPFDocument docx = new XWPFDocument(new FileInputStream(path));
		POIXMLProperties properties = docx.getProperties();

		nullCoreProperties(properties);

		org.apache.poi.ooxml.POIXMLProperties.CustomProperties customProperties = properties.getCustomProperties();
		removeUnderlyingProps(customProperties, 0);

		nullExtendedProperties(properties);

		FileOutputStream fos = new FileOutputStream(settings.getOutputPath(path));
		docx.write(fos);
		fos.close();
		docx.close();

	}

	static void msDoc97(String path, Settings settings) throws Exception {
		InputStream is = new FileInputStream(path);
		POIFSFileSystem poifs = new POIFSFileSystem(is);
		is.close();
		DirectoryEntry dir = poifs.getRoot();
		clearEntries(dir, settings);
		OutputStream out = new FileOutputStream(settings.getOutputPath(path));
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

	static void openDoc(String path, Settings settings) throws MalformedURLException, IOException, Exception {
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
		if (settings.isHarshMode()) {
			metadata.setSubject("");
			metadata.setTitle("");
		}

		odfDoc.save(settings.getOutputPath(path));
		odfDoc.close();
	}

	static void image(String path, String outType, Settings settings) {
		BufferedImage image;
		try {
			image = ImageIO.read(new File(path));
			String writeToPath = settings.getOutputPath(path);
			ImageIO.write(image, outType, new File(writeToPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void pdf(String path, Settings settings) throws IOException {
		// TODO harshmode, don't strip title? does pdf have those fields?
		PDDocument document = PDDocument.load(new File(path));
		// clean basic
		PDDocumentInformation empty = new PDDocumentInformation();
		document.setDocumentInformation(empty);
		// clean XMP
		PDDocumentCatalog catalog = document.getDocumentCatalog();
		PDMetadata newMetadata = new PDMetadata(document, new ByteArrayInputStream("".getBytes()));
		catalog.setMetadata(newMetadata);

		document.save(settings.getOutputPath(path));
	}
}

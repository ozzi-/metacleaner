package metacleaner;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

public class Cleaner {
	
	public static void main(String[] args) {
		// TODO parse args
		Overwrite ov = new Overwrite(true, "_cleaned");
		
		try {
			clean("C:\\Users\\ozzi\\Desktop\\example.pdf",ov);
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void clean(String path, Overwrite overwrite) throws IOException {
		File file = new File(path);
		if(!file.exists()) {
			throw new FileNotFoundException("File '"+path+"' not found or readable");
		}
		
		String pathLower = path.toLowerCase();
		int lastDot = path.lastIndexOf(".")+1;
		String fileEnding = pathLower.substring(lastDot);
		
		if(fileEnding.equals("jpg") || fileEnding.equals("jpeg")) {
			System.out.println("File ending found: JPG");
			stripImage(path, "jpg", overwrite);
		}else if(fileEnding.equals("png")) {
			System.out.println("File ending found: PNG");
			stripImage(path, "png", overwrite);
		}else if(fileEnding.equals("pdf")) {
			System.out.println("File ending found: PDF");
			stripPDF(path,overwrite);
		}else {
			throw new UnsupportedOperationException("File type '"+fileEnding+"' is not implemented");			
		}
		System.out.println("Written clean document to "+overwrite.getPath(path));
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
        PDDocumentInformation a = new PDDocumentInformation();
		document.setDocumentInformation(a);
		document.save(overwrite.getPath(path));
	}
}


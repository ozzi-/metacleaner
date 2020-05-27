package metacleaner;

public class Overwrite {
	private boolean overwrite;
	private String overwriteAppender;
	
	public Overwrite(boolean overwrite, String overwriteAppender) {
		this.overwrite=overwrite;
		this.overwriteAppender=overwriteAppender;
	}
	
	public String getPath(String path) {
		if(!overwrite) {
			return path;
		}
		int dotPos = path.lastIndexOf(".");
		String finalPath = path.substring(0,dotPos)+overwriteAppender+path.substring(dotPos);
		return finalPath;
	}

	public String getOverwriteAppender() {
		return overwriteAppender;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

}

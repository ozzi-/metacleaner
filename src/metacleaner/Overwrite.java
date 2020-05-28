package metacleaner;

public class Overwrite {
	private boolean overwrite;
	private String overwriteAppender;
	private boolean harshMode;
	
	public Overwrite(boolean overwrite, String overwriteAppender, boolean harshMode) {
		this.overwrite=overwrite;
		this.overwriteAppender=overwriteAppender;
		this.harshMode = harshMode;
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

	public boolean isHarshMode() {
		return harshMode;
	}

	public void setHarshMode(boolean harshMode) {
		this.harshMode = harshMode;
	}
}

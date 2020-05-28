package metacleaner;

public class Settings {
	private boolean overwrite;
	private String suffix;
	private boolean harshMode;
	private String path;
	
	public Settings(String path, boolean overwrite, String suffix, boolean harshMode) {
		this.path=path;
		this.overwrite=overwrite;
		this.suffix=suffix;
		this.harshMode = harshMode;
	}
	
	public String getOutputPath() {
		if(overwrite) {
			return path;
		}
		int dotPos = path.lastIndexOf(".");
		return path.substring(0,dotPos)+suffix+path.substring(dotPos);
	}

	public String getOverwriteAppender() {
		return suffix;
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

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}

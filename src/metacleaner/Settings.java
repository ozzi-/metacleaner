package metacleaner;

public class Settings {
	private boolean overwrite;
	private boolean isDirectory;
	private boolean recursive;
	private String suffix;
	private boolean harshMode;
	private String path;
	

	public Settings(String path, boolean isDirectory, boolean recursive, boolean overwrite, String suffix, boolean harshMode) {
		this.path=path;
		this.isDirectory=isDirectory;		
		this.recursive=recursive;
		this.overwrite=overwrite;
		this.suffix=suffix;
		this.harshMode = harshMode;
	}
	
	public String getOutputPathStrict(String path) {
		int dotPos = path.lastIndexOf(".");
		if(dotPos==-1) {
			return path+suffix;
		}
		return path.substring(0,dotPos)+suffix+path.substring(dotPos);
	}
	
	public String getOutputPath(String path) {
		if(overwrite) {
			return path;
		}
		int dotPos = path.lastIndexOf(".");
		if(dotPos==-1) {
			return path+suffix;
		}
		return path.substring(0,dotPos)+suffix+path.substring(dotPos);
	}

	public String getSuffix() {
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

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}
}

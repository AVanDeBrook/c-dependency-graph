package depgraph;

public class Configurator {

	public Configurator() {
		
	}
	
	public boolean isDirectory() {
		return true; // TODO
	}
	
	public String getFilePath() {
		System.out.println("Configurator providing location...");
		return "C:\\dummy\\location\\single\\file.DOT"; // TODO
	}
	
	public String getDirectory() {
		System.out.println("Configurator providing location...");
		return "C:\\dummy\\location\\directory"; // TODO 
	}
}

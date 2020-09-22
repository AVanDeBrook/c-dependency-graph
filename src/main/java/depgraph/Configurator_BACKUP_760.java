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
<<<<<<< HEAD
		// TODO get location of single file or directory from command line arguments
		manager.start(true, "C:\\Users\\Sammi\\Desktop\\Dot\\adc_8c_ae0b9ae6e4ef2dbf771dcc0ea30901ae2_cgraph.dot");
		manager.start(false, "C:\\Users\\Sammi\\Desktop\\Dot");
=======
		return "C:\\dummy\\location\\directory"; // TODO 
>>>>>>> fd5ca26875196f312dff4dcd2ae7ebcab2c5b96d
	}
}
package depgraph;

public class Configurator {

	Manager manager;

	public Configurator() {
		manager = new Manager();
	}

	public void provideLocation() {
		System.out.println("Configurator providing location...");
		// TODO get location of single file or directory from command line arguments
		manager.start(true, "C:\\dummy\\location\\single\\file.DOT");
//		manager.start(false, "C:\\dummy\\location\\directory");
	}
}

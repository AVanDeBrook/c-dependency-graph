package depgraph;

public class Configurator {

	Manager manager;

	public Configurator() {
		manager = new Manager();
		// TODO get location of single file or directory from command line
		manager.start(true, "C:\\dummy\\location\\single\\file.DOT");
		manager.start(false, "C:\\dummy\\location\\directory");
	}
}

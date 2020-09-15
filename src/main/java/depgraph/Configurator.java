package depgraph;

public class Configurator {

	Manager manager;

	public Configurator() {
		manager = new Manager();
		// TODO get location of single file or directory from command line
		manager.provideSingleFile("C:\\dummy\\location.DOT");
		manager.provideDirectory("C:\\dummy\\location");
	}
}

package depgraph;

public class Configurator {

	Manager manager;

	public Configurator() {
		manager = new Manager();
	}

	public void provideLocation() {
		System.out.println("Configurator providing location...");
		// TODO get location of single file or directory from command line arguments
		manager.start(true, "C:\\Users\\Sammi\\Desktop\\Dot\\adc_8c_ae0b9ae6e4ef2dbf771dcc0ea30901ae2_cgraph.dot");
		manager.start(false, "C:\\Users\\Sammi\\Desktop\\Dot");
	}
}
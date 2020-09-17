package depgraph;

import java.util.List;

public class Manipulator {

	Manager manager;

	public Manipulator() {
		manager = new Manager();
	}

	public Graph manipulate(Graph graph) {
		System.out.println("Manipulator manipulating graph...");
		// TODO
		removeConnection();
		addConnection();
		return graph;
	}

	public void removeConnection() {
		// TODO
	}

	public void addConnection() {
		// TODO
	}
}

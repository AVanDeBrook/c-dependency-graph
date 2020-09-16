package depgraph;

import java.util.ArrayList;
import java.util.List;

public class Graph {

	List<Module> modules;

	public Graph() {
		modules = new ArrayList<Module>();
	}

	public void addModule(Module module) {
		System.out.println("Adding module to graph...");
		modules.add(module);
	}

	public List<Module> getModules() {
		return modules;
	}
}

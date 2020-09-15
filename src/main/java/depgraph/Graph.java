package depgraph;

import java.util.List;

public class Graph {

	List<Module> modules;

	public Graph() {

	}

	public void addModule(Module module) {
		modules.add(module);
	}

	public List<Module> getModules() {
		return modules;
	}
}

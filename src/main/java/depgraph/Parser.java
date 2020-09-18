package depgraph;

public class Parser {

	public Parser() {
		
	}

	public Module parse(String singleFileContents) {
		System.out.println("Parser parsing single file...");
		// TODO parse
		Module module = new Module();
		Node node1 = new Node();
		module.addNode(node1);
		Node node2 = new Node();
		module.addNode(node2);
		return module;
	}
}

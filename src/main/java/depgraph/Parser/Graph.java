package depgraph.Parser;

public class Graph {
	private String name="";
    private String prefix="";

    public String [][] edgeAttributes;
    public String [][] nodeAttributes;
    public Node [] arrayOfNodes;

	public Graph() {
	}

	public String getName() {
		return name;
    }

    public void setName(String graphName){
        this.name=graphName;
    }

    public void setNodeAttributes(){

    }

	public String prefix() {
		return prefix;
	}
}

package depgraph.Parser;

import java.util.LinkedList;

public class Graph {
	private String name="";
    private String prefix="";

    public String [][] edgeAttributes;
    public String [][] nodeAttributes;
    public LinkedList <Node> arrayOfNodes = new LinkedList<Node>() ;

	public Graph() {
	}

    public void addNode(Node temp){
        boolean tempBool = true;

        for(int i = 0; i < arrayOfNodes.size(); i++){
            if(arrayOfNodes.get(i).getName()==temp.getName())
                tempBool = false;
        }

        if(tempBool){
            arrayOfNodes.add(temp);
        }
    }

	public String getName() {
		return name;
    }

    public void setName(String graphName){
        this.name=graphName;
    }

    public void setNodeAttributes(String [][] tempArray){
        this.nodeAttributes=tempArray;
    }

    public void setEdgeAttributes(String [][] tempArray){
        this.edgeAttributes = tempArray;
    }

    public void setPrefix(String pre){
        this.prefix = pre;
    }

	public String prefix() {
		return prefix;
	}
}

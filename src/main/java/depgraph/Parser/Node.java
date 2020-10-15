package depgraph.Parser;

import java.util.StringTokenizer;

public class Node {

    private String name;
    private String functionName;
    private String prefix;
    private int [] connection;
    private String [][] attributes;

	public Node() {

    }

    public void setName(String temp){
        this.name=temp;
    }

    public String getName(){
        return this.name;
    }

    public void setAttributes(String [][] temp){

        this.attributes = temp;
        this.functionName = this.lookForFunctionName();
        StringTokenizer st = new StringTokenizer(this.functionName,"_");
        this.prefix = st.nextToken();

    }

    private String lookForFunctionName(){
        int key = 0;
        int value = 1;
        String temp = "";
        for(int counter=0; counter < attributes.length; counter++){
            if(attributes[counter][key].matches("label")){
                return attributes[counter][value];
            }
        }
        return temp;
    }
}

package depgraph.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Parser {

	private Lexer lexer;

	/**
	 * No-arg constructor.
	 */
	public Parser() {
		lexer = new Lexer();
	}

	/**
	 * Kick off point for parsing, used by Manager.
	 */
	public ArrayList<Graph> parse(List<String> fileContents) {
		ArrayList<Graph> graphList = new ArrayList<Graph>();

		for (String singleFileContents : fileContents) {
			graphList.add(this.parse(singleFileContents));
		}

		return graphList;
	}

	/**
	 * Parses file contents and creates a graph object.
	 *
	 * @param fileContents - A string containing the contents of a single DOT
	 * file
	 * @return A graph object representing the relevant values found in the DOT
	 * file
	 */
	private Graph parse(String fileContents) {
		String[] lines = fileContents.split("\n");
		// Token token = null;
        Graph functionGraph = new Graph();

		for (String line : lines) {
			//System.out.println(lexer.tokenize(line));

            switch(lexer.tokenize(line).getToken()){
                case DIGRAPH_DEF:
                    functionGraph.setName(lexer.tokenize(line).getValue());
                    System.out.println("\nFunction evaluated "+lexer.tokenize(line).getValue());
                    break;
                case L_BRACE:
                    System.out.println("\nFunction entered ...");
                    break;
                case R_BRACE:
                    System.out.println("\nFunction exited ...");
                    break;
                case NODE_ATTR_STMT:
                    this.splitIntoKeyValuePairs(lexer.tokenize(line).getValue());
                    break;
                case EDGE_ATTR_STMT:
                    break;
                case NODE_STMT:
                    break;
                case EDGE_STMT:
                    break;
                case IGNORED:
                    break;
                case NONE:
                    break;
                default:
                    break;

            }
		}

		// temp return to satisfy errors/warnings
		return new Graph();
    }

    private String [][] splitIntoKeyValuePairs(String unfilteredString){

        StringTokenizer multiTokenizer = new StringTokenizer(unfilteredString,"[]=,\"");
        int arraySize = multiTokenizer.countTokens()/2;
        String [][] tempArray = new String[arraySize][arraySize];
        int key =0;
        int value=1;
        int counter=0;

        while(multiTokenizer.hasMoreElements()){
            tempArray[counter][key] = multiTokenizer.nextToken();
            tempArray[counter][value] = multiTokenizer.nextToken();
            System.out.println("\nKey: "+ tempArray[counter][key] + "\tValue: "+tempArray[counter][value]);
            counter++;
        }

        return tempArray;
    }


}

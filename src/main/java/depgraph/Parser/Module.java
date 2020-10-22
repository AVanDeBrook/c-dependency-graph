package depgraph.Parser;

import java.util.ArrayList;

public class Module {

    /**
     * List of all nodes in this specific module that match the module prefix.
     */
    private ArrayList<Node> nodes;

    private String modulePrefix;

    /**
     * No-arg constructor.
     */
    public Module() {
        this.nodes = new ArrayList<Node>();
        this.modulePrefix = "";
    }

    /**
     * Create an object with a specific module prefix.
     *
     * @param modulePrefix
     */
    public Module(String modulePrefix) {
        this.nodes = new ArrayList<Node>();
        this.modulePrefix = modulePrefix;
    }

    /**
     * Wrapper for the Collection.add function. Adds a node to this module.
     *
     * @param n Node to be appened to node list.
     * @return  true (as specified by Collection.add).
     */
    public boolean add(Node n) {
        return nodes.add(n);
    }

    /* Setters and Getters */

    public ArrayList<Node> getNodes() {
        return this.nodes;
    }

    public String getModulePrefix() {
        return this.modulePrefix;
    }

    public void setModulePrefix(String modulePrefix) {
        this.modulePrefix = modulePrefix;
    }

    @Override
    public String toString() {
        return String.format("MODULE: %s\nSIZE %d\n", this.modulePrefix, this.nodes.size());
    }
}

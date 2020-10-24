package depgraph.Configurator;

/**
 * Possible return types from the Configurator class.
 * This is necessary to determine what/how to pass data to the next class (Reader).
 */
public enum ConfigType {
	FILE,
	DIRECTORY,
	NONE
}

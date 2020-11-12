package depgraph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import depgraph.Configurator.ConfigType;
import depgraph.Configurator.Configurator;

public class TestConfigurator {
	@Test
	public void testSingleFileReturnsFile() {
		String[] args = { "-s", "test/dot-files/adc_8c_ae0b9ae6e4ef2dbf771dcc0ea30901ae2_cgraph.dot" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.FILE, config.manageCmdLineArguments(args));
	}

	@Test
	public void testInvalidFileReturnsNone() {
		String[] args = { "-s", "stupid/dummy/directory/that/will/never/be/found" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));
	}

	@Test
	public void testDirectoryReturnsDirectory() {
		String[] args = { "-d", "test/dot-files" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.DIRECTORY, config.manageCmdLineArguments(args));
	}

	@Test
	public void testInvalidDirectoryReturnsNone() {
		String[] args = { "-d", "stupid/dummy/directory/that/will/never/be/found" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));
	}

	@Test
	public void testSevereVerbosity() {
		String[] args = { "-v", "0" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO run app and check verbosity level
	}

	@Test
	public void testWarningVerbosity() {
		String[] args = { "-v", "1" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO run app and check verbosity level
	}

	@Test
	public void testInfoVerbosity() {
		String[] args = { "-v", "2" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO run app and check verbosity level
	}

	@Test
	public void testFineVerbosity() {
		String[] args = { "-v", "3" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO run app and check verbosity level
	}

	@Test
	public void testInvalidVerbosity() {
		String[] args = { "-v", "4" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO
	}

	@Test
	public void testHelpMenu() {
		String[] args = { "-h" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO
	}

	@Test
	public void testInvalidHelpMenu() {
		String[] args = { "-h", "invalid text following flag" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO
	}

	@Test
	public void testOutputFileName() {
		String[] args = { "-L", "TODO" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO
	}

	@Test
	public void testInvalidOutputFileName() {
		String[] args = { "-o", "TODO" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO
	}

	@Test
	public void testOutputLogFile() {
		String[] args = { "-L", "TODO" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO
	}

	@Test
	public void testInvalidOutputLogFile() {
		String[] args = { "-L", "TODO" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO
	}

	@Test
	public void testInvalidFlag() {
		String[] args = { "-x" };
		Configurator config = new Configurator();
		assertEquals(ConfigType.NONE, config.manageCmdLineArguments(args));

		// TODO
	}
}

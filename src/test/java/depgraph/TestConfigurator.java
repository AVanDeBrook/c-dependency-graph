package depgraph;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import depgraph.Configurator.*;

public class TestConfigurator {
    @Test
    public void testSingleFileReturnsFile() {
        String[] args = {"-s", "test/dot-files/adc_8c_ae0b9ae6e4ef2dbf771dcc0ea30901ae2_cgraph.dot"};
        Configurator config = new Configurator();
        assertEquals(ConfigReturnType.FILE, config.manageCmdLineArguments(args));
    }

    @Test
    public void testDirectoryReturnsDirectory() {
        String[] args = {"-d", "test/dot-files"};
        Configurator config = new Configurator();
        assertEquals(ConfigReturnType.DIRECTORY, config.manageCmdLineArguments(args));
    }

    @Test
    public void testInvalidFileReturnsNone() {
        String[] args = {"-s", "stupid/dummy/directory/that/will/never/be/found"};
        Configurator config = new Configurator();
        assertEquals(ConfigReturnType.NONE, config.manageCmdLineArguments(args));
    }

    @Test
    public void testInvalidDirectoryReturnsNone() {
        String[] args = {"-d", "stupid/dummy/directory/that/will/never/be/found"};
        Configurator config = new Configurator();
        assertEquals(ConfigReturnType.NONE, config.manageCmdLineArguments(args));
    }
}

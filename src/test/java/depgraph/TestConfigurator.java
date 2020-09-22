package depgraph;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;


public class TestConfigurator {
    @Test
    public void singleFileprovided() {
        Configurator config = new Configurator();
        String [] cmdArgs = {"-h", "-s", "fileName","-d","directoryName"};
        config.manageCmdLineArguments(cmdArgs);
        assertTrue(config.getFileName().equals("fileName"));
        assertTrue(config.getDirectoryName().equals("directoryName"));
    }
}

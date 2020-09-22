    package depgraph;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;


public class TestConfigurator {
    @Test
    public void singleFileprovided() {
        Configurator config = new Configurator();
        //source directory is C:\SeniorDesign\c-dependecy-graph\src\test\java\depgraph
        String [] cmdArgs = {"-h", "-s", ".\\src\\test\\java\\depgraph\\dot-files\\adc_8c_ae0b9ae6e4ef2dbf771dcc0ea30901ae2_cgraph.dot","-d",".\\src\\test\\java\\depgraph\\dot-files"};
        config.manageCmdLineArguments(cmdArgs);
        assertTrue(config.getFileName().equals(".\\src\\test\\java\\depgraph\\dot-files\\adc_8c_ae0b9ae6e4ef2dbf771dcc0ea30901ae2_cgraph.dot"));
        assertTrue(config.getDirectoryName().equals(".\\src\\test\\java\\depgraph\\dot-files"));
    }
}

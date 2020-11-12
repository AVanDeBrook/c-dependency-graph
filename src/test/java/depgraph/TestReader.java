package depgraph;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import depgraph.Reader.Reader;

public class TestReader {

	@Test
	public void testReadSingleFileDoesNotThrowException() {
		Reader reader = new Reader();
		assertDoesNotThrow(
				() -> reader.readSingleFile("test/dot-files/adc_8c_ae0b9ae6e4ef2dbf771dcc0ea30901ae2_cgraph.dot"));
	}

	@Test
	public void testReadBadSingleFileThrowsException() {
		Reader reader = new Reader();
		assertThrows(Exception.class, () -> reader.readSingleFile("dummy/file/path"));
	}

	@Test
	public void testReadDirectoryDoesNotThrowException() {
		Reader reader = new Reader();
		assertDoesNotThrow(() -> reader.readDirectory("test/dot-files"));
	}

	@Test
	public void testReadBadDirectoryThrowsException() {
		Reader reader = new Reader();
		assertThrows(Exception.class, () -> reader.readDirectory("dummy/file/path"));
	}
}

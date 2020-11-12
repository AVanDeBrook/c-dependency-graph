package depgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import depgraph.Reader.Reader;

public class TestReader {

	@Test
	public void testReadSingleFile() {
		Reader reader = new Reader();
		List<String> filesList = null;
		try {
			filesList = reader.readSingleFile("test/dot-files/adc_8c_ae0b9ae6e4ef2dbf771dcc0ea30901ae2_cgraph.dot");
		} catch (Exception e) {
			assertTrue(false);
		}
		assertNotNull(filesList);
		assertEquals(filesList.size(), 1);
		assertNotNull(filesList.get(0));
	}

	@Test
	public void testReadDirectory() {
		Reader reader = new Reader();
		List<String> filesList = null;
		try {
			filesList = reader.readDirectory("test/dot-files");
		} catch (Exception e) {
			assertTrue(false);
		}
		assertNotNull(filesList);
		assertTrue(filesList.size() > 0);
		for (String file : filesList) {
			assertNotNull(file);
		}
	}

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

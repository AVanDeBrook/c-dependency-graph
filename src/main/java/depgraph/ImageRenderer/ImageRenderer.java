package depgraph.ImageRenderer;

/**
 * Relatively simple class. It takes a format and output file name and and
 * creates and runs a process based off a run string generated by this class
 * (see createRunString).
 */
public class ImageRenderer {

	/**
	 * Runtime instace. Used to execute programs.
	 */
	private Runtime runtime;

	/**
	 * Used to keep track of the command line process until it has finished
	 * executing.
	 */
	private Process process;

	/**
	 * List of supported image types that dot can generate. See dot(1)
	 */
	public final String[] OUTPUT_TYPES = { "dot", "xdot", "ps", "pdf", "svg", "fig", "png", "gif", "jpg", "jpeg",
			"json", "imap", "cmapx" };

	/**
	 * No-arg constructor. Initializes the runtime instance.
	 */
	public ImageRenderer() {
		runtime = Runtime.getRuntime();
	}

	/**
	 * Default render option. Renders the graph as a PDF called "out.pdf".
	 */
	public void renderImage() {
		renderImage("pdf", "out.pdf");
	}

	/**
	 * Renders the graph as a pdf with the specified file name.
	 *
	 * @param fileName Name of the output file.
	 */
	public void renderImage(String fileName) {
		try {
			String format = getFormatFromFileExtension(fileName);
			renderImage(format, fileName);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Renders the graph image by creating a dot run string. Specifies the type and
	 * output file name.
	 *
	 * @param format   Format the image should be generated as.
	 * @param fileName Name of the output file.
	 */
	public void renderImage(String format, String fileName) {
		try {
			process = runtime.exec(createRunString(format, fileName));
			process.waitFor();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Checks if the file extension specified in the output file name is a valid by
	 * checking if it exists in the OUTPUT_TYPES class attribute.
	 *
	 * @param fileName Name of the output file.
	 * @return The type of image to render. See OUTPUT_TYPES and dot(1) for details.
	 * @throws Exception If the specified format is invalid.
	 */
	private String getFormatFromFileExtension(String fileName) throws Exception {
		String extension = fileName.split("\\.")[1];

		for (String type : OUTPUT_TYPES)
			if (extension.equals(type))
				return extension;

		throw new Exception("Invalid output format.");
	}

	/**
	 * Creates a run string from the format of the image and nmae of the output
	 * file.
	 *
	 * @param format   Image format.
	 * @param fileName Output file name.
	 * @return Valid run string to create a process from.
	 */
	private String createRunString(String format, String fileName) {
		return String.format("dot -T%s -o %s %s.dot", format, fileName, fileName.split("\\.")[0]);
	}
}
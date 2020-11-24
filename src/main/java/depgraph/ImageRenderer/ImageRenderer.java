package depgraph.ImageRenderer;

public class ImageRenderer {
    private Runtime runtime;
    private Process process;

    public ImageRenderer() {
        runtime = Runtime.getRuntime();
    }

    public void renderImage(String format, String fileName) {
        try {
            process = runtime.exec(createRunString(format, fileName));
            process.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String createRunString(String format, String fileName) {
        return String.format("dot -T%s -o out.%s out.dot", format, format, fileName);
    }
}

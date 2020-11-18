package depgraph.ImageRenderer;

public class ImageRenderer {

	public ImageRenderer() {
		DotInterface.INSTANCE.GW_Init();
	}

	public void readGraph(String graph) {
		DotInterface.INSTANCE.GW_ReadGraph(graph);
	}

	public void readGraphFromFile(String fileName) {
		DotInterface.INSTANCE.GW_ReadGraphFile(fileName);
	}

	public void renderImage(String format, String fileName) {
		DotInterface.INSTANCE.GW_RenderImage(format, fileName);
	}

	public void close() {
		DotInterface.INSTANCE.GW_Close();
	}
}

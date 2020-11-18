package depgraph.ImageRenderer;

import com.sun.jna.*;

public interface DotInterface extends Library {

	public DotInterface INSTANCE = (DotInterface) Native.load("gw", DotInterface.class);

	public void GW_Init();

	public void GW_ReadGraph(String graph);

	public void GW_ReadGraphFile(String fileName);

	public void GW_RenderImage(String format, String fileName);

	public void GW_Close();
}

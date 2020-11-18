#include "wrapper.h"
#include "general.h"

#include "graphviz/gvc.h"
#include "graphviz/cgraph.h"

static Agraph_t *graph;
static GVC_t *graphviz_context;

WRAPPER void GW_Init(void)
{
	graphviz_context = gvContext();
}

WRAPPER void GW_ReadGraph(const char *graph_string)
{
	graph = agmemread(graph_string);
}

WRAPPER void GW_ReadGraphFile(const char *file_name)
{
	FILE *graph_file = fopen(file_name, "r");
	graph = agread(graph_file, 0);
	fclose(graph_file);
}

WRAPPER void GW_RenderImage(const char *format, const char *file_name)
{
	gvLayout(graphviz_context, graph, "dot");
	gvRenderFilename(graphviz_context, graph, format, file_name);
}

WRAPPER void GW_Close(void)
{
	gvFreeLayout(graphviz_context, graph);
	agclose(graph);
}

#ifdef TEST
int main(int argc, char **argv)
{
	char buffer[UINT16_MAX];
	char *buffer_ptr = buffer;
	FILE *graph = fopen("../out.dot", "r");

	while (!feof(graph))
		*buffer_ptr++ = fgetc(graph);
	buffer_ptr = '\0';
	fclose(graph);

	GW_Init();
	GW_ReadGraph(buffer);
	GW_RenderImage("png", "out_s.png");

	GW_ReadGraphFile("../out.dot");
	GW_RenderImage("png", "out_f.png");

	GW_Close();
	return 0;
}
#endif

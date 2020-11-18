#ifndef _WRAPPER_H
#define _WRAPPER_H

/**
 * Initializes the gvc and cgraph contexts.
 */
extern void GW_Init(void);

/**
 * Reads a graph from a string.
 * Note that this is not a file from disk, but rather a string version of the
 * file (can be read from disk before hand or passed in from another class/module)
 *
 * @param graph Graph to be read by graphviz
 */
extern void GW_ReadGraph(const char *graph);

/**
 * Reads a graph from a file. This is intended to reduce the amount of I/O from
 * the primary application. A majority of the time it would be easier to pass the
 * graph as a string and never write it to disk, however, that is up to the user.
 *
 * @param file_name Name of the file to read from
 */
extern void GW_ReadGraphFile(const char *file_name);

/**
 * Renders the image according to the specified and outputs the graph as an
 * image called file_name.
 *
 * @param format Format of the image (e.g. PDF, GIF, PNG, JPG, etc.)
 * @param file_name Name of the file
 */
extern void GW_RenderImage(const char *format, const char *file_name);

/**
 * Releases all resources used by the API. Should be called before the program
 * exits.
 */
extern void GW_Close(void);

#endif

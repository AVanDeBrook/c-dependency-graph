#ifndef _GENERAL_H
#define _GENERAL_H

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdbool.h>

#ifndef TEST
	#ifdef _WIN32
		#define WRAPPER __declspec(dllexport)
	#else
		#define WRAPPER
	#endif
#else
#define WRAPPER
#endif
#endif
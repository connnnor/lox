#ifndef CLOX_COMMON_H
#define CLOX_COMMON_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>

#ifdef DEBUG
#define DEBUG_TRACE_EXECUTION
#define DEBUG_PRINT_CODE
#endif

#ifdef DEBUG_GC
#define DEBUG_STRESS_GC
#define DEBUG_LOG_GC
#endif

#define UINT8_COUNT (UINT8_MAX + 1)

// error codes
#define EXIT_SUCCESS 0
#define EXIT_MALLOC_ERROR 1
#define EXIT_FILE_ERROR 74
#define EXIT_COMPILE_ERROR 65
#define EXIT_RUNTIME_ERROR 70

#endif //CLOX_COMMON_H

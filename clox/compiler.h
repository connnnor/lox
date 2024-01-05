#ifndef CLOX_COMPILER_H
#define CLOX_COMPILER_H
#include "chunk.h"
#include "object.h"

obj_function_t *compile(const char *source);

#endif // CLOX_COMPILER_H

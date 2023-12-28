#ifndef CLOX_COMPILER_H
#define CLOX_COMPILER_H
#include "chunk.h"
#include "object.h"

bool compile(const char *source, chunk_t *chunk);

#endif // CLOX_COMPILER_H

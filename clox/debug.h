#ifndef CLOX_DEBUG_H
#define CLOX_DEBUG_H

#include "chunk.h"

void disassemble_chunk(chunk_t *ch, const char *name);
int disassemble_inst(chunk_t *ch, int offset);

#endif //CLOX_DEBUG_H
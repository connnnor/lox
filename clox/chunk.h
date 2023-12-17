#ifndef CLOX_CHUNK_H
#define CLOX_CHUNK_H

#include "common.h"

typedef enum {
  OP_RETURN,
} op_code_t;

typedef struct {
  int count;
  int capacity;
  uint8_t *code;
} chunk_t;

void init_chunk(chunk_t *ch);
void free_chunk(chunk_t *ch);
void write_chunk(chunk_t *ch, uint8_t byte);

#endif // CLOX_CHUNK_H
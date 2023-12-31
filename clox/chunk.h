#ifndef CLOX_CHUNK_H
#define CLOX_CHUNK_H

#include "common.h"
#include "value.h"

typedef enum {
  OP_CONSTANT,
  OP_NIL,
  OP_TRUE,
  OP_FALSE,
  OP_POP,
  OP_GET_LOCAL,
  OP_SET_LOCAL,
  OP_GET_GLOBAL,
  OP_DEFINE_GLOBAL,
  OP_SET_GLOBAL,
  OP_EQUAL,
  OP_GREATER,
  OP_LESS,
  OP_ADD,
  OP_SUBTRACT,
  OP_MULTIPLY,
  OP_DIVIDE,
  OP_NOT,
  OP_NEGATE,
  OP_PRINT,
  OP_JUMP,
  OP_JUMP_IF_FALSE,
  OP_LOOP,
  OP_RETURN,
} op_code_t;

typedef struct {
  int count;
  int capacity;
  uint8_t *code;
  value_arr_t constants;
  int *lines;
} chunk_t;

void init_chunk(chunk_t *ch);
void free_chunk(chunk_t *ch);
void write_chunk(chunk_t *ch, uint8_t byte, int line);
int add_constant(chunk_t *ch, value_t value);

#endif // CLOX_CHUNK_H
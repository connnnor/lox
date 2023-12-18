#ifndef CLOX_VM_H
#define CLOX_VM_H

#include "chunk.h"

#define STACK_MAX 256

typedef struct {
  chunk_t *chunk;
  uint8_t *ip;
  value_t stack[STACK_MAX];
  value_t *stack_top;
} vm_t;

typedef enum {
  INTERPRET_OK,
  INTERPRET_COMPILE_ERROR,
  INTERPRET_RUNTIME_ERROR
} InterpretResult;

void init_vm();
void free_vm();
InterpretResult interpret(const char *source);
void push(value_t v);
value_t pop();

#endif // CLOX_VM_H

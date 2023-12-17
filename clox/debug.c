#include <stdio.h>

#include "debug.h"
#include "chunk.h"

void disassemble_chunk(chunk_t *ch, const char *name) {
  printf("== %s ==\n", name);

  for (int offset = 0; offset < ch->count;) {
    offset = disassemble_inst(ch, offset);
  }
}

static int simple_inst(const char *name, int offset) {
  printf("%s\n", name);
  return offset + 1;
}

int disassemble_inst(chunk_t *ch, int offset) {
  printf("%04d ", offset);
  uint8_t inst = ch->code[offset];
  switch (inst) {
  case OP_RETURN:
    return simple_inst("OP_RETURN", offset);
  default:
    printf("Unknown opcode %d\n", inst);
    return offset + 1;
  }
}
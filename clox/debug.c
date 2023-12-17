#include <stdio.h>

#include "debug.h"
#include "chunk.h"
#include "value.h"

void disassemble_chunk(chunk_t *ch, const char *name) {
  printf("== %s ==\n", name);

  for (int offset = 0; offset < ch->count;) {
    offset = disassemble_inst(ch, offset);
  }
}

static int constant_inst(const char *name, chunk_t  *ch, int offset) {
  uint8_t constant = ch->code[offset + 1];
  printf("%-16s %4d '", name, constant);
  print_value(ch->constants.values[constant]);
  printf("\n");
  return offset + 2;
}


static int simple_inst(const char *name, int offset) {
  printf("%s\n", name);
  return offset + 1;
}

int disassemble_inst(chunk_t *ch, int offset) {
  printf("%04d ", offset);
  if (offset > 0 && ch->lines[offset] == ch->lines[offset - 1]) {
    printf("   | ");
  } else {
    printf("%4d ", ch->lines[offset]);
  }
  uint8_t inst = ch->code[offset];
  switch (inst) {

  case OP_CONSTANT:
    return constant_inst("OP_CONSTANT", ch, offset);
  case OP_ADD:
    return simple_inst("OP_ADD", offset);
  case OP_SUBTRACT:
    return simple_inst("OP_SUBTRACT", offset);
  case OP_MULTIPLY:
    return simple_inst("OP_MULTIPLY", offset);
  case OP_DIVIDE:
    return simple_inst("OP_DIVIDE", offset);
  case OP_NEGATE:
    return simple_inst("OP_NEGATE", offset);
  case OP_RETURN:
    return simple_inst("OP_RETURN", offset);
  default:
    printf("Unknown opcode %d\n", inst);
    return offset + 1;
  }
}

#include <stdio.h>

#include "debug.h"
#include "object.h"
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

static int byte_inst(const char *name, chunk_t * ch, int offset) {
  uint8_t slot = ch->code[offset + 1];
  printf("%-16s %4d\n", name, slot);
  return offset + 2;
}

static int jump_inst(const char *name, int sign, chunk_t *ch, int offset) {
  uint16_t jump = (uint16_t) (ch->code[offset + 1] << 8);
  jump |= ch->code[offset + 2];
  printf("%-16s %4d -> %d\n", name, offset,
         offset + 3 + sign * jump);
  return offset + 3;
}

// Format is like:
// O - Offset
// S - Source line number
// N - Inst Name
// B - Instr Byte Operands
// OFFS    S NAME                BYTE(S)
// 0000    1 OP_CONSTANT         1 '0
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
  case OP_NIL:
    return simple_inst("OP_NIL", offset);
  case OP_TRUE:
    return simple_inst("OP_TRUE", offset);
  case OP_FALSE:
    return simple_inst("OP_FALSE", offset);
  case OP_POP:
    return simple_inst("OP_POP", offset);
  case OP_GET_LOCAL:
    return byte_inst("OP_GET_LOCAL", ch, offset);
  case OP_SET_LOCAL:
    return byte_inst("OP_SET_LOCAL", ch, offset);
  case OP_GET_GLOBAL:
    return constant_inst("OP_GET_GLOBAL", ch, offset);
  case OP_DEFINE_GLOBAL:
    return constant_inst("OP_DEFINE_GLOBAL", ch, offset);
  case OP_SET_GLOBAL:
    return constant_inst("OP_SET_GLOBAL", ch, offset);
  case OP_GET_UPVALUE:
    return byte_inst("OP_GET_UPVALUE", ch, offset);
  case OP_SET_UPVALUE:
    return byte_inst("OP_SET_UPVALUE", ch, offset);
  case OP_EQUAL:
    return simple_inst("OP_EQUAL", offset);
  case OP_GET_PROPERTY:
    return constant_inst("OP_GET_PROPERTY", ch, offset);
  case OP_SET_PROPERTY:
    return constant_inst("OP_SET_PROPERTY", ch, offset);
  case OP_GREATER:
    return simple_inst("OP_GREATER", offset);
  case OP_LESS:
    return simple_inst("OP_LESS", offset);
  case OP_ADD:
    return simple_inst("OP_ADD", offset);
  case OP_SUBTRACT:
    return simple_inst("OP_SUBTRACT", offset);
  case OP_MULTIPLY:
    return simple_inst("OP_MULTIPLY", offset);
  case OP_DIVIDE:
    return simple_inst("OP_DIVIDE", offset);
  case OP_NOT:
    return simple_inst("OP_NOT", offset);
  case OP_NEGATE:
    return simple_inst("OP_NEGATE", offset);
  case OP_PRINT:
    return simple_inst("OP_PRINT", offset);
  case OP_JUMP:
    return jump_inst("OP_JUMP", 1, ch, offset);
  case OP_JUMP_IF_FALSE:
    return jump_inst("OP_JUMP_IF_FALSE", 1, ch, offset);
  case OP_LOOP:
    return jump_inst("OP_JUMP", -1, ch, offset);
  case OP_CALL:
    return byte_inst("OP_CALL", ch, offset);
  case OP_CLOSE_UPVALUE:
    return simple_inst("OP_CLOSE_UPVALUE", offset);
  case OP_RETURN:
    return simple_inst("OP_RETURN", offset);
  case OP_CLASS:
    return constant_inst("OP_CLASS", ch, offset);
  case OP_CLOSURE: {
    offset++;
    uint8_t constant = ch->code[offset++];
    printf("%-16s %4d ", "OP_CLOSURE", constant);
    print_value(ch->constants.values[constant]);
    printf("\n");
    obj_function_t *function = AS_FUNCTION(ch->constants.values[constant]);
    for (int j = 0; j < function->upvalue_count; j++) {
      int is_local = ch->code[offset++];
      int index = ch->code[offset++];
      printf("%04d      |                     %s %d\n",
             offset - 2, is_local ? "local" : "upvalue", index);
    }
    return offset;
  }
  default:
    printf("Unknown opcode %d\n", inst);
    return offset + 1;
  }
}

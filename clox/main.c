#include "common.h"
#include "chunk.h"
#include "debug.h"
#include "vm.h"

int main() {
  init_vm();

  chunk_t chunk;
  init_chunk(&chunk);

  // manually compiling -((1.2 + 3.4) / 5.6) into bytecode
  int constant = add_constant(&chunk, 1.2);
  write_chunk(&chunk, OP_CONSTANT, 123);
  write_chunk(&chunk, constant, 123);

  constant = add_constant(&chunk, 3.4);
  write_chunk(&chunk, OP_CONSTANT, 123);
  write_chunk(&chunk, constant, 123);

  write_chunk(&chunk, OP_ADD, 123);

  constant = add_constant(&chunk, 5.6);
  write_chunk(&chunk, OP_CONSTANT, 123);
  write_chunk(&chunk, constant, 123);

  write_chunk(&chunk, OP_DIVIDE, 123);
  write_chunk(&chunk, OP_NEGATE, 123);

  write_chunk(&chunk, OP_RETURN, 123);

  disassemble_chunk(&chunk, "test chunk");
  interpret(&chunk);
  free_vm();
  free_chunk(&chunk);

  return 0;
}

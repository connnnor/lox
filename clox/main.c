#include "common.h"
#include "chunk.h"
#include "debug.h"

int main() {
  chunk_t chunk;
  init_chunk(&chunk);
  int constant = add_constant(&chunk, 1.23);
  write_chunk(&chunk, OP_CONSTANT, 123);
  write_chunk(&chunk, constant, 123);
  write_chunk(&chunk, OP_RETURN, 123);

  disassemble_chunk(&chunk, "test chunk");
  free_chunk(&chunk);

  return 0;
}

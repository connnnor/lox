#include "common.h"
#include "chunk.h"
#include "debug.h"

int main() {
  chunk_t chunk;
  init_chunk(&chunk);
  write_chunk(&chunk, OP_RETURN);

  disassemble_chunk(&chunk, "test chunk");
  free_chunk(&chunk);

  return 0;
}

#include "chunk.h"
#include "memory.h"

void init_chunk(chunk_t *ch) {
  ch->count = 0;
  ch->capacity = 0;
  ch->code = NULL;
}

void free_chunk(chunk_t *ch) {
  FREE_ARRAY(uint8_t, ch->code, ch->capacity);
  init_chunk(ch);
}

void write_chunk(chunk_t *ch, uint8_t byte) {
  if (ch->capacity < ch->count + 1) {
    int old_capacity = ch->capacity;
    ch->capacity = GROW_CAPACITY(old_capacity);
    ch->code = GROW_ARRAY(uint8_t, ch->code, old_capacity, ch->capacity);
  }
  ch->code[ch->count] = byte;
  ch->count++;
}

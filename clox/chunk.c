#include "chunk.h"
#include "memory.h"
#include "vm.h"

void init_chunk(chunk_t *ch) {
  ch->count = 0;
  ch->capacity = 0;
  ch->code = NULL;
  ch->lines = NULL;
  init_value_arr(&ch->constants);
}

void free_chunk(chunk_t *ch) {
  FREE_ARRAY(uint8_t, ch->code,  ch->capacity);
  FREE_ARRAY(int,     ch->lines, ch->capacity);
  free_value_arr(&ch->constants);
  init_chunk(ch);
}

void write_chunk(chunk_t *ch, uint8_t byte, int line) {
  if (ch->capacity < ch->count + 1) {
    int old_capacity = ch->capacity;
    ch->capacity = GROW_CAPACITY(old_capacity);
    ch->code  = GROW_ARRAY(uint8_t, ch->code,  old_capacity, ch->capacity);
    ch->lines = GROW_ARRAY(int,     ch->lines, old_capacity, ch->capacity);
  }
  ch->code[ch->count] = byte;
  ch->lines[ch->count] = line;
  ch->count++;
}

int add_constant(chunk_t *ch, value_t value) {
  push(value);
  write_value_arr(&ch->constants, value);
  pop();
  return ch->constants.count - 1;
}

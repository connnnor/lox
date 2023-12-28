#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "value.h"
#include "object.h"

void init_value_arr(value_arr_t *arr) {
  arr->values = NULL;
  arr->capacity = 0;
  arr->count = 0;
}

void write_value_arr(value_arr_t *arr, value_t value) {
  if (arr->capacity < arr->count + 1) {
    int old_capacity = arr->capacity;
    arr->capacity = GROW_CAPACITY(old_capacity);
    arr->values = GROW_ARRAY(value_t, arr->values, old_capacity, arr->capacity);
  }
  arr->values[arr->count] = value;
  arr->count++;
}

void free_value_arr(value_arr_t *arr) {
  FREE_ARRAY(value_t, arr->values, arr->capacity);
  init_value_arr(arr);
}
void print_value(value_t value) {
  switch(value.type) {
  case VAL_BOOL:
    printf(AS_BOOL(value) ? "true" : "false");
    break;
  case VAL_NIL:    printf("nil"); break;
  case VAL_NUMBER: printf("%g", AS_NUMBER(value)); break;
  case VAL_OBJ:    print_object(value); break;
  }
}

bool values_equal(value_t a, value_t b) {
  if (a.type != b.type) return false;
  switch(a.type) {
  case VAL_BOOL: return AS_BOOL(a) == AS_BOOL(b);
  case VAL_NIL: return true;
  case VAL_NUMBER: return AS_NUMBER(a) == AS_NUMBER(b);
  case VAL_OBJ: return AS_OBJ(a) == AS_OBJ(b);
  default: return false; // unreachable
  }
}
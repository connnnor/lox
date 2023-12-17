#include <stdio.h>

#include "memory.h"
#include "value.h"

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
  printf("%g", value);
}
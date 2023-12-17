#ifndef CLOX_VALUE_H
#define CLOX_VALUE_H

typedef double value_t;

typedef struct {
  int capacity;
  int count;
  value_t *values;
} value_arr_t;

void init_value_arr(value_arr_t *arr);
void write_value_arr(value_arr_t *arr, value_t value);
void free_value_arr(value_arr_t *arr);
void print_value(value_t value);

#endif //CLOX_VALUE_H

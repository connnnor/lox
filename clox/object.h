#ifndef CLOX_OBJECT_H
#define CLOX_OBJECT_H

#include "common.h"
#include "chunk.h"
#include "value.h"

#define OBJ_TYPE(value)     (AS_OBJ(value)->type)

#define IS_CLASS(value)     is_obj_type(value, OBJ_CLASS)
#define IS_CLOSURE(value)   is_obj_type(value, OBJ_CLOSURE)
#define IS_FUNCTION(value)  is_obj_type(value, OBJ_FUNCTION)
#define IS_NATIVE(value)    is_obj_type(value, OBJ_NATIVE)
#define IS_STRING(value)    is_obj_type(value, OBJ_STRING)

#define AS_CLASS(value)     ((obj_class_t *) AS_OBJ(value))
#define AS_CLOSURE(value)   ((obj_closure_t *) AS_OBJ(value))
#define AS_FUNCTION(value)  ((obj_function_t*) AS_OBJ(value))
#define AS_NATIVE(value)    ((obj_native_t*) AS_OBJ(value))
#define AS_STRING(value)    ((obj_string_t*) AS_OBJ(value))
#define AS_CSTRING(value)   (((obj_string_t*) AS_OBJ(value))->chars)

typedef enum {
  OBJ_CLASS,
  OBJ_CLOSURE,
  OBJ_FUNCTION,
  OBJ_NATIVE,
  OBJ_STRING,
  OBJ_UPVALUE
} obj_type_t;

struct obj_t {
  obj_type_t type;
  bool is_marked;
  struct obj_t *next;
};

typedef struct {
  obj_t obj;
  int arity;
  int upvalue_count;
  chunk_t chunk;
  obj_string_t *name;
} obj_function_t;

typedef value_t (*native_fn_t)(int arg_count, value_t *args);

typedef struct {
  obj_t obj;
  native_fn_t function;
  int arity;
} obj_native_t;

struct obj_string_t {
  obj_t obj;
  int length;
  char *chars;
  uint32_t hash;
};

typedef struct obj_upvalue_t {
  obj_t obj;
  value_t *location;
  value_t closed;
  struct obj_upvalue_t *next;
} obj_upvalue_t;

typedef struct obj_closure_t {
  obj_t obj;
  obj_function_t *function;
  obj_upvalue_t **upvalues;
  int upvalue_count;
} obj_closure_t;

typedef struct {
  obj_t obj;
  obj_string_t *name;
} obj_class_t;


obj_class_t *new_class(obj_string_t *name);
obj_closure_t *new_closure(obj_function_t *function);
obj_function_t *new_function();
obj_native_t *new_native(native_fn_t *function, int arity);
obj_string_t *take_string(const char *chars, int length);
obj_string_t *copy_string(const char *chars, int length);
obj_upvalue_t *new_upvalue(value_t *slot);
void print_object(value_t value);

static inline bool is_obj_type(value_t value, obj_type_t type) {
  return IS_OBJ(value) && AS_OBJ(value)->type == type;
}

#endif // CLOX_OBJECT_H

#include "memory.h"
#include "vm.h"

void *reallocate(void *ptr, size_t old_size, size_t new_size) {
  if (new_size == 0) {
    free(ptr);
    return NULL;
  }

  void *out = realloc(ptr, new_size);
  if (out == NULL) {
    exit(EXIT_MALLOC_ERROR);
  }
  return out;
}

static void free_object(obj_t *object) {
  switch (object->type) {
  case OBJ_STRING: {
    obj_string_t *string = (obj_string_t *)object;
    FREE_ARRAY(char, string->chars, string->length);
    FREE(obj_string_t, object);
    break;
  }
  }
}

void free_objects() {
  obj_t *object = vm.objects;
  while (object != NULL) {
    obj_t *next = object->next;
    free_object(object);
    object = next;
  }
}

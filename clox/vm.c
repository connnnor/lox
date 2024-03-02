#include <math.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

#include "common.h"
#include "compiler.h"
#include "debug.h"
#include "object.h"
#include "memory.h"
#include "vm.h"

vm_t vm;

static void runtime_error(const char * format, ...);

static value_t clock_native(int arg_count, value_t *args) {
  return NUMBER_VAL((double) clock() / CLOCKS_PER_SEC);
}

static value_t floor_native(int arg_count, value_t *args) {
  return NUMBER_VAL((double) floor((double) AS_NUMBER(args[0])));
}

static value_t rand_native(int arg_count, value_t *args) {
  return NUMBER_VAL((double) rand() / (double) (RAND_MAX));
}

static void reset_stack() {
  vm.stack_top = vm.stack;
  vm.frame_count = 0;
  vm.open_upvalues = NULL;
}

static void runtime_error(const char * format, ...) {
  va_list args;
  va_start(args, format);
  vfprintf(stderr, format, args);
  va_end(args);
  fputs("\n", stderr);

  for (int i = vm.frame_count - 1; i >= 0; i--) {
    call_frame_t *frame = &vm.frames[i];
    obj_function_t *function = frame->closure->function;
    size_t instruction = frame->ip - frame->closure->function->chunk.code - 1;
    fprintf(stderr, "[line %d] in ", function->chunk.lines[instruction]);
    if (function->name == NULL) {
      fprintf(stderr, "script\n");
    } else {
      fprintf(stderr, "%s()\n", function->name->chars);
    }
  }

  reset_stack();
}

static void define_native(const char *name, native_fn_t function, int arity) {
  push(OBJ_VAL(copy_string(name, (int)strlen(name))));
  push(OBJ_VAL(new_native(function, arity)));
  table_set(&vm.globals, AS_STRING(vm.stack[0]), vm.stack[1]);
  pop();
  pop();
}

static bool call_native(obj_native_t *native, int arg_count) {
  if (arg_count != native->arity) {
    runtime_error("Expected %d arguments but got %d.",
                  native->arity, arg_count);
    return false;
  }

  value_t result = native->function(arg_count, vm.stack_top - arg_count);
  vm.stack_top -= arg_count + 1;
  push(result);
  return true;
}

void init_vm() {
  reset_stack();
  vm.objects = NULL;
  vm.bytes_allocated = 0;
  vm.next_gc = 1024 * 1024;

  vm.gray_count = 0;
  vm.gray_capacity = 0;
  vm.gray_stack = NULL;

  init_table(&vm.globals);
  init_table(&vm.strings);

  vm.init_string = NULL;
  vm.init_string = copy_string("init", 4);

  define_native("clock", clock_native, 0);
  define_native("floor", floor_native, 1);
  define_native("random", rand_native, 0);
}

void free_vm() {
  free_table(&vm.globals);
  free_table(&vm.strings);
  vm.init_string = NULL;
  free_objects();
}

void push(value_t value) {
  *vm.stack_top = value;
  vm.stack_top++;
}

value_t pop() {
  vm.stack_top--;
  return *vm.stack_top;
}

static value_t peek(int distance) {
  return vm.stack_top[-1 - distance];
}

static bool call(obj_closure_t *closure, int arg_count) {
  if (arg_count != closure->function->arity) {
    runtime_error("Expected %d arguments but got %d.",
                  closure->function->arity, arg_count);
    return false;
  }

  if (vm.frame_count == FRAMES_MAX) {
    runtime_error("Stack overflow.");
    return false;
  }

  call_frame_t *frame = &vm.frames[vm.frame_count++];
  frame->closure = closure;
  frame->ip = closure->function->chunk.code;
  frame->slots = vm.stack_top - arg_count - 1;
  return true;
}

static bool call_value(value_t callee, int arg_count) {
  if (IS_OBJ(callee)) {
    switch (OBJ_TYPE(callee)) {
    case OBJ_BOUND_METHOD: {
      obj_bound_method_t *bound = AS_BOUND_METHOD(callee);
      vm.stack_top[-arg_count - 1] = bound->receiver;
      return call(bound->method, arg_count);
    }
    case OBJ_CLOSURE:
      return call(AS_CLOSURE(callee), arg_count);
    case OBJ_CLASS: {
      obj_class_t *klass = AS_CLASS(callee);
      vm.stack_top[-arg_count - 1] = OBJ_VAL(new_instance(klass));
      value_t initializer;
      if (table_get(&klass->methods, vm.init_string, &initializer)) {
        return call(AS_CLOSURE(initializer), arg_count);
      } else if (arg_count != 0) {
        runtime_error("Expected 0 arguments but got %d.", arg_count);
        return false;
      }
      return true;
    }
    case OBJ_NATIVE: {
      return call_native(AS_NATIVE(callee), arg_count);
    }
    default:
      break; // non-callable object type.
    }
  }
  runtime_error("Can only call functions and classes.");
  return false;
}

static bool bind_method(obj_class_t *klass, obj_string_t *name) {
  value_t method;
  if (!table_get(&klass->methods, name, &method)) {
    runtime_error("Undefined property '%s'.", name->chars);
    return false;
  }

  obj_bound_method_t *bound = new_bound_method(peek(0), AS_CLOSURE(method));

  pop();
  push(OBJ_VAL(bound));
  return true;
}

// 3 reasons we exit the loop here:
// 1. the local slot we stopped at is the slot we're looking for
// 2. we ran out of upvalues to search
// 3. we found an upvalue whose local slot is below the one we're looking for
static obj_upvalue_t *capture_upvalue(value_t *local) {
  obj_upvalue_t *prev_upvalue = NULL;
  obj_upvalue_t *upvalue = vm.open_upvalues;
  while (upvalue != NULL && upvalue->location > local) {
    prev_upvalue = upvalue;
    upvalue = upvalue->next;
  }

  if (upvalue != NULL && upvalue->location == local) {
    return upvalue;
  }

  obj_upvalue_t *created_upvalue = new_upvalue(local);
  created_upvalue->next = upvalue;

  if (prev_upvalue == NULL) {
    vm.open_upvalues = created_upvalue;
  } else {
    prev_upvalue->next = created_upvalue;
  }

  return created_upvalue;
}

// accepts a pointer to a stack slot
// closes every open upvalue it can find that points to that slot
// or any slot above it on the stack
static void close_upvalues(value_t *last) {
  while(vm.open_upvalues != NULL && vm.open_upvalues->location >= last) {
    obj_upvalue_t *upvalue = vm.open_upvalues;
    upvalue->closed = *upvalue->location;
    upvalue->location = &upvalue->closed;
    vm.open_upvalues = upvalue->next;
  }
}

static void define_method(obj_string_t *name) {
  value_t method = peek(0);
  obj_class_t *klass = AS_CLASS(peek(1));
  table_set(&klass->methods, name, method);
  pop();
}

static bool is_falsey(value_t value) {
  return IS_NIL(value) || (IS_BOOL(value) && !AS_BOOL(value));
}

static void concatenate() {
  obj_string_t *b = AS_STRING(peek(0));
  obj_string_t *a = AS_STRING(peek(1));
  int length = a->length + b->length;
  char *chars = ALLOCATE(char, length + 1);
  memcpy(chars, a->chars, a->length);
  memcpy(chars + a->length, b->chars, b->length);
  chars[length] = '\0';

  pop();
  pop();
  obj_string_t *result = take_string(chars, length);
  push(OBJ_VAL(result));
}

static interpret_result_t run() {
  call_frame_t *frame = &vm.frames[vm.frame_count - 1];
#define READ_BYTE() (*frame->ip++)

#define READ_SHORT() \
   (frame->ip += 2, \
   (uint16_t) ((frame->ip[-2] << 8) | frame->ip[-1]))

#define READ_CONSTANT() \
  (frame->closure->function->chunk.constants.values[READ_BYTE()])

#define READ_STRING() AS_STRING(READ_CONSTANT())
#define BINARY_OP(value_type, op)                                              \
  do {                                                                         \
    if (!IS_NUMBER(peek(0)) || !IS_NUMBER(peek(1))) {                          \
      runtime_error("Operands must be numbers.");                              \
      return INTERPRET_RUNTIME_ERROR;                                          \
    }                                                                          \
    double b = AS_NUMBER(pop());                                               \
    double a = AS_NUMBER(pop());                                               \
    push(value_type(a op b));                                                  \
  } while (false)

  for (;;) {
#ifdef DEBUG_TRACE_EXECUTION
    printf("          ");
    for (value_t *slot = vm.stack; slot < vm.stack_top; slot++) {
      printf("[ ");
      print_value(*slot);
      printf(" ]");
    }
    printf("\n");
    disassemble_inst(&frame->closure->function->chunk,
                     (int) (frame->ip - frame->closure->function->chunk.code));
#endif
    uint8_t inst;
    switch (inst = READ_BYTE()) {
    case OP_CONSTANT: {
      value_t constant = READ_CONSTANT();
      push(constant);
      break;
    }
    case OP_NIL:   push(NIL_VAL); break;
    case OP_TRUE:  push(BOOL_VAL(true)); break;
    case OP_FALSE: push(BOOL_VAL(false)); break;
    case OP_POP:   pop(); break;
    case OP_GET_LOCAL: {
      uint8_t slot = READ_BYTE();
      push(frame->slots[slot]);
      break;
    }
    case OP_SET_LOCAL: {
      uint8_t slot = READ_BYTE();
      frame->slots[slot] = peek(0);
      break;
    }
    case OP_GET_GLOBAL: {
      obj_string_t *name = READ_STRING();
      value_t value;
      if (!table_get(&vm.globals, name, &value)) {
        runtime_error("Undefined variable '%s'.", name->chars);
        return INTERPRET_RUNTIME_ERROR;
      }
      push(value);
      break;
    }
    case OP_DEFINE_GLOBAL: {
      obj_string_t *name = READ_STRING();
      table_set(&vm.globals, name, peek(0));
      pop();
      break;
    }
    case OP_SET_GLOBAL: {
      obj_string_t *name = READ_STRING();
      if (table_set(&vm.globals, name, peek(0))) {
        table_delete(&vm.globals, name);
        runtime_error("Undefined variable '%s'.", name->chars);
        return INTERPRET_RUNTIME_ERROR;
      }
      break;
    }
    case OP_GET_PROPERTY: {
      if (!IS_INSTANCE(peek(0))) {
        runtime_error("Only instances have properties.");
        return INTERPRET_RUNTIME_ERROR;
      }

      obj_instance_t *instance = AS_INSTANCE(peek(0));
      obj_string_t *name = READ_STRING();

      value_t value;
      if (table_get(&instance->fields, name, &value)) {
        pop(); // instance
        push(value);
        break;
      }

      if (!bind_method(instance->klass, name)) {
        return INTERPRET_RUNTIME_ERROR;
      }
      break;
    }
    case OP_SET_PROPERTY: {
      if (!IS_INSTANCE(peek(1))) {
        runtime_error("Only instances have fields.");
        return INTERPRET_RUNTIME_ERROR;
      }

      obj_instance_t *instance = AS_INSTANCE(peek(1));
      table_set(&instance->fields, READ_STRING(), peek(0));
      value_t value = pop();
      pop();
      push(value);
      break;
    }
    case OP_EQUAL: {
      value_t a = pop();
      value_t b = pop();
      push(BOOL_VAL(values_equal(a, b)));
      break;
    }
    case OP_GET_UPVALUE: {
      uint8_t slot = READ_BYTE();
      push(*frame->closure->upvalues[slot]->location);
      break;
    }
    case OP_SET_UPVALUE: {
      uint8_t slot = READ_BYTE();
      *frame->closure->upvalues[slot]->location = peek(0);
      break;
    }
    case OP_GREATER:  BINARY_OP(BOOL_VAL, >); break;
    case OP_LESS:     BINARY_OP(BOOL_VAL, <); break;
    case OP_ADD: {
      if (IS_STRING(peek(0)) && IS_STRING(peek(1))) {
        concatenate();
      } else if (IS_NUMBER(peek(0)) && IS_NUMBER(peek(1))) {
        BINARY_OP(NUMBER_VAL, +);
      } else {
        runtime_error("Operands must be two numbers or two strings.");
        return INTERPRET_RUNTIME_ERROR;
      }
      break;
    }
    case OP_SUBTRACT: BINARY_OP(NUMBER_VAL, -); break;
    case OP_MULTIPLY: BINARY_OP(NUMBER_VAL, *); break;
    case OP_DIVIDE:   BINARY_OP(NUMBER_VAL, /); break;
    case OP_NOT:
      push(BOOL_VAL(is_falsey(pop())));
      break;
    case OP_NEGATE:
      if (!IS_NUMBER(peek(0))) {
        runtime_error("Operand must be a number.");
        return INTERPRET_RUNTIME_ERROR;
      }
      push(NUMBER_VAL(-AS_NUMBER(pop())));
      break;
    case OP_PRINT:
      print_value(pop());
      printf("\n");
      break;
    case OP_JUMP: {
      uint16_t offset = READ_SHORT();
      frame->ip += offset;
      break;
    }
    case OP_JUMP_IF_FALSE: {
      uint16_t offset = READ_SHORT();
      if (is_falsey(peek(0))) frame->ip += offset;
      break;
    }
    case OP_CALL: {
      int arg_count = READ_BYTE();
      if (!call_value(peek(arg_count), arg_count)) {
        return INTERPRET_RUNTIME_ERROR;
      }
      frame = &vm.frames[vm.frame_count - 1];
      break;
    }
    case OP_LOOP: {
      uint16_t offset = READ_SHORT();
      frame->ip -= offset;
      break;
    }
    case OP_CLOSURE: {
      obj_function_t *function = AS_FUNCTION(READ_CONSTANT());
      obj_closure_t *closure = new_closure(function);
      push(OBJ_VAL(closure));
      for (int i = 0; i < closure->upvalue_count; i++) {
        uint8_t is_local = READ_BYTE();
        uint8_t index = READ_BYTE();
        if (is_local) {
          closure->upvalues[i] = capture_upvalue(frame->slots + index);
        } else {
          closure->upvalues[i] = frame->closure->upvalues[index];
        }
      }
      break;
    }
    case OP_RETURN: {
      value_t result = pop();
      close_upvalues(frame->slots);
      vm.frame_count--;
      if (vm.frame_count == 0) {
        pop();
        return INTERPRET_OK;
      }

      vm.stack_top = frame->slots;
      push(result);
      frame = &vm.frames[vm.frame_count - 1];
      break;
    }
    case OP_CLASS:
      push(OBJ_VAL(new_class(READ_STRING())));
      break;
    case OP_METHOD:
      define_method(READ_STRING());
      break;
    }
  }
#undef READ_BYTE
#undef READ_CONSTANT
#undef READ_SHORT
#undef READ_STRING
#undef BINARY_OP
}

interpret_result_t interpret(const char *source) {
  obj_function_t *function = compile(source);
  if (function == NULL) return INTERPRET_COMPILE_ERROR;

  push(OBJ_VAL(function));
  obj_closure_t *closure = new_closure(function);
  pop();
  push(OBJ_VAL(closure));
  call(closure, 0);

  return run();
}

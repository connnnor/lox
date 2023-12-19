#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "common.h"
#include "chunk.h"
#include "debug.h"
#include "vm.h"

static void repl() {
  char line[1024];
  for (;;) {
    printf("> ");

    if (!fgets(line, sizeof(line), stdin)) {
      printf("\n");
      break;
    }
    interpret(line);
  }
}

static char *read_file(const char *path) {
  FILE *f = fopen(path, "rb");
  if (f == NULL) {
    fprintf(stderr, "Could not open file \"%s\".\n", path);
    exit(EXIT_FILE_ERROR);
  }

  fseek(f, 0L, SEEK_END);
  size_t size = ftell(f);
  rewind(f);

  char *buf = malloc(size + 1);
  if (buf == NULL) {
    fprintf(stderr, "Not enough memory to read \"%s\".\n", path);
    exit(EXIT_FILE_ERROR);
  }
  size_t bytes_read = fread(buf, sizeof(char), size, f);
  if (bytes_read < size) {
    fprintf(stderr, "Could not read file \"%s\".\n", path);
    exit(EXIT_FILE_ERROR);
  }
  buf[bytes_read] = '\0';

  fclose(f);
  return buf;
}

static void run_file(const char *path) {
  char *source = read_file(path);
  interpret_result_t result = interpret(source);
  free(source);

  if (result == INTERPRET_COMPILE_ERROR) exit(EXIT_COMPILE_ERROR);
  if (result == INTERPRET_RUNTIME_ERROR) exit(EXIT_RUNTIME_ERROR);
}

int main(int argc, char **argv) {
  init_vm();

  chunk_t chunk;
  init_chunk(&chunk);

  if (argc == 1) {
    repl();
  } else if (argc == 2) {
    run_file(argv[1]);
  } else {
    fprintf(stderr, "Usage: clox [path]\n");
  }

  free_vm();
  free_chunk(&chunk);

  return EXIT_SUCCESS;
}

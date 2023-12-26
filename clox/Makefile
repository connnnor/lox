TARGET=clox
OBJECTS=main.o compiler.o chunk.o debug.o memory.o scanner.o table.o object.o value.o vm.o
#CFLAGS = -g -Wall -O3
CFLAGS = -gdwarf-4 -Wall -O0
CFLAGS = -gdwarf-4 -Wall -O0 -fdump-rtl-expand
LDLIBS=
#CC=clang
CC=gcc

$(TARGET): $(OBJECTS)

.PHONY : clean
clean:
	rm $(OBJECTS) $(P)

.PHONY : all
all : $(TARGET)

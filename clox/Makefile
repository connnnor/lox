TARGET=clox
OBJECTS=main.o compiler.o chunk.o debug.o memory.o scanner.o value.o vm.o
#CFLAGS = -g -Wall -O3
CFLAGS = -gdwarf-4 -Wall -O0
LDLIBS=
CC=clang

$(TARGET): $(OBJECTS)

.PHONY : clean
clean:
	rm $(OBJECTS) $(P)

.PHONY : all
all : $(TARGET)
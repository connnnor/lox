TARGET=clox
OBJECTS=main.o chunk.o memory.o debug.o value.o
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
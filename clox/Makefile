# ==========================================
#   Unity Project - A Test Framework for C
#   Copyright (c) 2007 Mike Karlesky, Mark VanderVoord, Greg Williams
#   [Released under MIT License. Please refer to license.txt for details]
# ==========================================

#We try to detect the OS we are running on, and adjust commands as needed
ifeq ($(OS),Windows_NT)
  ifeq ($(shell uname -s),) # not in a bash-like shell
	CLEANUP = del /F /Q
	MKDIR = mkdir
  else # in a bash-like shell, like msys
	CLEANUP = rm -f
	MKDIR = mkdir -p
  endif
	TARGET_EXTENSION=.exe
else
	CLEANUP = rm -f
	MKDIR = mkdir -p
	TARGET_EXTENSION=.out
endif

C_COMPILER=gcc
ifeq ($(shell uname -s), Darwin)
C_COMPILER=clang
endif

CFLAGS = -gdwarf-4 -Wall -O0

UNITY_ROOT = unity

TARGET_BASE1=clox
TARGET_BASE2=tests
TARGET1 = $(TARGET_BASE1)$(TARGET_EXTENSION)
TARGET2 = $(TARGET_BASE2)$(TARGET_EXTENSION)
SRC_FILES1=chunk.c  clox.c  compiler.c  debug.c  main.c  memory.c  object.c  scanner.c  table.c  value.c  vm.c
SRC_FILES2=chunk.c  clox.c  compiler.c  debug.c  memory.c  object.c  scanner.c  table.c  value.c  vm.c $(UNITY_ROOT)/src/unity.c test/TestsHash.c
INC_DIRS=-Isrc -I$(UNITY_ROOT)/src

all: clean default

default: $(SRC_FILES)
	$(C_COMPILER) $(CFLAGS) $(INC_DIRS) $(SYMBOLS) $(SRC_FILES1) -o $(TARGET1)
	$(C_COMPILER) $(CFLAGS) $(INC_DIRS) $(SYMBOLS) $(SRC_FILES2) -o $(TARGET2)
####- ./$(TARGET1)
####- ./$(TARGET2)


clean:
	$(CLEANUP) $(TARGET1) $(TARGET2)
#
#####
#
#TARGET=clox
#OBJECTS=main.o compiler.o chunk.o debug.o memory.o scanner.o table.o object.o value.o vm.o
##CFLAGS = -g -Wall -O3
#CFLAGS = -gdwarf-4 -Wall -O0
#CFLAGS = -gdwarf-4 -Wall -O0 -fdump-rtl-expand
#LDLIBS=
##CC=clang
#CC=gcc
#
#
#
#$(TARGET): $(OBJECTS)
#
#.PHONY : clean
#clean:
#	rm $(OBJECTS) $(P)
#
#.PHONY : all
#all : $(TARGET)

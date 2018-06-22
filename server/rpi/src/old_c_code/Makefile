# Simple Generic Makefile for C/C++ Programs
#
# Sean Tracy 2014/09/15
#
# MODE - Either C or C++
# SOURCES - List of .c .cpp files to compile (By default matches all .c or .cpp files)
# EXECUTABLE - final compiled and linked program name
# SONAME - Internal name of a library (if applicable)
# CFLAGS - Self Explanatory
# LDFLAGS - Flags for ld, in gcc format
#

MODE = C
SOURCES =
EXECUTABLE = 
SONAME =
CFLAGS = 
LDFLAGS =

ifeq ($(MODE), C)
	CC = gcc
	SOURCE_EXTENSION = .c
	ifndef SOURCES
		SOURCES = $(wildcard *.c)
	endif
	OBJECTS = $(SOURCES:.c=.o)
	CFLAGS += -Wall -std=c99
else
	CC = g++
	SOURCE_EXTENSION = .cpp
	ifndef SOURCES
		SOURCES = $(wildcard *.cpp)
	endif
	OBJECTS = $(SOURCES:.cpp=.o)
	CFLAGS += -Wall
endif
ifdef SONAME
	CFLAGS += -fPIC
	LDFLAGS += -shared -Wl,-soname,$(SONAME)
endif

DEFAULT_EXEC = $(shell basename `pwd`)

ifndef EXECUTABLE
	EXECUTABLE := $(DEFAULT_EXEC)
	DEFAULT_USED := t
endif

all: CFLAGS += -g -O0
all: $(SOURCES) $(EXECUTABLE)
ifdef DEFAULT_USED
	$(warning Using Current Directory Name as EXECUTABLE)
endif

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@

$(SOURCE_EXTENSION).o: $(SOURCES)
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -rf $(OBJECTS) $(EXECUTABLE)

release: CFLAGS += -Ofast -pipe
release: LDFLAGS += -pipe
release: $(SOURCES) $(EXECUTABLE)
ifdef DEFAULT_USED
	$(warning Using Current Directory Name as EXECUTABLE)
endif
	strip --strip-unneeded $(EXECUTABLE) 

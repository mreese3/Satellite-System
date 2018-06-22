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
EXECUTABLE = rpi-sensors
SONAME =
CFLAGS = -Wall -std=c99
LDFLAGS =



ifeq ($(MODE), C)
	CC = gcc
	SOURCE_EXTENSION = .c
	ifndef SOURCES
		SOURCES = $(wildcard *.c)
	endif
else
	CC = g++
	SOURCE_EXTENSION = .cpp
	ifndef SOURCES
		SOURCES = $(wildcard *.cpp)
	endif
endif
ifdef SONAME
	CFLAGS += -fPIC
	LDFLAGS += -shared -Wl,-soname,$(SONAME)
endif
OBJECTS = $(SOURCES:.c=.o)

all: $(SOURCES) $(EXECUTABLE)
ifndef EXECUTABLE
	$(error Error: Please set the EXECUTABLE name in the Makefile)
endif	

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@

$(SOURCE_EXTENSION).o: $(SOURCES)
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -rf $(OBJECTS) $(EXECUTABLE)


ifeq ($(CC), "")
$(warning CC is not set, setting to gcc by default.)
CC=gcc
endif

ifeq ($(CXX), "")
$(warning CXX is not set, setting to g++ by default.)
CXX=g++
endif

all: server-rpi client-cpp
build-server:
	echo "In server dep"
	mkdir -p server/all/bin
	$(CC) server/all/include/rpacket.h -o server/all/bin/rpacket.o

build-client:
	echo "In client dep"
	mkdir -p client/all/bin
	$(CC) client/all/include/runpacket.h -o client/all/bin/runpacket.o

server-rpi: build-server
	echo "In rpi server"
	mkdir -p server/rpi/bin
	$(CC) server/rpi/src/main.c -o server/rpi/bin/server

client-cpp: build-client
	echo "In cpp client"
	mkdir -p client/cpp/bin
	$(CXX) client/cpp/src/main.cpp -o client/cpp/bin/client
clean:
	rm -vf server/all/bin/*
	rm -vf server/rpi/bin/*
	rm -vf client/all/bin/*
	rm -vf client/cpp/bin/*

	rmdir server/all/bin
	rmdir server/rpi/bin
	rmdir client/all/bin
	rmdir client/cpp/bin

.PHONY: clean

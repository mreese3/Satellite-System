#include "rpacket.h"

// CRC8 functions
#define CRC8POLY 0x07
#define INITVAL 0x00

unsigned char crc8table[256];
static int crc8_table_state = 0;		// 0 - table has not been generated; 1 - table has been created

void init_crc8() {
	unsigned char crc;
	for (int i = 0; i < 256; i++) {
		crc = i;
		for (int j = 0; j < 8; j++)
			crc = (crc << 1) ^ ((crc & 0x80) ? CRC8POLY : 0);
		crc8table[i] = crc;
	}
}

unsigned int crc8(const char *msg) {
	if (!crc8_table_state)
		init_crc8();
	unsigned int len = msglen(msg);
	char crc = INITVAL;
	for (int i = 0; i < len; i++)
		crc = crc8table[(crc & 0xff) ^ msg[i]];
	return (int)(crc ^ 0x00);
}

//Message Length - re-implementation of strlen()

unsigned int msglen(const char *msg) {
	const char *end;

	for (end = msg; *end; ++end);
	return(end - msg);
}

//mcpy - memcpy() re-implementation

void* mcpy(void* dst, void* src, unsigned int size) {
	unsigned int i;
	for (i = 0; i < size; ++i)
		((char*)dst)[i] = ((char*)src)[i];
	return dst;
}

//mst - memset() re-implementation

void* mset(void* m, char f, unsigned int size) {
	unsigned int i;
	for (i = 0; i < size; ++i)
		((char*)m)[i] = f;
	return m;
}

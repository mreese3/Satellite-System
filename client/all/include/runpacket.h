#ifndef RUNPACKET_H
#define RUNPACKET_H

#define RPACKET_PREAMBLE 0xff
#define RPACKET_EOD   	 0x88
#define MAX_RPACKET_SIZE 254

typedef struct rp_flags {
	char rp_flg_cnt : 1;
	char rp_flg_end : 1;
	char rp_flg_arm : 1;
	char rp_flg_urg : 1;
} rp_flags;

typedef struct rp_packed {
	unsigned int size;
	char *pkt;
} rp_packed;

// Build an rpacket
char* rp_unpack(rp_packed);

// CRC Function
unsigned int crc8(const char *msg);

// utility functions from string.h needed for rpacket construction
unsigned int rp_msglen(const char *msg);
void* rp_msgcpy(void* dst, void* src, unsigned int size);
void* rp_memset(void* m, char f, unsigned int size);

#endif /* RUNPACKET_H */

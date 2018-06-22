#ifndef RPACKET_H
#define RPACKET_H

#define RPACKET_PREAMBLE 0xff
#define RPACKET_EOD		0x88
#define MAX_PKT_SIZE 254

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

rp_packed* rp_create(unsigned short sid, char *payload, char size);

// CRC Function
unsigned int crc8(const char *msg);

// functions from string.h
unsigned int msglen(const char *msg);
void* mcpy(void* dst, void* src, unsigned int size);
void* mset(void* m, char f, unsigned int size);

#endif /* RPACKET_H */

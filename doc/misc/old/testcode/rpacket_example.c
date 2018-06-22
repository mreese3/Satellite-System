/* Example Radio Packet creation;
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

// Test Values
const unsigned char RP_PREAMBLE = 0x3f;
const unsigned char RP_EOD = 0x5f;

typedef struct rp_struct {
	unsigned short rp_sid;
	unsigned char rp_len;
	unsigned char rp_crc;
	unsigned char rp_flg_cnt : 1;
	unsigned char rp_flg_end : 1;
	unsigned char rp_flg_arm : 1;
	unsigned char rp_flg_urg : 1;
	unsigned char rp_pld[];
} rp_struct;

typedef struct rp_packed {
	unsigned int size;
	unsigned char *pkt;
} rp_packed;


rp_packed* rp_create(rp_struct* rs) {
	rp_packed* p = (rp_packed*)malloc(sizeof(rp_packed));
	p->pkt = (unsigned char*)malloc(sizeof(rs));

	int written = 0;
	memcpy(p->pkt, &RP_PREAMBLE, 1);
	written++;

	unsigned char flags = 0 ^ rs->rp_flg_cnt ^ (rs->rp_flg_end << 1) ^ (rs->rp_flg_arm << 2) ^ (rs->rp_flg_urg << 3);
	memcpy(p->pkt + written++, &flags, 1);
	
	memcpy(p->pkt + written, &rs->rp_sid, 2);
	written += 2;

	memcpy(p->pkt + written++, &rs->rp_len, 1);

	memcpy(p->pkt + written, rs->rp_pld, rs->rp_len);
	written += rs->rp_len;

	memcpy(p->pkt + written++, &rs->rp_crc, 1);

	memcpy(p->pkt + written++, &RP_EOD, 1);

	p->size = written;

	return p;
}

int main() {
	char str[] = "test";
	rp_struct *a = (rp_struct*)malloc(sizeof(rp_struct) + strlen(str));
	memset(a, 0, sizeof(rp_struct) + strlen(str));

	memcpy(a->rp_pld, str, strlen(str));
	a->rp_len = strlen(str);

	rp_packed *p = rp_create(a);
	write(1, p->pkt, p->size);
	write(1, str, strlen(str) + 1);
	return 0;
}

#include "rpacket.h"

rp_packed buf;
char p[MAX_PKT_SIZE];


rp_packed* rp_create(unsigned short sid, char *payload, char size) {
	buf.pkt = p;
	buf.size = 0;
	mset(buf.pkt, 0, MAX_PKT_SIZE);
	
	unsigned int written = 0;
	buf.pkt[written++] = RPACKET_PREAMBLE;
	buf.pkt[written++] = 0x00;
	buf.pkt[written++] = (sid & 0xff00) >> 2;
	buf.pkt[written++] = sid & 0x00ff;
	buf.pkt[written++] = size;
	mcpy(buf.pkt + written, payload, size);
	written += size;
	char crc = crc8(buf.pkt);
	buf.pkt[written++] = crc;
	buf.pkt[written++] = RPACKET_EOD;

	buf.size = written;

	return &buf;
}

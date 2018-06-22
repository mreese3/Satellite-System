// This is dependant on string, but won't be once the helper functions are reimplimented.

#include <string.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include "rpacket.h"
int main() {
	rp_flags* flagvar 	= malloc(sizeof(struct rp_flags));
	flagvar->rp_flg_cnt = 1;
	unsigned short s = 2;
	char* thing = "rewewroiruewpoewrupouqrowpurqewpiurewqpi";
	unsigned char length = rp_msglen(thing);
	rp_create(s, thing, length, flagvar);
	return 0;
	}

rp_packed* rp_create(unsigned short sid, char *payload, unsigned char payloadsize, rp_flags* flags) {
	struct rp_packed *newpacket 	= malloc(sizeof(struct rp_packed));
	char* mtufiller 			= malloc((MAX_RPACKET_SIZE - (payloadsize + 5)) * sizeof(char));
	char* packetbuffer 			= malloc(MAX_RPACKET_SIZE * sizeof(char));
	// An incredible, editable filler, to space us out to the MTU. 7 is
	// the sum of all the parts of packet, except the payload
	// Dereferencing NULL pointers are like the funnest thing ever.
	assert(packetbuffer != NULL);
	// At this point, we have a buffer, ready for our packet information,
	// and we just need to copy it in. Our memset function will do that.
	// for us.
	// NB: I seriously thought about using a character array directly,
	// and using a for loop to hand edit each character. 
	// It would've worked, but seemed a little too kludgey.
	// I am not a real programmer.

	unsigned char header	 = RPACKET_PREAMBLE;

	// I tried lots of hacks to shove the bitfield into a char. Bitwise is the 
	// best way. We're gonna get screwed on endiness.
	unsigned char compressedflags 	= 0;
	compressedflags = compressedflags | flags->rp_flg_cnt;
	compressedflags = 1<<compressedflags;
	compressedflags = compressedflags | flags->rp_flg_end;
	compressedflags = 1<<compressedflags;
	compressedflags = compressedflags | flags->rp_flg_arm;
	compressedflags = 1<<compressedflags;
	compressedflags = compressedflags | flags->rp_flg_urg;

	unsigned char highsensorid	= sid >> 8;
	unsigned char lowsensorid	= sid & 0x00ff;	
	unsigned char datalength = rp_msglen(payload);
	// The original spec called for CRC against the entire constructed packet.
	// At the moment, this is just against the payload.
	// FIXME
	unsigned char crc	 	= crc8(payload);
	unsigned char eof		= RPACKET_EOD;

	// The magic happens here. Horrible abuse of strcpy.
	rp_memcpy(packetbuffer, header, 2);
	rp_memcpy(packetbuffer+1, compressedflags, 2);
	rp_memcpy(packetbuffer+2, highsensorid, 2);
	rp_memcpy(packetbuffer+3, lowsensorid, 2);
	rp_memcpy(packetbuffer+4, datalength, 2);
	memcpy(packetbuffer+5, payload, datalength);
	memcpy(packetbuffer+datalength, mtufiller, (MAX_RPACKET_SIZE - (payloadsize + 5)));
	rp_memcpy(packetbuffer+253, crc, 2);
	rp_memcpy(packetbuffer+254, eof, 2);
	newpacket->size = MAX_RPACKET_SIZE;
	newpacket->pkt  = packetbuffer;

	printf(packetbuffer);

	free(packetbuffer);
	free(mtufiller);
	return newpacket;
	}


	 	
unsigned char rp_msglen(const char *msg) {
	// Completely dependant on libc, but functional
	// Complete beats correct every time.
	unsigned char length = (unsigned char) strlen(msg);
	return length;
	}

void rp_memset(void* ptr, char replace, unsigned char length) {
	// See the above note about msglen
	int replaceint = (int)replace;
	memset(ptr, replaceint, length);
	}

void rp_memcpy(void* dst, void* src, unsigned int size) {
	//Ditto
	memcpy(dst, src, strlen(src)+1);
	}


// Implimentation pulled as noticed below.
// FIXME
// This will need to get replaced with something else, preferably
// BSD licenced.
// Also, this is quite possibly one of the crappiest implimentations
// I found. It's only here because it had the benefit of brevity.
unsigned char crc8(const char *msg) { 
//unsigned int CRC8(const byte *data, byte len) {
//CRC-8 - based on the CRC8 formulas by Dallas/Maxim
//code released under the therms of the GNU GPL 3.0 license
  unsigned char len = rp_msglen(msg);
  unsigned char crc = 0x00;
  while (len--) {
    unsigned char extract = *msg++;
    for (unsigned char tempI = 8; tempI; tempI--) {
      unsigned char sum = (crc ^ extract) & 0x01;
      crc >>= 1;
      if (sum) {
        crc ^= 0x8C;
      }
      extract >>= 1;
    }
  }
  return crc;
}

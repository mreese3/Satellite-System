#ifndef RPACKET_H
#define RPACKET_H

#define RPACKET_PREAMBLE 	0xff
#define RPACKET_EOD		0x88
#define MAX_RPACKET_SIZE 	254

typedef struct rp_flags {
	// This brings the total length of this struct to 1 byte.
	// If more flags are necessary, this padding can be cut
	// into smaller chunks.
	char rp_flg_reservedpadding : 4;

	// Continuation Flag:
	// Set if this is one part of a multipart message.
	// Which part is defined in the payload.
	char rp_flg_cnt : 1;

	// End Flag:
	// Set if this is the last part of a multipart message.
	// This acts like .r99 files did back when bootlegging via
	// eD2k was actually valid.
	// If the client only receives this, they may be SOL on
	// the actual message. Such is life.
	char rp_flg_end : 1;

	// Armored Message Flag:
	// Message was sent with error correction built in,
	// so hopefully it can be reconstructed, if the ionsphere ate it.
	char rp_flg_arm : 1;

	// Urgent Message Flag:
	// If this is set, Syslog is probably having a kitten.
	// The client should treat this message as pretty important.
	// If the server is going down, them dang kids and their fancy
	// maps won't have much to work with.
	char rp_flg_urg : 1;
} rp_flags;

typedef struct rp_packed {
// Size is ambiguous. There's like 7 different sizes we need
// to worry aboot. I'm assuming that refers to the size of
// completed packet, which should be equal to MAX_RPACKET_SIZE
	unsigned int size;
	char *pkt;
} rp_packed;

// /--##rp_create##--
// rp_create is a portable function, required by all platforms,
// which takes a character string, a sensor id, and a flag struct
// and constructs an ASCII-coded packet, ready for transmission.
// Build an rpacket
rp_packed* rp_create(unsigned short sid, char *payload, 
			unsigned char payloadsize, rp_flags* flags);
// --##rp_create##--/

// CRC Function
unsigned char crc8(const char *msg);

// utility functions from string.h needed for rpacket construction
// Some platforms are poorer than Kenny's family, and twice as jacked up. 
// We'll need to provide replacement functions for them.

// Returns the length of the message
// Used for payload, if that wasn't obvious
unsigned char rp_msglen(const char *msg);

// Memcpy replacement
void rp_memcpy(void* dst, void* src, unsigned int size);
// Unidentified flying copy function. Stub for now.


// Memset replacement function.
// Works like the regular memset, but accepts uint8s directly
void rp_memset(void* ptr, char replace, unsigned char length);


#endif /* RPACKET_H */

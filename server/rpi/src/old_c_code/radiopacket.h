#ifndef RADIOPACKET_H
#define RADIOPACKET_H

#include <stdint.h>
#include <stdbool.h>
#include "utils.h"

// Start and end sequence for the RADIOPackets
#define RP_PREAMBLE 0xDEAD
#define RP_POSTAMBLE 0xBEEF

// The actual structure order of the
// header and footer for a packet
typedef struct
{
	uint8_t crc8;
	uint8_t pad;
	uint16_t postamble;
} rpkt_footer_t;
#define RP_FOOTER_SIZE sizeof(rpkt_footer_t)

typedef struct
{
	uint16_t preamble;
	uint8_t pad;
	uint8_t  flags;

	uint32_t seqno;
	uint32_t prev_seqno;

	uint16_t sid;
	uint16_t len;
} rpkt_header_t;
#define RP_HEADER_SIZE sizeof(rpkt_header_t)

// Error numbers
typedef enum 
{
	RP_NO_ERR,		//no error
	RP_CRC_MISMATCH,	//The Calculated CRC did not match the crc in the packet
	RP_ZERO_LENGTH		//The packet received had no payload
} rpkt_errno_t;

// Argument struct for the rp_* functions
// Read from by rp_pack and written to by rp_unpack
typedef struct
{
	uint32_t seqno;
	uint32_t previous_seqno;
	uint16_t sid;

	uint8_t flags_continue	: 1;
	uint8_t flags_end		: 1;
	uint8_t flags_armored	: 1;
	uint8_t flags_urgent	: 1;

	rpkt_errno_t errno;	// Used by unpack to indicate error conditions; ignored by pack function

	uint8_t crc8;
	uint8_t calc_crc8;
} radiopacket_t;
	
uint32_t rp_pack(str_t* strptr, radiopacket_t* packet, str_t* payload_string);
uint32_t rp_unpack(str_t* strptr, radiopacket_t* packet, str_t* payload_string);

#endif /* RPACKET_H */

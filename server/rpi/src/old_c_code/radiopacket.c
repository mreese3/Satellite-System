#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <string.h>
#include <netinet/in.h>
#include "radiopacket.h"

// RADIOPACKET CRC8 functions
#define RP_CRC8POLY 0x07
#define RP_INITVAL 0x00

unsigned char rp_crc8table[256];
static int rp_crc8_table_state = 0;		// 0 - table has not been generated; 1 - table has been created

void rp_init_crc8() {
	unsigned char crc;
	for (int i = 0; i < 256; i++) {
		crc = i;
		for (int j = 0; j < 8; j++)
			crc = (crc << 1) ^ ((crc & 0x80) ? RP_CRC8POLY : 0);
		rp_crc8table[i] = crc;
	}
}

char rp_calculate_crc8(const char *msg, unsigned int size) {
	if (!rp_crc8_table_state)
		rp_init_crc8();
	char crc = RP_INITVAL;
	for (int i = 0; i < size; i++)
		crc = rp_crc8table[(crc & 0xff) ^ msg[i]];
	return (crc ^ 0x00);
}


uint32_t rp_pack(str_t* strptr, radiopacket_t* packet, str_t* payload_string)
{
	// Final packet length
	strptr->string_length = RP_HEADER_SIZE + payload_string->string_length + RP_FOOTER_SIZE;

	// Some Pointers into the packet string 
	rpkt_header_t* headerptr = (rpkt_header_t*)(strptr->string);

	char* payload_destination = strptr->string + RP_HEADER_SIZE;

	rpkt_footer_t* footerptr = (rpkt_footer_t*)(strptr->string + RP_HEADER_SIZE + payload_string->string_length);

	// Populate the packet header with our values
	headerptr->preamble = htons(RP_PREAMBLE);
	headerptr->pad = 0x00;	// Set to NULL for now

	// Set flags by ORing together the values passed in the radiopacket struct argument
	headerptr->flags = 0x00 | packet->flags_end | packet->flags_continue << 1 | packet->flags_urgent << 2 | packet->flags_armored << 3;

	headerptr->seqno = htonl(packet->seqno);
	headerptr->prev_seqno = htonl(packet->previous_seqno);

	headerptr->sid = htons(packet->sid);
	headerptr->len = htons(payload_string->string_length);
	
	// copy payload into the packet buffer
	memcpy(payload_destination, payload_string->string, payload_string->string_length);


	// Calculate the CRC for up to the end of the payload
	footerptr->crc8 = rp_calculate_crc8(strptr->string, RP_HEADER_SIZE + payload_string->string_length);
	footerptr->pad = 0x00;
	footerptr->postamble = htons(RP_POSTAMBLE);
	return 0;
}

uint32_t rp_unpack(str_t* strptr, radiopacket_t* packet, str_t* payload_string)
{
	rpkt_header_t* headerptr = (rpkt_header_t*)(strptr->string);
	char* payload = strptr->string + RP_HEADER_SIZE;
	rpkt_footer_t* footerptr = (rpkt_footer_t*)(strptr->string + strptr->string_length - RP_FOOTER_SIZE);

	char crc8 = rp_calculate_crc8(strptr->string, strptr->string_length - RP_FOOTER_SIZE);
	packet->calc_crc8 = crc8;
	packet->crc8 = footerptr->crc8;

	packet->errno = RP_NO_ERR;
	if (packet->calc_crc8 != packet->crc8)
		packet->errno = RP_CRC_MISMATCH;

	packet->seqno = ntohl(headerptr->seqno);
	packet->previous_seqno = ntohl(headerptr->prev_seqno);
	packet->sid = ntohs(headerptr->sid);

	payload_string->string_length = ntohs(headerptr->len);
	memcpy(payload_string->string, payload, payload_string->string_length);

	return 0;
}

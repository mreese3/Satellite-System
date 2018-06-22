#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include "radiopacket.h"
#include "ringbuffer.h"

#define msgsize 256
#define ringsize 256
#define payloadmax msgsize - RP_HEADER_SIZE - RP_FOOTER_SIZE

void send(str_t* string)
{
	/*
	printf("----------Packet Begin----------\n\n");
	printf("Size: %d\n", string->string_length);

	for (unsigned int i = 0; i < string->string_length; i++) {
		printf("0x%02x ", (unsigned)string->string[i] & 0xffU);
		if (i % 16 == 15)
			printf("\n");
	}
	printf("\n\n");
	printf("----------Packet Begin----------\n\n");
	*/

	radiopacket_t pktstruct;
	str_t* payload = (str_t*)malloc(sizeof(str_t)+payloadmax);

	rp_unpack(string, &pktstruct, payload);

	printf("----------Packet Begin----------\n\n");
	printf("Seqno: %d\nPrevious_Seqno: %d\nSID: %d\nPayload Length: %d\nErrno: %d\n",
			pktstruct.seqno, pktstruct.previous_seqno, pktstruct.sid, payload->string_length, pktstruct.errno);
	printf("CRC8: %x\nCalculated CRC8: %x\n", pktstruct.crc8, pktstruct.calc_crc8);
	printf("Packet Contents:\n%s\n", payload->string);

	printf("-----------Packet End-----------\n\n");
}

int main()
{
	ringbuffer_t* send_queue = rbuffer_create(ringsize, msgsize);
	str_t* raw_buffer = (str_t*)malloc(sizeof(str_t)+msgsize);

	uint32_t seqno = 1;
	uint32_t pseqno = 0;
	while (true)
	{
		unsigned int bytesread = read(0, raw_buffer->string, payloadmax);
		if (bytesread)
		{
			raw_buffer->string_length = bytesread;

			radiopacket_t pktstruct;
			pktstruct.seqno = seqno;
			pktstruct.previous_seqno = pseqno;
			pktstruct.sid = 1;
			pktstruct.flags_armored = 0;
			pktstruct.flags_urgent = 0;
			
			if (bytesread < payloadmax)
				pktstruct.flags_end = 1;
			else
				pktstruct.flags_end = 0;
			
			pktstruct.flags_continue = 0;


			rp_pack(rbuffer_additem(send_queue, true), &pktstruct, raw_buffer);

			send(rbuffer_removeitem(send_queue));
			seqno++;
			pseqno++;
		}
		else
			break;
	}
}

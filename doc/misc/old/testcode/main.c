#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include "rpacket.h"

int main(int argc, char **argv) {
	char *test = "This is a Test";
	unsigned short sid = 1;
	rp_packed* p = rp_create(sid, test, msglen(test));

	write(1, p->pkt, p->size);
	return 0;
}

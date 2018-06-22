#include <stdio.h>
#include <termios.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include <sys/types.h>
#include <fcntl.h>

#define TDEV "/dev/ttyUSB0"


/*
time 	= 32bit Unix timestamp
msglen	= 8 bit unisigned length
msgtype	= 8 bit magic number for source id

pad[1-3]= 8 bit nulls, to space bytes
*/
typedef struct tmsg_header {
	uint32_t time;
	uint8_t msglen;
	uint8_t msgtype;

	uint8_t pad1;
	uint8_t pad2;
	uint8_t pad3;
} tmsg_header;



void sendmsg(int fd, char *msg);

int main(int argc, char **argv) {
/*






*/
	if (argc < 2) {
		fprintf(stderr, "please pass a message to send\n");
		exit(1);
	}
	fprintf(stdout, "Device is " TDEV ". \n");

	int ofd = open(TDEV, O_RDWR | O_NOCTTY | O_NDELAY);
	FILE *in = NULL;
	in = fopen(argv[1], "r");
	if (in == NULL) {
		fprintf(stderr, "Failed to open " TDEV ". Leaving!\n");
		exit(2);
		}
	char *buf = (char *)malloc(sizeof(char)*128);
	while (!feof(in)) {
			fread(buf, 1, 128, in);
			sendmsg(ofd, buf);
	}
	fclose(in);
	close(ofd);
	exit(0);
}

void sendmsg(int fd, char *msg) {
	int mlen = strlen(msg);
		
	struct termios config;
	if(tcgetattr(fd, &config) < 0) {
		exit(1);
	}

	config.c_iflag &= ~(IGNBRK | BRKINT | ICRNL | INLCR | PARMRK | INPCK | ISTRIP | IXON);
	config.c_oflag = 0;
	config.c_lflag &= ~(ECHO | ECHONL | ICANON | IEXTEN | ISIG);
	config.c_cflag &= ~(CSIZE | PARENB);
	config.c_cflag |= CS8;
	config.c_cc[VMIN] = 1;
	config.c_cc[VTIME] = 0;
	if (cfsetispeed(&config, B9600) < 0 || cfsetospeed(&config, B9600) < 0) {
		fprintf(stderr, "Failed to set baudrate and mode on device.  Leaving!\n");
		exit(2);
	}

	if (tcsetattr(fd, TCSAFLUSH, &config) < 0) {
		fprintf(stderr, "Failed to set parameters on device.  Leaving!\n");
		exit(2);
	}

	tmsg_header sending;
	sending.pad1 = sending.pad2 = sending.pad3 = 0x00;

	sending.time = time(NULL);
	sending.msglen = mlen;
	sending.msgtype = 1;
	write (fd, "--MSGBEGIN--", 12);
	write (fd, &sending, sizeof(sending));
	write (fd, msg, mlen);
	write (fd, "--MSGEND--\n", 11);
}

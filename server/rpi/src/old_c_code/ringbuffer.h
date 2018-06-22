#ifndef RINGBUFFER_H
#define RINGBUFFER_H

#include "utils.h"

// To Do: Add more flags for other situations -Sean 1/24/16
typedef enum rb_errno
{
	E_RB_NOERROR = 0,
	E_RB_OVERFLOW = 1,
	E_RB_UNDERFLOW = 1<<1
} rb_errflag_t;

/*
 * The ring buffer is implemented as a two demensional array of characters, or an array of cstrings.
 * It takes a copy of strings added to it and returns pointer to its locations for reading.  This allows
 * for a central message queue that can be accessed quickly and only requires malloc calls once at startup.
 *
 */

typedef struct ringbuffer_s
{
	str_t** bufferstart_ptr;		// points to the start of the buffer space array
	str_t** read_ptr;			// points to the next item to return with rbuffer_removeitem
	str_t** write_ptr;			// points to the next location to write to with rbuffer_additem
	unsigned int count;			// contains the current unread message count in the buffer spaces
	unsigned int max_entries;	// this is the maximum amount of messages that can be stored in the buffer
	unsigned int max_msgsize;	// this is the maximum size of the character array (string) that can be stored in one message
	unsigned int flags;			// this is used to store flags for error conditions.  Right now, only for E_RB_OVERFLOW.
} ringbuffer_t;

// Create a ring buffer with a ring size of buffersize and fixed message character arrays of size msgsize.
// Returns a pointer to a ringbuffer_t struct on success or NULL on error.
ringbuffer_t* rbuffer_create(unsigned int buffersize, unsigned int msgsize);

// Frees a previously created ring buffer.
void rbuffer_destroy(ringbuffer_t* rbuf);

// NOTE: Update these comments to reflect new function
// Copies a message string pointed to by msg of length msgsize into the ring buffer.
// This can cause the buffer to overflow, so it is advised to check if ringbuffer_t.flags  has been set
// or call rbuffer_checkerror()
str_t* rbuffer_additem(ringbuffer_t* rbuf, _Bool zero);

// NOTE: Update these comments to reflect new function
// Assigns the next message to read to the character pointer pointed to by mptr
// Returns the message size if the successful or 0 if the buffer was empty.
// If the return was 0, then mptr remains unchanged.
str_t* rbuffer_removeitem(ringbuffer_t* rbuf);

// Checks if flags in the ringbuffer_t struct has been set.
// Currently only returns flags, so it is just as easy to inspect rbuf->flags, but this may change
// in the future, so using this function is advised.
unsigned int rbuffer_checkerror(ringbuffer_t* rbuf);

// Resets an error flag if it was set or, if E_RB_NOERROR (0) is passed, clears all the flags (sets flags to 0).
// Like rbuffer_checkerror(), it may be easier to manipulate rbuf->flags directly, but the implementation of the error
// tracking may change, so using this function is also advised.
// NOTE: it is not a very good idea to clear all of the flags.  It is better to check for each flag you care about,
// reacting to it, and then reseting it.  This prevents errors from being missed.
unsigned int rbuffer_reseterror(ringbuffer_t* rbuf, rb_errflag_t flag);

#endif /* RINGBUFFER_H */

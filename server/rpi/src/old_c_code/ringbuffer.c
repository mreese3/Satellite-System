#include <stdlib.h>
#include <string.h>
#include "ringbuffer.h"


ringbuffer_t* rbuffer_create(unsigned int buffersize, unsigned int msgsize)
{
	ringbuffer_t* rbuf = (ringbuffer_t*)calloc(1, sizeof(ringbuffer_t));
	if (rbuf)
	{
		rbuf->bufferstart_ptr = (str_t**)calloc(buffersize, sizeof(str_t*));
		if (!rbuf->bufferstart_ptr)
		{
			// Cleanup and return NULL
			free(rbuf);
			return NULL;
		}

		for (unsigned int i = 0; i < buffersize; i++)
		{
			rbuf->bufferstart_ptr[i] = (str_t*)calloc(msgsize, sizeof(str_t)+msgsize);
			if (!rbuf->bufferstart_ptr[i])
			{
				// Cleanup our allocations up to this point and return NULL
				for (unsigned j = 0; j < i; j++)
					free(rbuf->bufferstart_ptr[j]);
				free(rbuf);
				return NULL;
			}
		}

		rbuf->max_entries = buffersize;
		rbuf->max_msgsize = msgsize;
		rbuf->count = 0;
		rbuf->read_ptr = rbuf->bufferstart_ptr;
		rbuf->write_ptr = rbuf->bufferstart_ptr;
		rbuf->flags = E_RB_NOERROR;
	}

	return rbuf;
}

void rbuffer_destroy(ringbuffer_t* rbuf)
{
	// first free our individual message buffers
	for (unsigned int i = 0; i < rbuf->max_entries; i++)
		free(rbuf->bufferstart_ptr[i]);

	// Next, free our buffer array
	free(rbuf->bufferstart_ptr);

	// Finally, delete the ring buffer struct itself
	free(rbuf);
}

str_t* rbuffer_additem(ringbuffer_t* rbuf, _Bool zero)
{
	str_t* mptr = (*rbuf->write_ptr);
	if (zero) {
		memset(mptr->string, 0x00, rbuf->max_msgsize);
		mptr->string_length = 0;
	}

	rbuf->write_ptr++;
	rbuf->count++;
	if (rbuf->count > rbuf->max_entries)
	{
		rbuf->read_ptr++;
		if (rbuf->read_ptr > rbuf->bufferstart_ptr + rbuf->max_entries)
			rbuf->read_ptr = rbuf->bufferstart_ptr;
		rbuf->count--;
		rbuf->flags |= E_RB_OVERFLOW;
	}
	if (rbuf->write_ptr == rbuf->bufferstart_ptr + rbuf->max_entries)
		rbuf->write_ptr = rbuf->bufferstart_ptr;

	return mptr;
}

str_t* rbuffer_removeitem(ringbuffer_t* rbuf)
{
	if (rbuf->count)
	{
		str_t* mptr = *rbuf->read_ptr;
		rbuf->read_ptr++;
		rbuf->count--;
		if (rbuf->read_ptr == rbuf->bufferstart_ptr + rbuf->max_entries)
			rbuf->read_ptr = rbuf->bufferstart_ptr;

		return mptr;
	}
	rbuf->flags |= E_RB_UNDERFLOW;
	return NULL;
}

unsigned int rbuffer_checkerror(ringbuffer_t* rbuf)
{
	return rbuf->flags;
}

unsigned int rbuffer_reseterror(ringbuffer_t* rbuf, rb_errflag_t flag)
{
	if (flag == E_RB_NOERROR)
		rbuf->flags = 0;
	else if (rbuf->flags & flag)
		rbuf->flags ^= flag;

	return rbuf->flags;
}

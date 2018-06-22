#ifndef UTILS_H
#define UTILS_H

#include <stdint.h>

// Safe string struct (contains the length of the string and a variable array to be allocated for the string)
typedef struct safestring_struct
{
	uint32_t string_length;
	char string[];
} str_t;

#endif /* UTILS_H */

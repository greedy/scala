#ifndef ARRAYS_H
#define ARRAYS_H

#include "object.h"

extern struct klass *arrayOf(struct klass *klass);

extern struct klass bool_array;
extern struct klass byte_array;
extern struct klass short_array;
extern struct klass char_array;
extern struct klass int_array;
extern struct klass long_array;
extern struct klass float_array;
extern struct klass double_array;

#define BOOL    0
#define BYTE    1
#define SHORT   2
#define CHAR    3
#define INT     4
#define LONG    5
#define FLOAT   6
#define DOUBLE  7
#define OBJECT  8

struct array {
  struct java_lang_Object super;
  int32_t length;
};

#define ARRAY_DATA(a,t) ((t*)(((char*)(a))+(a)->super.klass->eltsoffset))

struct array *new_array(uint8_t k, struct klass *et, int32_t ndims, int32_t dim0, ...);

#endif

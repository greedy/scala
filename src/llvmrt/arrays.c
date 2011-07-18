#include "arrays.h"
#include <string.h>
#include <assert.h>
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>

struct klass *arrayof(struct klass *klass)
{
  if (klass->arrayklass == NULL) {
    struct klass *ac = malloc(sizeof(struct klass));
    ac->name.bytes = malloc(klass->name.len+1);
    ac->name.bytes[0] = '[';
    memcpy(&ac->name.bytes[1], klass->name.bytes, klass->name.len);
    ac->instsize = 0;
    ac->super = NULL;
    ac->vtable = NULL;
    ac->numiface = 0;
    ac->arrayklass = NULL;
    ac->elementklass = klass;
    klass->arrayklass = ac;
  }
  return klass->arrayklass;
}

#define PRIM_ARRAY(t) struct klass t ## _array = { { sizeof("]" # t)-1, "]" # t }, 0, NULL, NULL, NULL, NULL, 0 }

PRIM_ARRAY(bool);
PRIM_ARRAY(byte);
PRIM_ARRAY(short);
PRIM_ARRAY(char);
PRIM_ARRAY(int);
PRIM_ARRAY(long);
PRIM_ARRAY(float);
PRIM_ARRAY(double);

#undef PRIM_ARRAY

struct array *
new_array(uint8_t k, struct klass *et, int32_t ndims, int32_t dim0, ...)
{
  va_list dims;
  struct klass *aclass;
  size_t eltsize;
  struct array *a;
  void *data;
  size_t datasize;
  switch (k) {
    case BOOL:
      aclass = &bool_array;
      eltsize = 1;
      break;
    case BYTE:
      aclass = &byte_array;
      eltsize = 1;
      break;
    case SHORT:
      aclass = &short_array;
      eltsize = 2;
      break;
    case CHAR:
      aclass = &char_array;
      eltsize = 4;
      break;
    case INT:
      aclass = &int_array;
      eltsize = 4;
      break;
    case LONG:
      aclass = &long_array;
      eltsize = 8;
      break;
    case FLOAT:
      aclass = &float_array;
      eltsize = 4;
      break;
    case DOUBLE:
      aclass = &double_array;
      eltsize = 8;
      break;
    case OBJECT:
      aclass = arrayof(et);
      eltsize = sizeof(struct reference);
      break;
  }
  datasize = sizeof(struct array) + dim0 * eltsize;
  data = a = calloc(1, datasize);
  a->super.klass = aclass;
  a->length = dim0;
  va_start(dims, dim0);
  for (int32_t n = 0; n < ndims - 1; n++) {
    int32_t dimN = va_arg(dims, int32_t);
    void **td;
    aclass = arrayof(aclass);
    a = calloc(1, sizeof(struct array) + dimN * sizeof(void*));
    a->super.klass = aclass;
    a->length = dimN;
    for (size_t i = 0; i < dimN; i++) {
      void *de;
      de = malloc(datasize);
      memcpy(de, data, datasize);
      ((void**)a->data)[i] = de;
    }
    /* FIXME - extra allocation overhead */
    free(data);
    datasize = sizeof(struct array) + dimN * sizeof(void*);
    data = a;
  }
  va_end(dims);
  return a;
}

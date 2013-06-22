#include "arrays.h"
#include "runtime.h"
#include <string.h>
#include <assert.h>
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>

#include "gc.h"

static void *vtable_array[] = {
  method_java_Dlang_DObject_Mclone_Rjava_Dlang_DObject,
  method_java_Dlang_DObject_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean,
  method_java_Dlang_DObject_Mfinalize_Rscala_DUnit,
  method_java_Dlang_DObject_MhashCode_Rscala_DInt,
  method_java_Dlang_DObject_MtoString_Rjava_Dlang_DString,
};

struct klass *arrayOf(struct klass *klass)
{
  if (klass->arrayklass == NULL) {
    struct klass *ac = malloc(sizeof(struct klass));
    ac->name.len = klass->name.len+1;
    ac->name.bytes = malloc(klass->name.len+1);
    ac->name.bytes[0] = '[';
    memcpy(&ac->name.bytes[1], klass->name.bytes, klass->name.len);
    ac->instsize = 0;
    ac->super = &class_java_Dlang_DObject;
    ac->vtable = vtable_array;
    ac->eltsoffset = offsetof(struct { struct array head; struct reference data[]; }, data);
    ac->numiface = 0;
    ac->arrayklass = NULL;
    ac->elementklass = klass;
    klass->arrayklass = ac;
  }
  return klass->arrayklass;
}

#define PRIM_ARRAY(t,ctype) struct klass t ## _array = { { sizeof("[" # t)-1, "[" # t }, 0, &class_java_Dlang_DObject, vtable_array, NULL, NULL, offsetof(struct { struct array head; ctype data[]; }, data), 0 }

PRIM_ARRAY(bool, bool);
PRIM_ARRAY(byte, int8_t);
PRIM_ARRAY(short, int16_t);
PRIM_ARRAY(char, uint16_t);
PRIM_ARRAY(int, int32_t);
PRIM_ARRAY(long, int64_t);
PRIM_ARRAY(float, float);
PRIM_ARRAY(double, double);

#undef PRIM_ARRAY

struct array *
allocate_array(struct klass **aclasses, int32_t *dims, int32_t ndims, size_t eltsize);

struct array *
new_array(uint8_t k, struct klass *et, int32_t ndims, int32_t dim0, ...)
{
  va_list vdims;
  struct klass *aclass;
  size_t eltsize;
  struct array *a;
  void *data;
  size_t datasize;
  assert(ndims > 0);

  int32_t dims[ndims];
  dims[0] = dim0;
  va_start(vdims, dim0);
  for (int i = 1; i < ndims; ++i) {
    dims[i] = va_arg(vdims, int32_t);
  }
  va_end(vdims);

  switch (k) {
    case BOOL:
      aclass = &bool_array;
      eltsize = sizeof(bool);
      break;
    case BYTE:
      aclass = &byte_array;
      eltsize = sizeof(int8_t);
      break;
    case SHORT:
      aclass = &short_array;
      eltsize = sizeof(int16_t);
      break;
    case CHAR:
      aclass = &char_array;
      eltsize = sizeof(uint16_t);
      break;
    case INT:
      aclass = &int_array;
      eltsize = sizeof(int32_t);
      break;
    case LONG:
      aclass = &long_array;
      eltsize = sizeof(int64_t);
      break;
    case FLOAT:
      aclass = &float_array;
      eltsize = sizeof(float);
      break;
    case DOUBLE:
      aclass = &double_array;
      eltsize = sizeof(double);
      break;
    case OBJECT:
      aclass = arrayOf(et);
      eltsize = sizeof(struct reference);
      break;
  }
  struct klass *aclasses[ndims];
  aclasses[ndims-1] = aclass;
  for (int i = 2; i <= ndims; ++i) {
    aclasses[ndims-i] = arrayOf(aclasses[(ndims-i)+1]);
  }

  return allocate_array(aclasses, dims, ndims, eltsize);
}

struct array *
allocate_array(struct klass **aclasses, int32_t *dims, int32_t ndims, size_t eltsize)
{
  int32_t mydim = dims[0];
  struct klass *myclass = aclasses[0];
  size_t datasize;
  if (ndims == 1) {
    datasize = myclass->eltsoffset + mydim * eltsize;
    struct array *me = (struct array*)gcalloc(datasize);
    me->length = mydim;
    me->super.klass = myclass;
    return me;
  } else {
    void *gcfp = rt_openframe();
    datasize = myclass->eltsoffset + mydim * sizeof(struct reference);
    struct array *me = (struct array*)gcalloc(datasize);
    me->length = mydim;
    me->super.klass = myclass;
    rt_pushref((struct java_lang_Object*)me);
    struct reference *mydata = ARRAY_DATA(me, struct reference);
    for (int i = 0; i < mydim; ++i) {
      mydata[i].vtable = aclasses[1]->vtable;
      mydata[i].object = (struct java_lang_Object*)allocate_array(aclasses + 1, dims + 1, ndims - 1, eltsize);
    }
    rt_closeframe(gcfp);
    return me;
  }
}

extern struct klass class_java_Dlang_DArrayIndexOutOfBoundsException;

extern void
method_java_Dlang_DArrayIndexOutOfBoundsException_M_Linit_G_Rjava_Dlang_DArrayIndexOutOfBoundsException(
    struct java_lang_Object *, vtable_t);

void rt_assertArrayBounds(struct array *arr,
                          int32_t i)
{
  if(i < 0 || i >= arr->length) {
    struct java_lang_Object *exception = rt_new(&class_java_Dlang_DArrayIndexOutOfBoundsException);
    void *uwx;
    method_java_Dlang_DArrayIndexOutOfBoundsException_M_Linit_G_Rjava_Dlang_DArrayIndexOutOfBoundsException(exception, rt_loadvtable(exception));
    uwx = createOurException(exception);
    _Unwind_RaiseException(uwx);
  }
}

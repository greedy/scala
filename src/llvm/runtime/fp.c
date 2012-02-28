#include "object.h"

#include <math.h>

float method__Ojava_Dlang_DFloat_MintBitsToFloat_Ascala_DInt_Rscala_DFloat(
    struct java_lang_Object *self, vtable_t selfVtable,
    int32_t v)
{
  /* XXX is this really valid; how to do it right? */
  return *(float*)&v;
}

double method__Ojava_Dlang_DDouble_MlongBitsToDouble_Ascala_DLong_Rscala_DDouble(
    struct java_lang_Object *self, vtable_t selfVtable,
    int64_t v)
{
  /* XXX is this really valid; how to do it right? */
  return *(double*)&v;
}

bool method__Ojava_Dlang_DFloat_MisNaN_Ascala_DFloat_Rscala_DBoolean(
    struct java_lang_Object *self, vtable_t selfVtable,
    float v)
{
  return isnan(v);
}

bool method__Ojava_Dlang_DDouble_MisNaN_Ascala_DDouble_Rscala_DBoolean(
    struct java_lang_Object *self, vtable_t selfVtable,
    double v)
{
  return isnan(v);
}

bool method__Ojava_Dlang_DFloat_MisInfinite_Ascala_DFloat_Rscala_DBoolean(
    struct java_lang_Object *self, vtable_t selfVtable,
    float v)
{
  return isinf(v);
}

bool method__Ojava_Dlang_DDouble_MisInfinite_Ascala_DDouble_Rscala_DBoolean(
    struct java_lang_Object *self, vtable_t selfVtable,
    double v)
{
  return isinf(v);
}

#ifndef BOXES_H
#define BOXES_H

#include "object.h"
#include "strings.h"

#define DECLARE_BOX(p,c)                              \
  struct java_lang_ ## c;                             \
  extern struct java_lang_Object*                     \
    rt_box_ ## c (p v);                               \
  extern p                                            \
    rt_unbox_ ## c (struct java_lang_Object *v)

DECLARE_BOX(bool, Boolean);
DECLARE_BOX(int8_t, Byte);
DECLARE_BOX(int16_t, Short);
DECLARE_BOX(int32_t, Integer);
DECLARE_BOX(int64_t, Long);
DECLARE_BOX(float, Float);
DECLARE_BOX(double, Double);
DECLARE_BOX(UChar32, Char);

#undef DECLARE_BOX

#endif

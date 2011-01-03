#include "boxes.h"
#include "runtime.h"

#define DEFINE_BOX(p,c,g)                                                         \
  struct object_java_lang_ ## c;                                                  \
  extern struct object_java_lang_ ## c module__Ojava_Dlang_D ## c;                \
  extern void initmodule_module__Ojava_Dlang_D ## c();                            \
  extern struct java_lang_ ## c *                                                 \
  method__Ojava_Dlang_D ## c ## _MvalueOf_Ascala_D ## c ## _Rjava_Dlang_D ## c    \
    (struct object_java_lang_ ## c *, p);                                         \
  extern p                                                                        \
  method__Ojava_Dlang_D ## c ## _M ## g ## _Rscala_D ## c (                       \
      struct java_lang_ ## c *);                                                  \
  struct java_lang_ ## c *rt_box_ ## c (p v)                                      \
  {                                                                               \
    initmodule_module__Ojava_Dlang_D ## c();                                      \
    return                                                                        \
    method__Ojava_Dlang_D ## c ## _MvalueOf_Ascala_D ## c ## _Rjava_Dlang_D ## c  \
    (&module__Ojava_Dlang_D ## c, v);                                             \
  }                                                                               \
  p rt_unbox_ ## c(struct java_lang_ ## c *v)                                     \
  {                                                                               \
    return                                                                        \
    method__Ojava_Dlang_D ## c ## _M ## g ## _Rscala_D ## c (v);                  \
  }

DEFINE_BOX(bool, Boolean, booleanValue);
DEFINE_BOX(int8_t, Byte, byteValue);
DEFINE_BOX(int16_t, Short, shortValue);
DEFINE_BOX(int32_t, Integer, intValue);
DEFINE_BOX(int64_t, Long, longValue);
DEFINE_BOX(float, Float, floatValue);
DEFINE_BOX(double, Double, doubleValue);
DEFINE_BOX(uint16_t, Char, charValue);

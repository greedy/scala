#include "boxes.h"
#include "runtime.h"

#define DEFINE_BOX2(p,k,c,g)                                                      \
  struct object_java_lang_ ## c;                                                  \
  extern struct object_java_lang_ ## c module__Ojava_Dlang_D ## c;                \
  extern void initmodule_module__Ojava_Dlang_D ## c();                            \
  extern struct java_lang_ ## c *                                                 \
  method__Ojava_Dlang_D ## c ## _MvalueOf_Ascala_D ## k ## _Rjava_Dlang_D ## c    \
    (struct object_java_lang_ ## c *, p);                                         \
  extern p                                                                        \
  method_java_Dlang_D ## c ## _M ## g ## _Rscala_D ## k (                       \
      struct java_lang_ ## c *);                                                  \
  struct java_lang_ ## c *rt_box_ ## c (p v)                                      \
  {                                                                               \
    initmodule_module__Ojava_Dlang_D ## c();                                      \
    return                                                                        \
    method__Ojava_Dlang_D ## c ## _MvalueOf_Ascala_D ## k ## _Rjava_Dlang_D ## c  \
    (&module__Ojava_Dlang_D ## c, v);                                             \
  }                                                                               \
  p rt_unbox_ ## c(struct java_lang_ ## c *v)                                     \
  {                                                                               \
    return                                                                        \
    method_java_Dlang_D ## c ## _M ## g ## _Rscala_D ## k (v);                  \
  }

#define DEFINE_BOX(p,c,g) DEFINE_BOX2(p,c,c,g)

DEFINE_BOX(bool, Boolean, booleanValue);
DEFINE_BOX(int8_t, Byte, byteValue);
DEFINE_BOX(int16_t, Short, shortValue);
DEFINE_BOX2(int32_t, Int, Integer, intValue);
DEFINE_BOX(int64_t, Long, longValue);
DEFINE_BOX(float, Float, floatValue);
DEFINE_BOX(double, Double, doubleValue);
DEFINE_BOX2(UChar32, Char, Character, charValue);

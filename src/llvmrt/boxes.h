#ifndef BOXES_H
#define BOXES_H

#include "object.h"

#define DECLARE_BOX(p,c)                              \
  struct java_lang_ ## c;                             \
  extern struct java_lang_ ## c *                     \
    rt_box_ ## c (p v);                               \
  extern p                                            \
    rt_unbox_ ## c (struct java_lang_ ## c *v)

DECLARE_BOX(bool, Boolean);
DECLARE_BOX(int8_t, Byte);
DECLARE_BOX(int16_t, Short);
DECLARE_BOX(int32_t, Integer);
DECLARE_BOX(int64_t, Long);
DECLARE_BOX(float, Float);
DECLARE_BOX(double, Double);
DECLARE_BOX(uint16_t, Char);

#undef DECLARE_BOX

#endif

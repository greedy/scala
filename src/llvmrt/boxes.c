#include "boxes.h"
#include "runtime.h"

#define DEFINE_BOX(p,c)                                                       \
  int32_t                                                                     \
  method_java_Dlang_D ## c ## _MhashCode_Rscala_DInt (                        \
      struct java_lang_ ## c *this)                                           \
  {                                                                           \
    return (int32_t)this->value;                                              \
  }                                                                           \
  bool                                                                        \
  method_java_Dlang_D ## c ## _Mequals_Ajava_Dlang_DObject_Rscala_DBoolean (  \
      struct java_lang_ ## c *this,                                           \
      struct java_lang_Object *other)                                         \
  {                                                                           \
    return (other->klass == &class_java_Dlang_D ## c) &&                      \
      (((struct java_lang_ ## c *)other)->value == this->value);              \
  }                                                                           \
  struct java_lang_Object*                                                    \
  method_java_Dlang_D ## c ## _Mclone_Rjava_Dlang_DObject (                   \
      struct java_lang_ ## c *this)                                           \
  {                                                                           \
    return (struct java_lang_Object*)this;                                    \
  }                                                                           \
  static void *vtable_java_lang_ ## c [] = {                                  \
    method_java_Dlang_D ## c ## _MhashCode_Rscala_DInt,                       \
    method_java_Dlang_D ## c ## _Mequals_Ajava_Dlang_DObject_Rscala_DBoolean, \
    method_java_Dlang_D ## c ## _Mclone_Rjava_Dlang_DObject,                  \
    method_java_Dlang_DObject_MtoString_Rjava_Dlang_DString,                  \
    method_java_Dlang_DObject_Mfinalize_Rscala_DUnit                          \
  };                                                                          \
  struct klass class_java_Dlang_D ## c = {                                    \
    "java.lang." # c ,                                                        \
    sizeof(struct java_lang_ ## c),                                           \
    &class_java_Dlang_DObject,                                                \
    vtable_java_lang_ ## c,                                                   \
    0,                                                                        \
  };                                                                          \
  struct java_lang_ ## c *                                                    \
    rt_box_ ## c (p v)                                                        \
  {                                                                           \
    struct java_lang_ ## c *box =                                             \
      (struct java_lang_ ## c *) rt_new(&class_java_Dlang_D ## c);            \
    box->value = v;                                                           \
    return box;                                                               \
  }                                                                           \
  p rt_unbox_ ## c(struct java_lang_ ## c *v)                                 \
  {                                                                           \
    return v->value;                                                          \
  }

DEFINE_BOX(bool, Boolean);
DEFINE_BOX(int8_t, Byte);
DEFINE_BOX(int16_t, Short);
DEFINE_BOX(int32_t, Integer);
DEFINE_BOX(int64_t, Long);
DEFINE_BOX(float, Float);
DEFINE_BOX(double, Double);
DEFINE_BOX(uint16_t, Char);

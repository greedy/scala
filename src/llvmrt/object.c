#include "object.h"

struct java_lang_String;

static void *vtable_java_lang_Object[] = {
  method_java_Dlang_DObject_Mclone_Rjava_Dlang_DObject,
  method_java_Dlang_DObject_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean,
  method_java_Dlang_DObject_Mfinalize_Rscala_DUnit,
  method_java_Dlang_DObject_MhashCode_Rscala_DInt,
  method_java_Dlang_DObject_MtoString_Rjava_Dlang_DString,
};

struct klass class_java_Dlang_DObject = {
  { sizeof("java.lang.Object")-1, "java.lang.Object" },
  sizeof(struct java_lang_Object),
  NULL,
  vtable_java_lang_Object,
  NULL,
  NULL,
  0,
};

int32_t
method_java_Dlang_DObject_MhashCode_Rscala_DInt(
    struct java_lang_Object *this)
{
  return (int32_t)(((intptr_t)this)/sizeof(struct java_lang_Object));
}

bool
method_java_Dlang_DObject_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(
    struct java_lang_Object *this,
    struct java_lang_Object *other)
{
  return this == other;
}

struct java_lang_Object*
method_java_Dlang_DObject_Mclone_Rjava_Dlang_DObject(
    struct java_lang_Object *this)
{
  return NULL;
}

struct java_lang_String*
method_java_Dlang_DObject_MtoString_Rjava_Dlang_DString(
    struct java_lang_Object *this)
{
  return NULL;
}

void
method_java_Dlang_DObject_Mfinalize_Rscala_DUnit(
    struct java_lang_Object *this)
{
  return;
}

extern void
method_java_Dlang_DObject_M_Linit_G_Rjava_Dlang_DObject(
    struct java_lang_Object *this)
{
  return;
}

struct klass class_scala_DScalaObject = {
  { sizeof("scala.ScalaObject")-1, "scala.ScalaObject" },
  0,
  NULL,
  vtable_java_lang_Object,
  NULL,
  NULL,
  0,
};


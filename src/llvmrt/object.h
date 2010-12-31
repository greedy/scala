#ifndef OBJECT_H
#define OBJECT_H

#include "klass.h"

#include <stdint.h>
#include <stdbool.h>

extern struct klass class_java_Dlang_DObject;

struct java_lang_Object {
  struct klass *klass;
};

extern int32_t
method_java_Dlang_DObject_MhashCode_Rscala_DInt(
    struct java_lang_Object *this);

extern bool
method_java_Dlang_DObject_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(
    struct java_lang_Object *this,
    struct java_lang_Object *other);

extern struct java_lang_Object*
method_java_Dlang_DObject_Mclone_Rjava_Dlang_DObject(
    struct java_lang_Object *this);

extern struct java_lang_String*
method_java_Dlang_DObject_MtoString_Rjava_Dlang_DString(
    struct java_lang_Object *this);

extern void
method_java_Dlang_DObject_Mfinalize_Rscala_DUnit(
    struct java_lang_Object *this);

extern void
method_java_Dlang_DObject_M_Linit_G_Rjava_Dlang_DObject(
    struct java_lang_Object *this);

extern struct klass class_scala_DscalaObject;

#endif

#ifndef OBJECT_H
#define OBJECT_H

#include "klass.h"

#include <stdint.h>
#include <stdbool.h>

extern struct klass class_java_Dlang_DObject;

struct java_lang_Object {
  struct klass *klass;
};

#include "runtime.h"

struct java_lang_Class;

extern struct klass class_java_Dlang_DClass;

extern struct java_lang_Class *
rt_classobject(struct klass*);

extern int32_t
method_java_Dlang_DObject_MhashCode_Rscala_DInt(
    struct java_lang_Object *self, vtable selfVtable);

extern bool
method_java_Dlang_DObject_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(
    struct java_lang_Object *self, vtable selfVtable,
    struct java_lang_Object *other, vtable otherVtable);

extern struct java_lang_Object*
method_java_Dlang_DObject_Mclone_Rjava_Dlang_DObject(
    struct java_lang_Object *self, vtable selfVtable,
    vtable *vtableOut);

extern struct java_lang_Object*
method_java_Dlang_DObject_MtoString_Rjava_Dlang_DString(
    struct java_lang_Object *self, vtable selfVtable,
    vtable *vtableOut);

extern void
method_java_Dlang_DObject_Mfinalize_Rscala_DUnit(
    struct java_lang_Object *self, vtable selfVtable);

extern void
method_java_Dlang_DObject_M_Linit_G_Rjava_Dlang_DObject(
    struct java_lang_Object *self, vtable selfVtable);

extern struct klass class_scala_DscalaObject;

#endif

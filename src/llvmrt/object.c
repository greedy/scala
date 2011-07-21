#include "object.h"
#include "strings.h"
#include "runtime.h"

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
    struct java_lang_Object *this, vtable thisVtable)
{
  return (int32_t)(((intptr_t)this)/sizeof(struct java_lang_Object));
}

bool
method_java_Dlang_DObject_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(
    struct java_lang_Object *this, vtable thisVtable,
    struct java_lang_Object *other, vtable otherVtable)
{
  return this == other;
}

struct java_lang_Object*
method_java_Dlang_DObject_Mclone_Rjava_Dlang_DObject(
    struct java_lang_Object *this, vtable thisVtable, vtable *vtableOut)
{
  *vtableOut = NULL;
  return NULL;
}

U_STRING_DECL(s_at, "@", 1);
static bool s_at_initted = false;

struct java_lang_Object*
method_java_Dlang_DObject_MtoString_Rjava_Dlang_DString(
    struct java_lang_Object *this, vtable thisVtable,
    vtable *vtableOut)
{
  struct stringlist *sl = NULL;
  rt_string_append_string(&sl, (struct java_lang_Object*)rt_makestring(&this->klass->name));
  if (!s_at_initted) {
    U_STRING_INIT(s_at, "@", 1);
    s_at_initted = true;
  }
  rt_string_append_ustring(&sl, 1, s_at);
  rt_string_append_Long(&sl, (uint64_t)this);
  struct java_lang_String *s = rt_stringconcat(&sl);
  *vtableOut = rt_loadvtable((struct java_lang_Object*)s);
  return (struct java_lang_Object*)s;
}

void
method_java_Dlang_DObject_Mfinalize_Rscala_DUnit(
    struct java_lang_Object *this, vtable thisVtable)
{
  return;
}

void
method_java_Dlang_DObject_M_Linit_G_Rjava_Dlang_DObject(
    struct java_lang_Object *this, vtable thisVtable)
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

struct java_lang_Class {
  struct java_lang_Object super;
  struct klass *theklass;
};

struct java_lang_Object*
method_java_Dlang_DClass_Mclone_Rjava_Dlang_DObject(struct java_lang_Object *self, vtable selfVtable, vtable *vtableOut)
{
  *vtableOut = self->klass->vtable;
  return self;
}

bool
method_java_Dlang_DClass_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(
    struct java_lang_Object *self, vtable selfVtable, 
    struct java_lang_Object *other, vtable otherVtable)
{
  if (self->klass == other->klass) {
    return ((struct java_lang_Class*)self)->theklass == ((struct java_lang_Class*)other)->theklass;
  } else {
    return false;
  }
}

int32_t
method_java_Dlang_DClass_MhashCode_Rscala_DInt(
    struct java_lang_Object *self, vtable selfVtable)
{
  return (int32_t)(((struct java_lang_Class*)self)->theklass);
}

struct java_lang_Object*
method_java_Dlang_DClass_MgetName_Rjava_Dlang_DString(struct java_lang_Object *self, vtable selfVtable, vtable *vtableOut)
{
  struct java_lang_Object *ret = (struct java_lang_Object*)rt_makestring(&((struct java_lang_Class*)self)->theklass->name);
  *vtableOut = rt_loadvtable(ret);
  return ret;
}

struct java_lang_Object*
method_java_Dlang_DClass_MtoString_Rjava_Dlang_DString(
    struct java_lang_Object *self, vtable selfVtable, vtable *vtableOut)
{
  return method_java_Dlang_DClass_MgetName_Rjava_Dlang_DString(self, selfVtable, vtableOut);
}

/* TODO - cache in klass struct */
extern struct java_lang_Class *
rt_classobject(struct klass* klass)
{
  struct java_lang_Class *klassobj = (struct java_lang_Class*)rt_new(&class_java_Dlang_DClass);
  method_java_Dlang_DObject_M_Linit_G_Rjava_Dlang_DObject((struct java_lang_Object*)klassobj, klassobj->super.klass->vtable);
  klassobj->theklass = klass;
  return klassobj;
}

struct java_lang_Object*
method_java_Dlang_DObject_MgetClass_Rjava_Dlang_DClass(struct java_lang_Object *self, vtable selfVtable, vtable *vtableOut)
{
  struct java_lang_Object *ret = (struct java_lang_Object*)rt_classobject(self->klass);
  *vtableOut = rt_loadvtable(ret);
  return ret;
}

static void *vtable_java_lang_Class[] = {
  method_java_Dlang_DClass_Mclone_Rjava_Dlang_DObject,
  method_java_Dlang_DClass_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean,
  method_java_Dlang_DObject_Mfinalize_Rscala_DUnit,
  method_java_Dlang_DClass_MhashCode_Rscala_DInt,
  method_java_Dlang_DClass_MtoString_Rjava_Dlang_DString,
};

struct klass class_java_Dlang_DClass = {
  { sizeof("java.lang.Class")-1, "java.lang.Class" },
  sizeof(struct java_lang_Class),
  &class_java_Dlang_DObject,
  vtable_java_lang_Class,
  NULL,
  NULL,
  0,
};


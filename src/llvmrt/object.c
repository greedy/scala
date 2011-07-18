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
    struct reference thisref)
{
  struct java_lang_Object *this = thisref.object;
  return (int32_t)(((intptr_t)this)/sizeof(struct java_lang_Object));
}

bool
method_java_Dlang_DObject_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(
    struct reference thisref,
    struct reference otherref)
{
  return thisref.object == otherref.object;
}

struct reference
method_java_Dlang_DObject_Mclone_Rjava_Dlang_DObject(
    struct reference thisref)
{
  return (struct reference){NULL,NULL};
}

U_STRING_DECL(s_at, "@", 1);
static bool s_at_initted = false;

struct reference
method_java_Dlang_DObject_MtoString_Rjava_Dlang_DString(
    struct reference this)
{
  struct stringlist *sl = NULL;
  rt_string_append_string(&sl, (struct java_lang_Object*)rt_makestring(&this.object->klass->name));
  if (!s_at_initted) {
    U_STRING_INIT(s_at, "@", 1);
    s_at_initted = true;
  }
  rt_string_append_ustring(&sl, 1, s_at);
  rt_string_append_Long(&sl, (uint64_t)this.object);
  return makeref((struct java_lang_Object*)rt_stringconcat(&sl));
}

void
method_java_Dlang_DObject_Mfinalize_Rscala_DUnit(
    struct reference this)
{
  return;
}

void
method_java_Dlang_DObject_M_Linit_G_Rjava_Dlang_DObject(
    struct reference this)
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

struct reference
method_java_Dlang_DClass_Mclone_Rjava_Dlang_DObject(struct reference selfref)
{
  return selfref;
}

bool
method_java_Dlang_DClass_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(
    struct reference selfref, 
    struct reference otherref)
{
  if (selfref.object->klass == otherref.object->klass) {
    return ((struct java_lang_Class*)selfref.object)->theklass == ((struct java_lang_Class*)otherref.object)->theklass;
  } else {
    return false;
  }
}

int32_t
method_java_Dlang_DClass_MhashCode_Rscala_DInt(
    struct reference selfref)
{
  struct java_lang_Class *self = (struct java_lang_Class*)selfref.object;
  return (int32_t)(self->theklass);
}

struct reference
method_java_Dlang_DClass_MgetName_Rjava_Dlang_DString(struct reference selfref)
{
  return makeref((struct java_lang_Object*)rt_makestring(&((struct java_lang_Class*)selfref.object)->theklass->name));
}

struct reference
method_java_Dlang_DClass_MtoString_Rjava_Dlang_DString(
    struct reference selfref)
{
  return method_java_Dlang_DClass_MgetName_Rjava_Dlang_DString(selfref);
}

/* TODO - cache in klass struct */
extern struct java_lang_Class *
rt_classobject(struct klass* klass)
{
  struct java_lang_Class *klassobj = (struct java_lang_Class*)rt_new(&class_java_Dlang_DClass);
  method_java_Dlang_DObject_M_Linit_G_Rjava_Dlang_DObject(makeref((struct java_lang_Object*)klassobj));
  klassobj->theklass = klass;
  return klassobj;
}

struct reference
method_java_Dlang_DObject_MgetClass_Rjava_Dlang_DClass(struct reference selfref)
{
  return makeref((struct java_lang_Object*)rt_classobject(selfref.object->klass));
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


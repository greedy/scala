#include "klass.h"
#include "object.h"
#include "runtime.h"
#include "arrays.h"
#include "strings.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unicode/ustring.h>
#include <unicode/utypes.h>

static void printclassname(FILE* f, struct klass *klass)
{
  fprintf(f, "%.*s", klass->name.len, klass->name.bytes);
}

void rt_initobj(struct java_lang_Object *object, struct klass *klass)
{
  object->klass = klass;
}

void rt_delete(struct java_lang_Object *object)
{
  if (object != NULL) free(object);
}

void **rt_iface_cast(struct java_lang_Object *object, struct klass *iface)
{
  //fprintf(stderr, "rt_iface_cast(%p, %p)\n", object, iface);
  if (object == NULL) return NULL;
  struct klass *klass = object->klass;
  void **vtable = NULL;
  uint32_t i;
  //fprintf(stderr, "Casting %p %.*s to %p %.*s\n", object, klass->name.len, klass->name.bytes, iface, iface->name.len, iface->name.bytes);
  for (i = 0; i < klass->numiface; i++) {
    if (klass->ifaces[i].klass == iface) {
      vtable = klass->ifaces[i].vtable;
    }
  }
  if (vtable != NULL) {
    return vtable;
  } else {
    fprintf(stderr, "Cant cast %p %.*s to %p %.*s\n", object, klass->name.len, klass->name.bytes, iface, iface->name.len, iface->name.bytes);
    abort();
  }
}

bool rt_isinstance(struct java_lang_Object *object, struct klass *classoriface)
{
  bool res;
  if (classoriface->instsize == 0)
    res = rt_isinstance_iface(object, classoriface);
  else
    res = rt_isinstance_class(object, classoriface);
  return res;
}

bool rt_issubclass(struct klass *super, struct klass *sub)
{
  if (super == sub) return true;
  if (super->instsize == 0 && sub->instsize == 0) {
    if ((super->elementklass != NULL) && (sub->elementklass != NULL)) {
      super = super->elementklass;
      sub = sub->elementklass;
    }
  }
  struct klass *checkclass = sub;
  while (checkclass != NULL) {
    if (super == checkclass) {
      return true;
    } else {
      checkclass = checkclass->super;
    }
  }
  return false;
}

bool rt_isinstance_class(struct java_lang_Object *object, struct klass *klass)
{
  return rt_issubclass(klass, object->klass);
}

bool rt_isinstance_iface(struct java_lang_Object *object, struct klass *iface)
{
  struct klass *klass = object->klass;
  uint32_t i;
  for (i = 0; i < klass->numiface; i++) {
    if (klass->ifaces[i].klass == iface) {
      return true;
    }
  }
  return false;
}

void rt_init_loop()
{
  fprintf(stderr, "PANIC: Initialization loop\n");
  abort();
}

extern struct klass class_java_Dlang_DNullPointerException;

extern void
method_java_Dlang_DNullPointerException_M_Linit_G_Rjava_Dlang_DNullPointerException(
    struct java_lang_Object *self, vtable_t selfVtable);

void rt_assertNotNull(struct java_lang_Object *object)
{
  if (object == NULL) {
    struct java_lang_Object *exception = rt_new(&class_java_Dlang_DNullPointerException);
    void *uwx;
    method_java_Dlang_DNullPointerException_M_Linit_G_Rjava_Dlang_DNullPointerException(exception, rt_loadvtable(exception));
    uwx = createOurException(exception);
    _Unwind_RaiseException(uwx);
    __builtin_unreachable();
  }
}

typedef struct java_lang_Object* (*toStringFn)(struct java_lang_Object *, vtable_t, vtable_t*);

void rt_printexception(struct java_lang_Object *object)
{
  fprintf(stderr, "Uncaught exception: %.*s\n", object->klass->name.len, object->klass->name.bytes);
  toStringFn toString;
  toString = object->klass->vtable[4];
  vtable_t tempVtable;
  struct java_lang_String *ss;
  ss = (struct java_lang_String*)toString(object, rt_loadvtable(object), &tempVtable);
  int32_t bufreq;
  UErrorCode err = U_ZERO_ERROR;
  u_strToUTF8(NULL, 0, &bufreq, ss->s, ss->len, &err);
  if (U_SUCCESS(err) || err == U_BUFFER_OVERFLOW_ERROR) {
    err = U_ZERO_ERROR;
    int32_t buflen = bufreq+1;
    char u8buf[buflen];
    u_strToUTF8(u8buf, buflen, NULL, ss->s, ss->len, &err);
    if (U_SUCCESS(err)) {
      u8buf[buflen-1] = 0;
      fputs(u8buf, stderr);
    }
  }
}

void *rt_argvtoarray(int argc, char **argv)
{
  struct array *ret = new_array(OBJECT, &class_java_Dlang_DString, 1, argc);
  struct reference* elements = ARRAY_DATA(ret, struct reference);
  struct utf8str temp;
  for (int i = 0; i < argc; i++) {
    temp.len = strlen(argv[i]);
    temp.bytes = argv[i];
    struct java_lang_Object *s = (struct java_lang_Object*)rt_makestring(&temp);
    elements[i].object = s;
    elements[i].vtable = rt_loadvtable(s);
  }
  return (void*)ret;
}

vtable_t rt_loadvtable(struct java_lang_Object *obj)
{
  if (obj == NULL) {
    //fprintf(stderr, "Loading vtable of NULL\n");
    return NULL;
  } else {
    //fprintf(stderr, "Loading vtable of %p (a %.*s) -> %p\n", obj, obj->klass->name.len, obj->klass->name.bytes, obj->klass->vtable);
    return obj->klass->vtable;
  }
}

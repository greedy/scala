#include "klass.h"
#include "object.h"
#include "runtime.h"
#include "arrays.h"
#include "strings.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

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
  struct klass *klass = object->klass;
  void **vtable = NULL;
  uint32_t i;
  for (i = 0; i < klass->numiface; i++) {
    if (klass->ifaces[i].klass == iface) {
      vtable = klass->ifaces[i].vtable;
    }
  }
  if (vtable != NULL) {
    return vtable;
  } else {
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
  if (super->instsize == 0) {
    /* array */
    if (sub->instsize == 0) {
      if ((super->elementklass != NULL) && (sub->elementklass != NULL)) {
        return rt_issubclass(super->elementklass, sub->elementklass);
      }
    }
  } else {
    struct klass *checkclass = sub;
    while (checkclass != NULL) {
      if (super == checkclass) {
        return true;
      } else {
        checkclass = checkclass->super;
      }
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

void rt_printexception(struct java_lang_Object *object)
{
  fprintf(stderr, "Uncaught exception: %.*s\n", object->klass->name.len, object->klass->name.bytes);
}

void *rt_argvtoarray(int argc, char **argv)
{
  struct array *ret = new_array(OBJECT, &class_java_Dlang_DString, 1, argc);
  struct reference* elements = (struct reference*)(&(ret->data[0]));
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
  if (obj == NULL)
    return NULL;
  else
    return obj->klass->vtable;
}

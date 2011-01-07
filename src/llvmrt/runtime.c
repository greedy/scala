#include "klass.h"
#include "object.h"
#include "runtime.h"
#include <stdlib.h>
#include <stdio.h>

extern int32_t _Unwind_RaiseException(void*);
struct java_lang_Object *rt_new(struct klass *klass)
{
  struct java_lang_Object* obj = (struct java_lang_Object*)calloc(1, klass->instsize);
  rt_initobj(obj, klass);
  return obj;
}

void rt_initobj(struct java_lang_Object *object, struct klass *klass)
{
  object->klass = klass;
}

void rt_delete(struct java_lang_Object *object)
{
  if (object != NULL) free(object);
}

struct ifaceref *rt_iface_cast(struct java_lang_Object *object, struct klass *iface)
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
    struct ifaceref *ret = calloc(1, sizeof(struct ifaceref));
    ret->object = object;
    ret->vtable = vtable;
    return ret;
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

bool rt_isinstance_class(struct java_lang_Object *object, struct klass *klass)
{
  struct klass *checkclass = object->klass;
  while (checkclass != NULL) {
    if (klass == checkclass) {
      return true;
    } else {
      checkclass = checkclass->super;
    }
  }
  return false;
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
    struct java_lang_Object *);

void rt_assertNotNull(struct java_lang_Object *object)
{
  if (object == NULL) {
    struct java_lang_Object *exception = rt_new(&class_java_Dlang_DNullPointerException);
    void *uwx;
    method_java_Dlang_DNullPointerException_M_Linit_G_Rjava_Dlang_DNullPointerException(exception);
    uwx = createOurException(exception);
    _Unwind_RaiseException(uwx);
  }
}

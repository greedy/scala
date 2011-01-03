#include "klass.h"
#include "object.h"
#include "runtime.h"
#include <stdlib.h>
#include <stdio.h>

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
  return rt_isinstance_class(object, classoriface) ||
    rt_isinstance_iface(object, classoriface);
}

bool rt_isinstance_class(struct java_lang_Object *object, struct klass *klass)
{
  struct klass *checkclass = klass;
  while (checkclass != NULL) {
    if (object->klass == checkclass) {
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

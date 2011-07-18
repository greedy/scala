#ifndef RUNTIME_H
#define RUNTIME_H

struct reference {
  struct java_lang_Object *object;
  void **vtable;
};

#include "object.h"
#include "klass.h"

inline struct reference makeref(struct java_lang_Object *object) {
  struct reference ret = {NULL,NULL};
  if (object != NULL) {
    ret.object = object;
    ret.vtable = object->klass->vtable;
  }
  return ret;
}

extern struct java_lang_Object* rt_new(struct klass *klass);
extern void rt_initobj(struct java_lang_Object *object, struct klass *klass);
extern void rt_delete(struct java_lang_Object *object);
extern void **rt_iface_cast(struct java_lang_Object *object, struct klass *iface);
extern bool rt_isinstance(struct java_lang_Object *object, struct klass *classoriface);
extern bool rt_isinstance_class(struct java_lang_Object *object, struct klass *klass);
extern bool rt_isinstance_iface(struct java_lang_Object *object, struct klass *iface);
extern void rt_init_loop();
extern void rt_assertNotNull(struct java_lang_Object *object);

extern void* createOurException(struct java_lang_Object *obj);

#endif

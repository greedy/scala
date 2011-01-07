#ifndef RUNTIME_H
#define RUNTIME_H

#include "object.h"
#include "klass.h"

struct ifaceref {
  struct java_lang_Object *object;
  void **vtable;
};

extern struct java_lang_Object* rt_new(struct klass *klass);
extern void rt_initobj(struct java_lang_Object *object, struct klass *klass);
extern void rt_delete(struct java_lang_Object *object);
extern struct ifaceref *rt_iface_cast(struct java_lang_Object *object, struct klass *iface);
extern bool rt_isinstance(struct java_lang_Object *object, struct klass *classoriface);
extern bool rt_isinstance_class(struct java_lang_Object *object, struct klass *klass);
extern bool rt_isinstance_iface(struct java_lang_Object *object, struct klass *iface);
extern void rt_init_loop();
extern void rt_assertNotNull(struct java_lang_Object *object);

extern void* createOurException(struct java_lang_Object *obj);

#endif

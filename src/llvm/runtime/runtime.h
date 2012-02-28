#ifndef RUNTIME_H
#define RUNTIME_H

typedef void** vtable_t;

struct reference {
  struct java_lang_Object *object;
  vtable_t vtable;
};

#include "object.h"
#include "klass.h"

extern struct java_lang_Object* rt_new(struct klass *klass);
extern void rt_initobj(struct java_lang_Object *object, struct klass *klass);
extern void rt_delete(struct java_lang_Object *object);
extern vtable_t rt_iface_cast(struct java_lang_Object *object, struct klass *iface);
extern bool rt_isinstance(struct java_lang_Object *object, struct klass *classoriface);
extern bool rt_isinstance_class(struct java_lang_Object *object, struct klass *klass);
extern bool rt_isinstance_iface(struct java_lang_Object *object, struct klass *iface);
extern void rt_init_loop();
extern void rt_assertNotNull(struct java_lang_Object *object);
extern vtable_t rt_loadvtable(struct java_lang_Object *object);

extern void* createOurException(struct java_lang_Object *obj);

extern int32_t _Unwind_RaiseException(void*);

#endif

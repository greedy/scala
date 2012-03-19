#ifndef GC_H
#define GC_H

#include <stdint.h>

struct java_lang_Object;

struct java_lang_Object* gcalloc(size_t nbytes);
void rt_pushref(struct java_lang_Object* obj);
void rt_popref();
void rt_addroot(struct java_lang_Object** obj);
void* rt_openframe();
void rt_localcell(struct java_lang_Object** cell);
void rt_closeframe(void *);

#endif

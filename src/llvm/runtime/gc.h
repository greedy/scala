#ifndef GC_H
#define GC_H

#include <stdint.h>

struct java_lang_Object;

struct java_lang_Object* gcalloc(size_t nbytes);
void rt_pushref(struct java_lang_Object* obj);
void rt_popref(uint32_t n);

#endif

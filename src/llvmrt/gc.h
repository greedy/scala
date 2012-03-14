#ifndef GC_H
#define GC_H

struct java_lang_Object;

struct java_lang_Object* gcalloc(size_t nbytes);
void rt_pushref(struct java_lang_Object* obj);
void rt_popref(size_t n);

#endif

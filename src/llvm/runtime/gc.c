#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <time.h>
#include <assert.h>

#include "arrays.h"
#include "runtime.h"
#include "klass.h"
#include "gc.h"

#define GC_DEBUG 1

void marksweep();

struct gcobj {
  struct gcobj* prev;
  struct gcobj* nextwork;
  size_t sz;
  char obj[];
};

enum slottype {
  OPSTACK,
  LOCAL
};

struct stackslot {
  enum slottype t;
  union {
    struct gcobj *opstack;
    struct java_lang_Object **local;
  } d;
};

static struct gcobj* head = NULL;

/* 1MB shadow stack */
#define STACK_SIZE (32L*1024L*1024L/sizeof(struct stackslot))
static struct stackslot shadowstack[STACK_SIZE];
static struct stackslot* shadowsp = &(shadowstack[0]);
static struct stackslot* shadowlimit = &(shadowstack[STACK_SIZE]);

#define ROOT_SIZE (1024L)
static struct java_lang_Object** staticroots[ROOT_SIZE];
static struct java_lang_Object*** nextroot = &(staticroots[0]);
static struct java_lang_Object*** rootlimit = &(staticroots[ROOT_SIZE]);

static size_t heapsize = 0;
#define HEAPLIMIT (8L*1024L*1024L*1024L)
#define INITHEAP (1024L*1024L*1024L)
#define HEAPINC (256*1024L*1024L)

static size_t curmax = INITHEAP;

static inline struct java_lang_Object* gc2object(struct gcobj *gc) {
  if (gc == NULL) return NULL;
  return (struct java_lang_Object*)&(gc->obj[0]);
}

static inline struct gcobj* object2gc(struct java_lang_Object *obj) {
  if (obj == NULL) return NULL;
  struct gcobj *gcp = (struct gcobj*)(((void*)obj)-offsetof(struct gcobj, obj[0]));
  assert(obj == gc2object(gcp));
  return gcp;
}

struct java_lang_Object* rt_new(struct klass *klass)
{
  struct java_lang_Object *obj = gcalloc(klass->instsize);
  //fprintf(stderr, "allocated a %.*s at %p\n", klass->name.len, klass->name.bytes, obj);
  rt_initobj(obj, klass);
  return obj;
}

struct java_lang_Object* gcalloc(size_t nbytes) {
  size_t objsize = sizeof(struct gcobj)+nbytes;
  if (heapsize + objsize > curmax) {
    marksweep();
    if (heapsize + objsize + HEAPINC > curmax) {
      curmax = curmax + HEAPINC;
#if GC_DEBUG >= 1
      fprintf(stderr, "Increasing heap size to %zu\n", curmax);
#endif
    }
  }
  if (heapsize + objsize > HEAPLIMIT) {
    fprintf(stderr, "Out of heap\n");
    abort();
  }
  struct gcobj* gcp = (struct gcobj*)calloc(1, objsize);
  gcp->sz = objsize;
#if GC_DEBUG >= 2
  if ((heapsize + gcp->sz)/(1<<28) != heapsize/(1<<28)) {
    fprintf(stderr, "allocating %zu heapsize is now %zu, limit %zu\n", objsize, heapsize, curmax);
  }
#endif
  heapsize += gcp->sz;
  struct java_lang_Object* obj = gc2object(gcp);
  gcp->prev = head;
  head = gcp;
  return obj;
}

static void dumpshadow() {
  for (struct stackslot *i = shadowsp-1; i >= &(shadowstack[0]); i--) {
    switch (i->t) {
      case OPSTACK:
        fprintf(stderr, "%p %td OPSTACK %p\n", i, shadowsp-i, i->d.opstack);
        break;
      case LOCAL:
        fprintf(stderr, "%p %td LOCAL %p(%p)\n", i, shadowsp-i, i->d.local, *(i->d.local));
        break;
    }
  }
}

void rt_addroot(struct java_lang_Object** obj) {
  if (nextroot == rootlimit) {
    fprintf(stderr, "Too many roots\n");
    fflush(stderr);
    abort();
  }
  *(nextroot++) = obj;
}

void* rt_openframe() {
#if GC_DEBUG >= 5
  fprintf(stderr, "openframe %p\n", shadowsp);
#endif
  return shadowsp;
}

void rt_closeframe(void* fp) {
#if GC_DEBUG >= 5
  fprintf(stderr, "closeframe %p to %p\n", shadowsp, fp);
#endif
  shadowsp = (struct stackslot*)fp;
}

void rt_localcell(struct java_lang_Object** cell) {
#if GC_DEBUG >= 5
  fprintf(stderr, "rt_localcall %p(%p)\n", cell, *cell);
#endif
  if (shadowsp == shadowlimit) {
    fprintf(stderr, "Stack overflow\n");
    fflush(stderr);
    abort();
  }
  *(shadowsp++) = (struct stackslot) { .t = LOCAL, .d.local = cell };
#if GC_DEBUG >= 6
  dumpshadow();
#endif
}

void rt_pushref(struct java_lang_Object* obj) {
#if GC_DEBUG >= 5
  fprintf(stderr, "Push %p depth=%td\n", obj, (shadowsp-&(shadowstack[0])));
#endif
  if (shadowsp == shadowlimit) {
    fprintf(stderr, "Stack overflow\n");
    fflush(stderr);
    abort();
  }
  *(shadowsp++) = (struct stackslot) { .t = OPSTACK, .d.opstack = object2gc(obj) };
#if GC_DEBUG >= 6
  dumpshadow();
#endif
}

void rt_popref() {
  if (shadowsp-&(shadowstack[0]) < 1) {
    fprintf(stderr, "Stack underflow\n");
    fflush(stderr);
    abort();
  }
#if GC_DEBUG >= 5
  fprintf(stderr, "Pop depth=%td\n", (shadowsp-&(shadowstack[0])));
#endif
  if ((shadowsp-1)->t != OPSTACK) {
    fprintf(stderr, "OOPS: Tried to pop a cell\n");
    dumpshadow();
    fflush(stderr);
    abort();
  }
  shadowsp--;
#if GC_DEBUG >= 6
  dumpshadow();
#endif
}

void marksweep() {
#if GC_DEBUG >= 1
  static clock_t last = 0;
  clock_t start = clock();
  if (last != 0) {
    fprintf(stderr, "start collecting, mutator ran for %g seconds\n", (float)(last-start)/CLOCKS_PER_SEC);
  }
  fprintf(stderr, "collecting heapsize = %zu\n", heapsize);
#endif
  struct gcobj* workq = NULL;
  size_t workqsz = 0;
#if GC_DEBUG >= 2
#if GC_DEBUG >= 6
  dumpshadow();
#endif
  fprintf(stderr, "scanning static roots\n");
#endif
  for (struct java_lang_Object*** curp = &(staticroots[0]); curp < nextroot; curp++) {
    struct gcobj* cur = object2gc(**curp);
    if (cur == NULL) continue;
#if GC_DEBUG >= 4
    fprintf(stderr, "visiting %p nw %p\n", cur, cur->nextwork);
#endif
    if (!cur->nextwork) {
      workqsz++;
      if (workq) {
        cur->nextwork = workq;
      } else {
        cur->nextwork = cur;
      }
      workq = cur;
    }
  }
#if GC_DEBUG >= 2
  fprintf(stderr, "scanning shadowstack\n");
#endif
  for (struct stackslot *curp = &(shadowstack[0]); curp < shadowsp; curp++) {
    struct gcobj* cur;
    switch (curp->t) {
      case OPSTACK:
        cur = curp->d.opstack;
        break;
      case LOCAL:
        cur = object2gc(*curp->d.local);
        break;
    }
    if (cur == NULL) continue;
#if GC_DEBUG >= 4
    fprintf(stderr, "visiting %p nw %p\n", cur, cur->nextwork);
#endif
    if (!cur->nextwork) {
      workqsz++;
      if (workq) {
        cur->nextwork = workq;
      } else {
        cur->nextwork = cur;
      }
      workq = cur;
    }
  }
#if GC_DEBUG >= 4
  {
    size_t i = 0;
    struct gcobj *p;
    for (p = workq; p->nextwork != p; p = p->nextwork) {
      fprintf(stderr, "workq entry %zu %p\n", i, gc2object(p));
      i++;
    }
    fprintf(stderr, "workq entry %zu %p\n", i, gc2object(p));
    i++;
  }
#endif
#if GC_DEBUG >= 2
  fprintf(stderr, "about to walk pointers\n");
#endif
  while (workq) {
#if GC_DEBUG >= 4
    fprintf(stderr, "workq size is %zu\n", workqsz);
#endif
    struct gcobj* cur = workq;
    workq = cur->nextwork;
    workqsz--;
    if (workq == cur) workq = NULL;
    struct java_lang_Object* obj = gc2object(cur);
#if GC_DEBUG >= 4
    fprintf(stderr, "tracing %p, a %.*s\n", obj, obj->klass->name.len, obj->klass->name.bytes);
#endif
    struct klass* k = obj->klass;
    if (k->instsize == 0) {
      struct array* a = (struct array*)obj;
      if (k->elementklass) {
        struct reference* data = ARRAY_DATA(a, struct reference);
        for (size_t i = 0; i < a->length; i++) {
          struct java_lang_Object* p = (data+i)->object;
          if (p == NULL) continue;
          struct gcobj* gcp = object2gc(p);
          if (gcp->nextwork == NULL) {
#if GC_DEBUG >= 4
            fprintf(stderr, "adding %p to workq\n", p);
#endif
            if (workq == NULL) {
              gcp->nextwork = gcp;
            } else {
              gcp->nextwork = workq;
            }
            workq = gcp;
            workqsz++;
          }
        }
      }
    } else {
      while (k != &class_java_Dlang_DObject) {
        struct klass* sk = k->super;
#if GC_DEBUG >= 4
        fprintf(stderr, "in class %.*s has %d references\n", k->name.len>64?64:k->name.len, k->name.bytes, k->npointers);
#endif
        struct reference* ps;
        ps = (struct reference*)(((char*)obj)+sk->instsize);
        for (size_t i = 0; i < k->npointers; ++i) {
          struct java_lang_Object* p = (ps+i)->object;
          if (p != NULL) {
            struct gcobj* gcp = object2gc(p);
            if (gcp->nextwork == NULL) {
#if GC_DEBUG >= 4
              fprintf(stderr, "adding %p to workq\n", p);
#endif
              if (workq == NULL) {
                gcp->nextwork = gcp;
              } else {
                gcp->nextwork = workq;
              }
              workq = gcp;
              workqsz++;
            }
          }
        }
        k = sk;
      }
    }
  }
#if GC_DEBUG >= 2
  fprintf(stderr, "tracing complete\n");
#endif
  /* everything live has non-null nextwork */
  workq = head;
  head = NULL;
  while (workq) {
    struct gcobj* cur = workq;
    workq = cur->prev;
    if (cur->nextwork) {
      /* cur is live */
      cur->prev = head;
      head = cur;
      cur->nextwork = NULL;
    } else {
#if 0
      if (cur->klass-> == /*...*/) {
      }
#endif
      heapsize -= cur->sz;
      free(cur);
    }
  }
#if GC_DEBUG >= 1
  clock_t end = clock();
  fprintf(stderr, "done collecting, heapsize=%zu time %g seconds\n", heapsize, (float)(end-start)/CLOCKS_PER_SEC);
  last = clock();
#endif
}

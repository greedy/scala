#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>

#include "arrays.h"
#include "runtime.h"
#include "klass.h"
#include "gc.h"

void marksweep();

struct gcobj {
  struct gcobj* prev;
  struct gcobj* nextwork;
  size_t sz;
  char obj[];
};

static struct gcobj* head = NULL;

/* 1MB shadow stack */
#define STACK_SIZE (1024*1024/sizeof(struct gcobj*))
static struct gcobj* shadowstack[STACK_SIZE];
static struct gcobj** shadowsp = &(shadowstack[0]);
static struct gcobj** shadowlimit = &(shadowstack[STACK_SIZE]);

#define ROOT_SIZE (1024)
static struct java_lang_Object** staticroots[ROOT_SIZE];
static struct java_lang_Object*** nextroot = &(staticroots[0]);
static struct java_lang_Object*** rootlimit = &(staticroots[ROOT_SIZE]);

static size_t heapsize = 0;
#define HEAPLIMIT (64*1024*1024)

static inline struct gcobj* object2gc(struct java_lang_Object *obj) {
  if (obj == NULL) return NULL;
  return (struct gcobj*)(((void*)obj)-offsetof(struct gcobj, obj[0]));
}

static inline struct java_lang_Object* gc2object(struct gcobj *gc) {
  if (gc == NULL) return NULL;
  return (struct java_lang_Object*)&(gc->obj[0]);
}

struct java_lang_Object* rt_new(struct klass *klass)
{
  struct java_lang_Object *obj = gcalloc(klass->instsize);
  rt_initobj(obj, klass);
  return obj;
}

struct java_lang_Object* gcalloc(size_t nbytes) {
  size_t objsize = sizeof(struct gcobj)+nbytes;
  if (heapsize + objsize > HEAPLIMIT) marksweep();
  if (heapsize + objsize > HEAPLIMIT) {
    fprintf(stderr, "Out of heap\n");
    abort();
  }
  struct gcobj* gcp = (struct gcobj*)calloc(1, objsize);
  gcp->sz = objsize;
  //if ((heapsize + gcp->sz)/(2<<20) != heapsize/(2<<20)) {
    //fprintf(stderr, "allocating %zu heapsize is now %zu\n", objsize, heapsize);
  //}
  heapsize += gcp->sz;
  struct java_lang_Object* obj = gc2object(gcp);
  gcp->prev = head;
  head = gcp;
  return obj;
}

void rt_addroot(struct java_lang_Object** obj) {
  if (nextroot == rootlimit) {
    fprintf(stderr, "Too many roots\n");
    fflush(stderr);
    abort();
  }
  *(nextroot++) = obj;
}

void rt_pushref(struct java_lang_Object* obj) {
  if (shadowsp == shadowlimit) {
    fprintf(stderr, "Stack overflow\n");
    fflush(stderr);
    abort();
  }
  *(shadowsp++) = object2gc(obj);
  //fprintf(stderr, "Push %p depth=%td\n", obj, (shadowsp-&(shadowstack[0])));
}

void rt_popref(uint32_t n) {
  if (shadowsp-&(shadowstack[0]) < n) {
    fprintf(stderr, "Stack underflow\n");
    fflush(stderr);
    abort();
  }
  shadowsp-=n;
  //fprintf(stderr, "Pop %d depth=%td\n", n, (shadowsp-&(shadowstack[0])));
}

void marksweep() {
  fprintf(stderr, "collecting heapsize = %zu\n", heapsize);
  struct gcobj* workq = NULL;
  for (struct java_lang_Object*** curp = &(staticroots[0]); curp < nextroot; curp++) {
    struct gcobj* cur = object2gc(**curp);
    if (cur == NULL) continue;
    if (!cur->nextwork) {
      if (workq) {
        cur->nextwork = workq;
      } else {
        cur->nextwork = cur;
      }
      workq = cur;
    }
  }
  for (struct gcobj **curp = &(shadowstack[0]); curp < shadowsp; curp++) {
    struct gcobj* cur = *curp;
    if (cur == NULL) continue;
    if (!cur->nextwork) {
      if (workq) {
        cur->nextwork = workq;
      } else {
        cur->nextwork = cur;
      }
      workq = cur;
    }
  }
  while (workq) {
    struct gcobj* cur = workq;
    struct java_lang_Object* obj = gc2object(cur);
    struct klass* k = obj->klass;
    if (k->instsize == 0) {
      struct array* a = (struct array*)obj;
      if (k->elementklass) {
        struct java_lang_Object** data = (struct java_lang_Object**)&(a->data[0]);
        for (size_t i = 0; i < a->length; i++) {
          struct java_lang_Object* p = *(data+i);
          struct gcobj* gcp = object2gc(p);
          if (gcp->nextwork == NULL) {
            gcp->nextwork = workq;
            workq = gcp;
          }
        }
      }
    } else {
      while (k != &class_java_Dlang_DObject) {
        struct klass* sk = k->super;
        struct reference* ps;
        ps = (struct reference*)(((char*)obj)+sk->instsize);
        for (size_t i = 0; i < k->npointers; ++i) {
          struct java_lang_Object* p = (ps+i)->object;
          struct gcobj* gcp = object2gc(p);
          if (gcp->nextwork == NULL) {
            gcp->nextwork = workq;
            workq = gcp;
          }
        }
        k = sk;
      }
    }
    if (workq->nextwork == workq) {
      workq = NULL;
    }
  }
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
  fprintf(stderr, "done collecting, heapsize=%zu\n", heapsize);
}

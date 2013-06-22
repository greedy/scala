#ifndef KLASS_H
#define KLASS_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

struct utf8str {
  int32_t len;
  char *bytes;
};

struct ifaceinfo {
  struct klass *klass;
  void **vtable;
};

struct klass {
  struct utf8str name;
  uint32_t instsize;
  struct klass *super;
  void **vtable;
  struct klass *arrayklass;
  struct klass *elementklass;
  uint32_t npointers;
#define eltsoffset npointers
  uint32_t numiface;
  struct ifaceinfo ifaces[];
};

#endif

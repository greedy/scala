#ifndef KLASS_H
#define KLASS_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

struct ifaceinfo {
  struct klass *klass;
  void **vtable;
};

struct klass {
  char *name;
  uint32_t instsize;
  struct klass *super;
  void **vtable;
  uint32_t numiface;
  struct ifaceinfo ifaces[];
};

#endif

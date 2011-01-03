#include "strings.h"
#include "object.h"
#include "runtime.h"
#include "arrays.h"

#include <string.h>
#include <stdlib.h>
#include <inttypes.h>
#include <stdio.h>
#include <wchar.h>

static void *vtable_java_lang_String[] = {
  method_java_Dlang_DString_Mclone_Rjava_Dlang_DObject,
  method_java_Dlang_DString_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean,
  method_java_Dlang_DObject_Mfinalize_Rscala_DUnit,
  method_java_Dlang_DString_MhashCode_Rscala_DInt,
  method_java_Dlang_DString_MtoString_Rjava_Dlang_DString,
};

struct klass class_java_Dlang_DString = {
  "java.lang.String",
  sizeof(struct java_lang_String),
  &class_java_Dlang_DObject,
  vtable_java_lang_String,
  NULL,
  NULL,
  0,
};

void
method_java_Dlang_DString_M_Linit_G_Ascala_DArray_Rjava_Dlang_DString(
    struct java_lang_String* s,
    struct array* bytes)
{
  method_java_Dlang_DObject_M_Linit_G_Rjava_Dlang_DObject((struct java_lang_Object*)s);
  s->bytes = bytes;
}

struct array*
method_java_Dlang_DString_Mbytes_Rscala_DArray(
    struct java_lang_String* s)
{
  return s->bytes;
}

char*
string_cstring(struct java_lang_String *s)
{
  return (char*)s->bytes->data;
}

void
method__Ojava_Dlang_DString_Mprint_Ajava_Dlang_DString_Rscala_DUnit(
    struct java_lang_Object *module,
    struct java_lang_String *s)
{
  puts(string_cstring(s));
}

struct java_lang_String*
rt_makestring(char *s)
{
  struct java_lang_String *ret = (struct java_lang_String*)rt_new(&class_java_Dlang_DString);
  size_t slen = strlen(s);
  struct array *chars = new_array(BYTE, NULL, 1, slen);
  memcpy(chars->data, s, slen);
  method_java_Dlang_DString_M_Linit_G_Ascala_DArray_Rjava_Dlang_DString(ret, chars);
  return ret;
}

void rt_string_append_Boolean(
    char **s,
    uint8_t v)
{
  char *o = *s;
  char *bs;
  if (v) bs = "true"; else bs = "false";
  if (o) {
    asprintf(s, "%s%s", o, bs);
    free(o);
  } else {
    asprintf(s, "%s", bs);
  }
}

void rt_string_append_Byte(
    char **s,
    int8_t v)
{
  char *o = *s;
  if (o) {
    asprintf(s, "%s" PRIi8, o, v);
    free(o);
  } else {
    asprintf(s, PRIi8, v);
  }
}

void rt_string_append_Short(
    char **s,
    int16_t v)
{
  char *o = *s;
  if (o) {
    asprintf(s, "%s" PRIi16, o, v);
    free(o);
  } else {
    asprintf(s, PRIi16, v);
  }
}

void rt_string_append_Char(
    char **s,
    uint32_t v)
{
  char *o = *s;
  if (o) {
    asprintf(s, "%s" "%lc", o, (wint_t)v);
    free(o);
  } else {
    asprintf(s, "%lc", (wint_t)v);
  }
}

void rt_string_append_Int(
    char **s,
    int32_t v)
{
  char *o = *s;
  if (o) {
    asprintf(s, "%s" PRIi32, o, v);
    free(o);
  } else {
    asprintf(s, PRIi32, v);
  }
}

void rt_string_append_Long(
    char **s,
    int64_t v)
{
  char *o = *s;
  if (o) {
    asprintf(s, "%s" PRIi64, o, v);
    free(o);
  } else {
    asprintf(s, PRIi64, v);
  }
}

void rt_string_append_Float(
    char **s,
    float v)
{
  char *o = *s;
  if (o) {
    asprintf(s, "%s" "%g", o, (double)v);
    free(o);
  } else {
    asprintf(s, "%g", (double)v);
  }
}

void rt_string_append_Double(
    char **s,
    double v)
{
  char *o = *s;
  if (o) {
    asprintf(s, "%s" "%g", o, (double)v);
    free(o);
  } else {
    asprintf(s, "%g", (double)v);
  }
}

typedef struct java_lang_String* (*toStringFn)(struct java_lang_Object*);

void rt_string_append_string(
    char **s,
    struct java_lang_Object *sobj,
    uint32_t toStringMethodNumber)
{
  char *o = *s;
  toStringFn toString;
  char *sobjs;
  toString = sobj->klass->vtable[toStringMethodNumber];
  sobjs = string_cstring(toString(sobj));
  if (o) {
    asprintf(s, "%s" "%s", o, sobjs);
    free(o);
  } else {
    asprintf(s, "%s", sobjs);
  }
}

int32_t method_java_Dlang_DString_MhashCode_Rscala_DInt(struct java_lang_String* self)
{
  return string_cstring(self)[0] + self->bytes->length;
}

bool method_java_Dlang_DString_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(struct java_lang_String* self, struct java_lang_Object* other)
{
  if (other->klass == &class_java_Dlang_DString) {
    return strcmp(string_cstring(self), string_cstring((struct java_lang_String*)other)) == 0;
  } else {
    return false;
  }
}

struct java_lang_Object *method_java_Dlang_DString_Mclone_Rjava_Dlang_DObject(struct java_lang_String* self)
{
  return (struct java_lang_Object*)self;
}

struct java_lang_String *method_java_Dlang_DString_MtoString_Rjava_Dlang_DString(struct java_lang_String* self)
{
  return self;
}

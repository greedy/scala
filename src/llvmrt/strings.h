#ifndef STRINGS_H
#define STRINGS_H

#include "runtime.h"
#include "object.h"

#include "unicode/ustring.h"

extern struct klass class_java_Dlang_DString;
extern struct klass class__Ojava_Dlang_DString;

struct stringlist;


struct java_lang_String {
  struct java_lang_Object super;
  int32_t len;
  UChar *s;
};

struct _Ojava_lang_String {
  struct java_lang_Object super;
};

extern struct java_lang_String* rt_makestring(struct utf8str *s);

extern int32_t method_java_Dlang_DString_MhashCode_Rscala_DInt(struct reference);
extern bool method_java_Dlang_DString_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(struct reference, struct reference);
extern struct reference method_java_Dlang_DString_Mclone_Rjava_Dlang_DObject(struct reference);
extern struct reference method_java_Dlang_DString_MtoString_Rjava_Dlang_DString(struct reference);


void rt_string_append_Boolean(
    struct stringlist **s,
    uint8_t v)
;

void rt_string_append_number(
    struct stringlist **s,
    int32_t v,
    int32_t initsize)
;

void rt_string_append_Byte(
    struct stringlist **s,
    int8_t v)
;

void rt_string_append_Short(
    struct stringlist **s,
    int16_t v)
;

void rt_string_append_Char(
    struct stringlist **s,
    UChar32 v)
;

void rt_string_append_Int(
    struct stringlist **s,
    int32_t v)
;

void rt_string_append_Long(
    struct stringlist **s,
    int64_t v)
;

void rt_string_append_Double(
    struct stringlist **s,
    double v)
;

void rt_string_append_Float(
    struct stringlist **s,
    float v)
;

void rt_string_append_string(
    struct stringlist **s,
    struct java_lang_Object *sobj)
;

void rt_string_append_ustring(
    struct stringlist **s,
    int32_t len,
    UChar *buf);

int32_t method_java_Dlang_DString_MhashCode_Rscala_DInt(struct reference self)
;

bool method_java_Dlang_DString_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(struct reference self, struct reference other)
;

struct reference method_java_Dlang_DString_Mclone_Rjava_Dlang_DObject(struct reference self)
;

struct reference method_java_Dlang_DString_MtoString_Rjava_Dlang_DString(struct reference self)
;

struct java_lang_String *
rt_stringconcat(struct stringlist **s)
;
#endif

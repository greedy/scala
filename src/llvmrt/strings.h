#ifndef STRINGS_H
#define STRINGS_H

#include "object.h"

#include "unicode/ustring.h"

extern struct klass class_java_Dlang_DString;
extern struct klass class_Ojava_Dlang_DString;

struct java_lang_String {
  struct java_lang_Object super;
  int32_t len;
  UChar *s;
};

struct _Ojava_lang_String {
  struct java_lang_Object super;
};

extern struct java_lang_String* rt_makestring(struct utf8str *s);

extern int32_t method_java_Dlang_DString_MhashCode_Rscala_DInt(struct java_lang_String*);
extern bool method_java_Dlang_DString_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean(struct java_lang_String*, struct java_lang_Object*);
extern struct java_lang_Object *method_java_Dlang_DString_Mclone_Rjava_Dlang_DObject(struct java_lang_String*);
extern struct java_lang_String *method_java_Dlang_DString_MtoString_Rjava_Dlang_DString(struct java_lang_String*);

#endif

#include "exceptions.h"

static void *vtable_java_lang_Throwable[] = {
  method_java_Dlang_DObject_MhashCode_Rscala_DInt,
  method_java_Dlang_DObject_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean,
  method_java_Dlang_DObject_Mclone_Rjava_Dlang_DObject,
  method_java_Dlang_DThrowable_MtoString_Rjava_Dlang_DString,
  method_java_Dlang_DObject_Mfinalize_Rscala_DUnit,
  method_java_Dlang_DThrowable_MgetMessage_Rjava_Dlang_DString,
  method_java_Dlang_DThrowable_MgetLocalizedMessage_Rjava_Dlang_DString,
  method_java_Dlang_DThrowable_MgetCause_Rjava_Dlang_DThrowable,
  method_java_Dlang_DThrowable_MinitCause_Ajava_Dlang_DThrowable_Rjava_Dlang_DThrowable,
  method_java_Dlang_DThrowable_MtoString_Rjava_Dlang_DString,
  method_java_Dlang_DThrowable_MprintStackTrace_Rscala_DUnit,
  method_java_Dlang_DThrowable_MprintStackTrace_Ajava_Dio_DPrintStream_Rscala_DUnit,
  method_java_Dlang_DThrowable_MprintStackTrace_Ajava_Dio_DPrintWriter_Rscala_DUnit,
  method_java_Dlang_DThrowable_MfillInStackTrace_Rjava_Dlang_DThrowable,
  method_java_Dlang_DThrowable_MgetStackTrace_Rscala_DArray,
  method_java_Dlang_DThrowable_MsetStackTrace_Ascala_DArray_Rscala_DUnit
};

static void *vtable_java_lang_Exception[] = {
  method_java_Dlang_DObject_MhashCode_Rscala_DInt,
  method_java_Dlang_DObject_Mequals_Ajava_Dlang_DObject_Rscala_DBoolean,
  method_java_Dlang_DObject_Mclone_Rjava_Dlang_DObject,
  method_java_Dlang_DThrowable_MtoString_Rjava_Dlang_DString,
  method_java_Dlang_DObject_Mfinalize_Rscala_DUnit,
  method_java_Dlang_DThrowable_MgetMessage_Rjava_Dlang_DString,
  method_java_Dlang_DThrowable_MgetLocalizedMessage_Rjava_Dlang_DString,
  method_java_Dlang_DThrowable_MgetCause_Rjava_Dlang_DThrowable,
  method_java_Dlang_DThrowable_MinitCause_Ajava_Dlang_DThrowable_Rjava_Dlang_DThrowable,
  method_java_Dlang_DThrowable_MtoString_Rjava_Dlang_DString,
  method_java_Dlang_DThrowable_MprintStackTrace_Rscala_DUnit,
  method_java_Dlang_DThrowable_MprintStackTrace_Ajava_Dio_DPrintStream_Rscala_DUnit,
  method_java_Dlang_DThrowable_MprintStackTrace_Ajava_Dio_DPrintWriter_Rscala_DUnit,
  method_java_Dlang_DThrowable_MfillInStackTrace_Rjava_Dlang_DThrowable,
  method_java_Dlang_DThrowable_MgetStackTrace_Rscala_DArray,
  method_java_Dlang_DThrowable_MsetStackTrace_Ascala_DArray_Rscala_DUnit
};

struct klass class_java_Dlang_DThrowable = {
  "java.lang.Throwable",
  sizeof(struct java_lang_Throwable),
  &class_java_Dlang_DObject,
  vtable_java_lang_Throwable,
  0
};

struct klass class_java_Dlang_DException = {
  "java.lang.Exception",
  sizeof(struct java_lang_Exception),
  &class_java_Dlang_DThrowable,
  vtable_java_lang_Exception,
  0
};

struct java_lang_String;

struct java_lang_String*
method_java_Dlang_DThrowable_MtoString_Rjava_Dlang_DString(
    struct java_lang_Throwable *this)
{
  return NULL;
}

struct java_lang_String*
method_java_Dlang_DThrowable_MgetMessage_Rjava_Dlang_DString(
    struct java_lang_Throwable *this)
{
  return NULL;
}

struct java_lang_String*
method_java_Dlang_DThrowable_MgetLocalizedMessage_Rjava_Dlang_DString(
    struct java_lang_Throwable *this)
{
  return NULL;
}

struct java_lang_Throwable*
method_java_Dlang_DThrowable_MgetCause_Rjava_Dlang_DThrowable(
    struct java_lang_Throwable *this)
{
  return NULL;
}

struct java_lang_Throwable*
method_java_Dlang_DThrowable_MinitCause_Ajava_Dlang_DThrowable_Rjava_Dlang_DThrowable(
    struct java_lang_Throwable *this)
{
  return this;
}

void
method_java_Dlang_DThrowable_MprintStackTrace_Rscala_DUnit(
    struct java_lang_Throwable *this)
{
  return;
}

struct java_io_PrintStream;

void
method_java_Dlang_DThrowable_MprintStackTrace_Ajava_Dio_DPrintStream_Rscala_DUnit(
    struct java_lang_Throwable *this,
    struct java_io_PrintStream *stream)
{
  return;
}

struct java_io_PrintWriter;

void
method_java_Dlang_DThrowable_MprintStackTrace_Ajava_Dio_DPrintWriter_Rscala_DUnit(
    struct java_lang_Throwable *this,
    struct java_io_PrintWriter *writer)
{
  return;
}

struct java_lang_Throwable*
method_java_Dlang_DThrowable_MfillInStackTrace_Rjava_Dlang_DThrowable(
    struct java_lang_Throwable *this)
{
  return this;
}

struct scala_Array*
method_java_Dlang_DThrowable_MgetStackTrace_Rscala_DArray(
    struct java_lang_Throwable *this)
{
  return NULL;
}

void
method_java_Dlang_DThrowable_MsetStackTrace_Ascala_DArray_Rscala_DUnit(
    struct java_lang_Throwable *this)
{
  return;
}

void
method_java_Dlang_DException_M_Linit_G_Rjava_Dlang_DException(
    struct java_lang_Exception *this)
{
  return;
}

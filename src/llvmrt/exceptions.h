#ifndef EXCEPTIONS_H
#define EXCEPTIONS_H

#include "object.h"

extern struct klass class_java_Dlang_DThrowable;
extern struct klass class_java_Dlang_DException;

struct java_lang_Throwable {
  struct java_lang_Object super;
};

struct java_lang_Exception {
  struct java_lang_Throwable super;
};

extern struct java_lang_String*
method_java_Dlang_DThrowable_MtoString_Rjava_Dlang_DString(
    struct java_lang_Throwable *this);

extern struct java_lang_String*
method_java_Dlang_DThrowable_MgetMessage_Rjava_Dlang_DString(
    struct java_lang_Throwable *this);

extern struct java_lang_String*
method_java_Dlang_DThrowable_MgetLocalizedMessage_Rjava_Dlang_DString(
    struct java_lang_Throwable *this);

extern struct java_lang_Throwable*
method_java_Dlang_DThrowable_MgetCause_Rjava_Dlang_DThrowable(
    struct java_lang_Throwable *this);

extern struct java_lang_Throwable*
method_java_Dlang_DThrowable_MinitCause_Ajava_Dlang_DThrowable_Rjava_Dlang_DThrowable(
    struct java_lang_Throwable *this);

extern void
method_java_Dlang_DThrowable_MprintStackTrace_Rscala_DUnit(
    struct java_lang_Throwable *this);

struct java_io_PrintStream;

extern void
method_java_Dlang_DThrowable_MprintStackTrace_Ajava_Dio_DPrintStream_Rscala_DUnit(
    struct java_lang_Throwable *this,
    struct java_io_PrintStream *stream);

struct java_io_PrintWriter;

extern void
method_java_Dlang_DThrowable_MprintStackTrace_Ajava_Dio_DPrintWriter_Rscala_DUnit(
    struct java_lang_Throwable *this,
    struct java_io_PrintWriter *writer);

extern struct java_lang_Throwable*
method_java_Dlang_DThrowable_MfillInStackTrace_Rjava_Dlang_DThrowable(
    struct java_lang_Throwable *this);

struct scala_Array;

extern struct scala_Array*
method_java_Dlang_DThrowable_MgetStackTrace_Rscala_DArray(
    struct java_lang_Throwable *this);

extern void
method_java_Dlang_DThrowable_MsetStackTrace_Ascala_DArray_Rscala_DUnit(
    struct java_lang_Throwable *this);

extern void
method_java_Dlang_DException_M_Linit_G_Rjava_Dlang_DException(
    struct java_lang_Exception *this);

#endif

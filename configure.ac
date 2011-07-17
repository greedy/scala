AC_INIT([scala-llvm],[0])
AC_CONFIG_SRCDIR([src/llvm-ffi/scala/ffi/ctypes.scala.in])

AC_DEFUN([CHECK_INT_TYPE],[
  AC_CHECK_SIZEOF([$1])
  case "x$AS_TR_SH([ac_cv_sizeof_$1])" in
    x1) AC_SUBST(AS_TR_CPP([$1])_SCALA_TYPE, [scala.Byte]) ;;
    x2) AC_SUBST(AS_TR_CPP([$1])_SCALA_TYPE, [scala.Short]) ;;
    x4) AC_SUBST(AS_TR_CPP([$1])_SCALA_TYPE, [scala.Int]) ;;
    x8) AC_SUBST(AS_TR_CPP([$1])_SCALA_TYPE, [scala.Long]) ;;
    *)  AC_MSG_ERROR([could not determine scala type for c type $1]) ;;
  esac
  AC_SUBST(AS_TR_CPP([$1])_LLVM_TYPE, [i$(expr $AS_TR_SH([ac_cv_sizeof_$1]) '*' '8')])
])

AC_DEFUN([CHECK_ALIGNMENT],[
  AC_CHECK_ALIGNOF([$1])
  AC_SUBST(AS_TR_CPP([$1])_ALIGNMENT, $AS_TR_SH([ac_cv_alignof_$1]))
])

CHECK_INT_TYPE([char])
CHECK_INT_TYPE([short])
CHECK_INT_TYPE([int])
CHECK_INT_TYPE([long])
CHECK_INT_TYPE([long long])
CHECK_INT_TYPE([intptr_t])
CHECK_INT_TYPE([intmax_t])
CHECK_INT_TYPE([size_t])

CHECK_ALIGNMENT([int8_t])
CHECK_ALIGNMENT([int16_t])
CHECK_ALIGNMENT([int32_t])
CHECK_ALIGNMENT([int64_t])
CHECK_ALIGNMENT([float])
CHECK_ALIGNMENT([double])

AC_CONFIG_SUBDIRS([src/llvmrt])
AC_CONFIG_FILES([src/llvm-ffi/scala/ffi/ctypes.scala])
AC_OUTPUT

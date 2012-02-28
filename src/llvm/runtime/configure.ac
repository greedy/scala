AC_INIT([scala-llvm-runtime],[0])
AC_CONFIG_SRCDIR([runscala.cpp])

AC_DEFUN([CHECK_LLVM_VERSION],[
  AC_MSG_CHECKING([LLVM version])
  llvmversion=`llvm-config --version`
  if test $? -ne 0 ; then
    AC_MSG_RESULT([not found])
    AC_MSG_ERROR([LLVM not found])
  else
    major=`echo $llvmversion | awk 'BEGIN{FS="."}{print 0+$[]1}'`
    minor=`echo $llvmversion | awk 'BEGIN{FS="."}{print 0+$[]2}'`
    AC_DEFINE_UNQUOTED([LLVM_MAJOR_VERSION], [$major], [Major version of LLVM])
    AC_DEFINE_UNQUOTED([LLVM_MINOR_VERSION], [$minor], [Minor version of LLVM])
    AC_MSG_RESULT([$llvmversion])
  fi
])

CHECK_LLVM_VERSION

AC_CONFIG_HEADERS([config.h])
AC_OUTPUT

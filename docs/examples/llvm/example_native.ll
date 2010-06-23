%"example!module" = type opaque
declare default ccc i32 @printf(i8*, ...)
@fmt = internal constant [4 x i8] c"%g\0A\00"
define default fastcc void @"example!module/printdouble"(%"example!module"* %m,double %d) {
  %fs = getelementptr [4 x i8]* @fmt, i32 0, i32 0
  %1 = call ccc i32 (i8*,...)* @printf(i8* %fs, double %d)
  ret void
}

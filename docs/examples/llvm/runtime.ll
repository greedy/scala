%.ifaceref = type { %.object*, %.vtable }
%.ifaceinfo = type { %.class*, %.vtable }
%.vtable = type i8**
%.class = type { i8*, i32, %.class*, %.vtable, i32, [0 x %.ifaceinfo] }
; array = { i32 size, [0 x elttype] data }
%.array.i1 = type { i32, [0 x i8] }
%.array.i8 = type { i32, [0 x i8] }
%.array.i16 = type { i32, [0 x i16] }
%.array.i32 = type { i32, [0 x i32] }
%.array.i64 = type { i32, [0 x i64] }
%.array.float = type { i32, [0 x float] }
%.array.double = type { i32, [0 x double] }
%.array.object = type { i32, [0 x %.object*] }

%java.lang.Boolean = type { %.object, i1 }
%java.lang.Byte = type { %.object, i8 }
%java.lang.Short = type { %.object, i16 }
%java.lang.Integer = type { %.object, i32 }
%java.lang.Long = type { %.object, i64 }
%java.lang.Float = type { %.object, float }
%java.lang.Double = type { %.object, double }
%java.lang.String = type { %.object, i8* }

@.object.classname = private constant [ 7 x i8 ] c"object\00"
@.boolean.classname = private constant [ 18 x i8] c"java.lang.Boolean\00"
@.byte.classname = private constant [ 15 x i8] c"java.lang.Byte\00"
@.short.classname = private constant [ 16 x i8] c"java.lang.Short\00"
@.integer.classname = private constant [ 18 x i8] c"java.lang.Integer\00"
@.long.classname = private constant [ 15 x i8] c"java.lang.Long\00"
@.float.classname = private constant [ 16 x i8] c"java.lang.Float\00"
@.double.classname = private constant [ 17 x i8] c"java.lang.Double\00"
@.string.classname = private constant [ 17 x i8] c"java.lang.String\00"

@.classinfo.java.lang.Object = constant %.class { 
  i8* getelementptr ([ 7 x i8 ]* @".object.classname", i32 0, i32 0), 
  i32 ptrtoint (%".object"* getelementptr (%".object"* null, i32 1) to i32), 
  %".class"* null, 
  %.vtable null,
  i32 0,
  [ 0 x %.ifaceinfo ] [  ] 
}
@.classinfo.java.lang.Boolean = constant %.class { 
  i8* getelementptr ([ 18 x i8]* @.boolean.classname, i32 0, i32 0), 
  i32 ptrtoint (%java.lang.Boolean* getelementptr (%java.lang.Boolean* null, i32 1) to i32), 
  %.class* @.classinfo.java.lang.Object, 
  %.vtable null,
  i32 0,
  [ 0 x %.ifaceinfo ] [  ] 
}
@.classinfo.java.lang.Byte = constant %.class { 
  i8* getelementptr ([ 15 x i8]* @.byte.classname, i32 0, i32 0), 
  i32 ptrtoint (%java.lang.Byte* getelementptr (%java.lang.Byte* null, i32 1) to i32), 
  %.class* @.classinfo.java.lang.Object, 
  %.vtable null,
  i32 0,
  [ 0 x %.ifaceinfo ] [  ] 
}
@.classinfo.java.lang.Short = constant %.class { 
  i8* getelementptr ([ 16 x i8]* @.short.classname, i32 0, i32 0), 
  i32 ptrtoint (%java.lang.Short* getelementptr (%java.lang.Short* null, i32 1) to i32), 
  %.class* @.classinfo.java.lang.Object, 
  %.vtable null,
  i32 0,
  [ 0 x %.ifaceinfo ] [  ] 
}
@.classinfo.java.lang.Integer = constant %.class {
  i8* getelementptr ([ 18 x i8]* @.integer.classname, i32 0, i32 0),
  i32 ptrtoint (%java.lang.Integer* getelementptr (%java.lang.Integer* null, i32 1) to i32),
  %.class* @.classinfo.java.lang.Object,
  %.vtable null,
  i32 0,
  [ 0 x %.ifaceinfo ] [  ] 
}
@.classinfo.java.lang.Long = constant %.class {
  i8* getelementptr ([ 15 x i8]* @.long.classname, i32 0, i32 0),
  i32 ptrtoint (%java.lang.Long* getelementptr (%java.lang.Long* null, i32 1) to i32),
  %.class* @.classinfo.java.lang.Object,
  %.vtable null,
  i32 0,
  [ 0 x %.ifaceinfo ] [  ] 
}
@.classinfo.java.lang.Float = constant %.class {
  i8* getelementptr ([ 16 x i8]* @.float.classname, i32 0, i32 0),
  i32 ptrtoint (%java.lang.Float* getelementptr (%java.lang.Float* null, i32 1) to i32),
  %.class* @.classinfo.java.lang.Object,
  %.vtable null,
  i32 0,
  [ 0 x %.ifaceinfo ] [  ] 
}
@.classinfo.java.lang.Double = constant %.class {
  i8* getelementptr ([ 17 x i8]* @.double.classname, i32 0, i32 0),
  i32 ptrtoint (%java.lang.Double* getelementptr (%java.lang.Double* null, i32 1) to i32),
  %.class* @.classinfo.java.lang.Object,
  %.vtable null,
  i32 0,
  [ 0 x %.ifaceinfo ] [  ] 
}
@.classinfo.java.lang.String = constant %.class {
  i8* getelementptr ([ 17 x i8]* @.string.classname, i32 0, i32 0),
  i32 ptrtoint (%java.lang.String* getelementptr (%java.lang.String* null, i32 1) to i32),
  %.class* @.classinfo.java.lang.Object,
  %.vtable null,
  i32 0,
  [ 0 x %.ifaceinfo ] [  ] 
}

%.object = type { %.class* }
%java.lang.Object = type %.object

declare void @llvm.memset.i32(i8*, i8, i32, i32)

define fastcc %.object* @.rt.new(%.class* %clsp) {
  %cls = load %.class* %clsp
  %objectsize = extractvalue %.class %cls, 1
  %memory = malloc i8, i32 %objectsize
  call void @llvm.memset.i32(i8* %memory, i8 0, i32 %objectsize, i32 0)
  %object = bitcast i8* %memory to %java.lang.Object*
  %initted = call fastcc %.object* @.rt.initobj(%java.lang.Object* %object, %.class* %clsp)
  ret %java.lang.Object* %initted
}

define fastcc %.object* @.rt.initobj(%java.lang.Object* %object, %.class* %clsp) {
  %cls = load %.class* %clsp
  %classp = getelementptr %java.lang.Object* %object, i32 0, i32 0
  store %.class* %clsp, %.class** %classp
  ret %java.lang.Object* %object
}

define fastcc void @.rt.delete(%.object* %object) {
  %memory = bitcast %java.lang.Object* %object to i8*
  free i8* %memory
  ret void
}

define fastcc %.class* @.rt.get_class(%.object* %object) {
  %classpp = getelementptr %java.lang.Object* %object, i32 0, i32 0
  %classp = load %.class** %classpp
  ret %.class* %classp
}

define fastcc void @"java.lang.Object/<init>()"(%java.lang.Object* %object) {
  ret void
}

@false = constant %java.lang.Boolean { %.object { %.class* @.classinfo.java.lang.Boolean }, i1 false }
@true = constant %java.lang.Boolean { %.object { %.class* @.classinfo.java.lang.Boolean }, i1 true }

define  default fastcc %"java.lang.Boolean"* @".rt.box.i1"(i1 %v) {
  %1 = select i1 %v, %java.lang.Boolean* @false, %java.lang.Boolean* @true
  ret %java.lang.Boolean* %1
}
define  default fastcc %"java.lang.Byte"* @".rt.box.i8"(i8 %v) {
  %1 = insertvalue %java.lang.Byte { %.object { %.class* @.classinfo.java.lang.Byte }, i8 undef }, i8 %v, 1
  %2 = malloc %java.lang.Byte
  store %java.lang.Byte %1, %java.lang.Byte* %2
  ret %java.lang.Byte* %2
}
define  default fastcc %"java.lang.Short"* @".rt.box.i16"(i16 %v) {
  %1 = insertvalue %java.lang.Short { %.object { %.class* @.classinfo.java.lang.Short }, i16 undef }, i16 %v, 1
  %2 = malloc %java.lang.Short
  store %java.lang.Short %1, %java.lang.Short* %2
  ret %java.lang.Short* %2
}
define  default fastcc %"java.lang.Integer"* @".rt.box.i32"(i32 %v) {
  %1 = insertvalue %java.lang.Integer { %.object { %.class* @.classinfo.java.lang.Integer }, i32 undef }, i32 %v, 1
  %2 = malloc %java.lang.Integer
  store %java.lang.Integer %1, %java.lang.Integer* %2
  ret %java.lang.Integer* %2
}
define  default fastcc %"java.lang.Long"* @".rt.box.i64"(i64 %v) {
  %1 = insertvalue %java.lang.Long { %.object { %.class* @.classinfo.java.lang.Long }, i64 undef }, i64 %v, 1
  %2 = malloc %java.lang.Long
  store %java.lang.Long %1, %java.lang.Long* %2
  ret %java.lang.Long* %2
}
define  default fastcc %"java.lang.Float"* @".rt.box.float"(float %v) {
  %1 = insertvalue %java.lang.Float { %.object { %.class* @.classinfo.java.lang.Float }, float undef }, float %v, 1
  %2 = malloc %java.lang.Float
  store %java.lang.Float %1, %java.lang.Float* %2
  ret %java.lang.Float* %2
}
define  default fastcc %"java.lang.Double"* @".rt.box.double"(double %v) {
  %1 = insertvalue %java.lang.Double { %.object { %.class* @.classinfo.java.lang.Double }, double undef }, double %v, 1
  %2 = malloc %java.lang.Double
  store %java.lang.Double %1, %java.lang.Double* %2
  ret %java.lang.Double* %2
}
define  default fastcc %java.lang.String* @".rt.makestring"(i8* %s) {
  %1 = insertvalue %java.lang.String { %.object { %.class* @.classinfo.java.lang.String }, i8* undef }, i8* %s, 1
  %2 = malloc %java.lang.String
  store %java.lang.String %1, %java.lang.String* %2
  ret %java.lang.String* %2
}
define  default fastcc i1 @".rt.unbox.i1"(%"java.lang.Boolean"* %v) {
  %1 = getelementptr %java.lang.Boolean* %v, i32 0, i32 1
  %2 = load i1* %1
  ret i1 %2
}
define  default fastcc i8 @".rt.unbox.i8"(%"java.lang.Byte"* %v) {
  %1 = getelementptr %java.lang.Byte* %v, i32 0, i32 1
  %2 = load i8* %1
  ret i8 %2
}
define  default fastcc i16 @".rt.unbox.i16"(%"java.lang.Short"* %v) {
  %1 = getelementptr %java.lang.Short* %v, i32 0, i32 1
  %2 = load i16* %1
  ret i16 %2
}
define  default fastcc i32 @".rt.unbox.i32"(%"java.lang.Integer"* %v) {
  %1 = getelementptr %java.lang.Integer* %v, i32 0, i32 1
  %2 = load i32* %1
  ret i32 %2
}
define  default fastcc i64 @".rt.unbox.i64"(%"java.lang.Long"* %v) {
  %1 = getelementptr %java.lang.Long* %v, i32 0, i32 1
  %2 = load i64* %1
  ret i64 %2
}
define  default fastcc float @".rt.unbox.float"(%"java.lang.Float"* %v) {
  %1 = getelementptr %java.lang.Float* %v, i32 0, i32 1
  %2 = load float* %1
  ret float %2
}
define  default fastcc double @".rt.unbox.double"(%"java.lang.Double"* %v) {
  %1 = getelementptr %java.lang.Double* %v, i32 0, i32 1
  %2 = load double* %1
  ret double %2
}

define default fastcc %.vtable @.rt.iface.vtable(%.class* %cls, %.class* %iface) {
start:
  %np = getelementptr %.class* %cls, i32 0, i32 4
  %ifacesp = getelementptr %.class* %cls, i32 0, i32 5
  %n = load i32* %np
  br label %search
search:
  %i = phi i32 [ 0, %start ], [ %i.next, %cont ]
  %oob = icmp uge i32 %i, %n
  br i1 %oob, label %notfound, label %cont
cont:
  %iip = getelementptr [0 x %.ifaceinfo]* %ifacesp, i32 0, i32 %i
  %ii = load %.ifaceinfo* %iip
  %iclass = extractvalue %.ifaceinfo %ii, 0
  %rightclass = icmp eq %.class* %iclass, %iface
  %i.next = add i32 %i, 1
  br i1 %rightclass, label %found, label %search
found:
  %vtbl = extractvalue %.ifaceinfo %ii, 1
  ret %.vtable %vtbl
notfound:
  ret %.vtable null
}

define default fastcc i1 @.rt.isinstance.iface(%.object* %obj, %.class* %iface) {
  %cls = call fastcc %.class* @.rt.get_class(%.object* %obj)
  %vtbl = call fastcc %.vtable @.rt.iface.vtable(%.class* %cls, %.class* %iface)
  %vtbl.isnotnull = icmp ne %.vtable %vtbl, null
  ret i1 %vtbl.isnotnull
}

define default fastcc i1 @.rt.isinstance.class(%.object* %obj, %.class* %cls) {
start:
  %oclass = call fastcc %.class* @.rt.get_class(%.object* %obj)
  br label %maybe
maybe:
  %curclass = phi %.class* [ %oclass, %start ], [ %nextclass, %cont ]
  %curclass.isnull = icmp eq %.class* %curclass, null
  br i1 %curclass.isnull, label %no, label %cont
cont:
  %nextclassp = getelementptr %.class* %curclass, i32 0, i32 2
  %nextclass = load %.class** %nextclassp
  %ismatch = icmp eq %.class* %curclass, %cls
  br i1 %ismatch, label %yes, label %maybe
no:
  ret i1 false
yes:
  ret i1 true
}

define default fastcc %.ifaceref @.rt.iface.cast(%.object* %obj, %.class* %iface) {
start:
  %cls = call fastcc %.class* @.rt.get_class(%.object* %obj)
  %vtbl = call fastcc %.vtable @.rt.iface.vtable(%.class* %cls, %.class* %iface)
  %vtbl.isnull = icmp eq %.vtable %vtbl, null
  br i1 %vtbl.isnull, label %bad, label %good
good:
  %iref.temp = insertvalue %.ifaceref undef, %.object* %obj, 0
  %iref = insertvalue %.ifaceref %iref.temp, %.vtable %vtbl, 1
  ret %.ifaceref %iref
bad:
  unwind
}

define default fastcc i32 @"java.lang.Object/hashCode()"(%".object"*) { unreachable }
define default fastcc %".object"* @"java.lang.Object/clone()"(%".object"*) { unreachable }
define default fastcc i1 @"java.lang.Object/equals(java.lang.Object)"(%".object"*,%".object"*) { unreachable }
define default fastcc void @"java.lang.Object/finalize()"(%".object"*) { unreachable }
define default fastcc %"java.lang.String"* @"java.lang.Object/toString()"(%".object"*) { unreachable }

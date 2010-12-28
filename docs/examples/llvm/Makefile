examplec.ll example\#module.ll Magic.ll Magic$$class\#module.ll: example.scala
	../../../build/quick/bin/scalac -target:llvm -Xmainclass example example.scala

%.bc: %.ll
	llvm-as $(subst $$,\$$,$<)

a.out a.out.bc: examplec.bc runtime.bc example\#module.bc Magic.bc Magic$$class\#module.bc E.bc
	llvm-ld $(subst $$,\$$,$^)

a.out.ll: a.out.bc
	llvm-dis $<

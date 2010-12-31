examplec.ll example\#module.ll Magic.ll Magic$$class\#module.ll: example.scala
	../../../build/quick/bin/scalac -target:llvm -Xmainclass example example.scala

classes: example.scala
	mkdir -p classes
	../../../build/quick/bin/scalac -d classes example.scala

%.bc: %.ll
	llvm-as $(subst $$,\$$,$<)

a.out a.out.bc: examplec.bc defs.bc example\#module.bc Magic.bc Magic$$class\#module.bc E.bc
	llvm-ld $(subst $$,\$$,$^)

native: examplec.bc runtime.bc example\#module.bc Magic.bc Magic$$class\#module.bc E.bc
	llvm-ld -v -native -o native $(subst $$,\$$,$^)

a.out.ll: a.out.bc
	llvm-dis $<

force:
	touch example.scala
	$(MAKE) a.out

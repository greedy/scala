SOURCES=@lib.sources
FFISOURCES=@ffi.sources

run-sample:
	make -C ../../../src/llvmrt llvmrt.a runscala
	rm -rf irfiles/example irfiles/example.stamp
	rm -rf bin/example.bc bin/example.aot
	make irfiles/example.stamp bin/example.bc
	../../../src/llvmrt/runscala bin/example.bc example

run-sample-jvm:
	make classes
	../../../build/quick/bin/scala -cp classes example

irfiles/%.stamp irfiles/%/*.ll: %.scala runtime.scala
	mkdir -p irfiles/$*
	JAVA_OPTS=-Xmx500M ../../../build/quick/bin/scalac -no-specialization -d irfiles/$* -target:llvm $*.scala apr.scala runtime.scala @base.sources $(FFISOURCES) # $(SOURCES)
	touch irfiles/$*.stamp

irfiles/separate2.stamp: separate1.scala separate2.scala runtime.scala
	mkdir -p irfiles/separate2
	JAVA_OPTS=-Xmx500M ../../../build/quick/bin/scalac -no-specialization -cp irfiles/separate2 -d irfiles/separate2 -target:llvm separate1.scala runtime.scala @base.sources $(FFISOURCES)
	JAVA_OPTS=-Xmx500M ../../../build/quick/bin/scalac -no-specialization -cp irfiles/separate2 -d irfiles/separate2 -target:llvm separate2.scala runtime.scala @base.sources $(FFISOURCES)
	touch irfiles/separate2.stamp

bin/%.bc: irfiles/%.stamp
	mkdir -p `dirname $@`
	for x in irfiles/$*/*.ll ; do \
	  llvm-as $$x; \
	done
	llvm-ld -o $@ -r -disable-internalize irfiles/$*/*.bc ../../../src/llvmrt/llvmrt.a
	opt -O3 -o bin/$*.opt.bc $@
	mv bin/$*.opt.bc $@

bin/%.aot: bin/%.bc
	../../../src/llvmrt/linkscala $< `basename $*`
	llvm-ld -v -native -o $@ b.out.bc ../../../src/llvmrt/llvmrt.a -lapr-1 -L/usr/lib64 `icu-config --ldflags-libsonly ` ../../../src/llvmrt/unwind.o
	rm -f b.out.bc

classes: example.scala
	mkdir -p classes
	../../../build/quick/bin/scalac -d classes example.scala

a.out.ll: a.out.bc
	llvm-dis $<

%.bc: %.ll
	llvm-as -o $@ $<

force:
	touch example.scala
	$(MAKE) a.out

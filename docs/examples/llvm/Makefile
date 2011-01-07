
run-sample:
	make -C ../../../src/llvmrt llvmrt.a runscala
	make clean
	make code.bc; make code.bc
	../../../src/llvmrt/runscala code.bc example

run-sample-jvm:
	make classes
	../../../build/quick/bin/scala -cp classes example

code.bc: irfiles.stamp $(patsubst %.ll,%.bc,$(wildcard irfiles/*.ll))
	llvm-ld -o $@ -r -disable-internalize irfiles/*.bc ../../../src/llvmrt/llvmrt.a

irfiles.stamp irfiles/*.ll: example.scala runtime.scala
	mkdir -p irfiles
	JAVA_OPTS=-Xmx1500M ../../../build/quick/bin/scalac -d irfiles -deprecation -target:llvm example.scala runtime.scala # @lib.sources
	touch irfiles.stamp

classes: example.scala
	mkdir -p classes
	../../../build/quick/bin/scalac -d classes example.scala

%.bc: %.ll
	llvm-as $<

%.ll: %.bc
	llvm-dis $<

a.out.ll: a.out.bc
	llvm-dis $<

force:
	touch example.scala
	$(MAKE) a.out

clean:
	rm -rf irfiles code.bc irfiles.stamp

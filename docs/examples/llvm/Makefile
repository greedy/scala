SOURCES=@lib.sources
FFISOURCES=@ffi.sources

SCALAC=JAVA_OPTS=-Xmx500M ../../../build/quick/bin/scalac
SLFLAGS=-no-specialization -target:llvm

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
	$(SCALAC) $(SLFLAGS) -d irfiles/$* $*.scala apr.scala runtime.scala @base.sources $(FFISOURCES) # $(SOURCES)
	touch irfiles/$*.stamp

## To test separate compilation, `make bin/separate2.bc`. Works to
## build @ffi.sources and @base.sources in advance, but not
## runtime.scala; see next target.
SEP2FLAGS=-cp irfiles/separate2 -d irfiles/separate2
irfiles/separate2.stamp: separate1.scala separate2.scala runtime.scala
	mkdir -p irfiles/separate2
	$(SCALAC) $(SLFLAGS) $(SEP2FLAGS) runtime.scala $(FFISOURCES)
	$(SCALAC) $(SLFLAGS) $(SEP2FLAGS) runtime.scala @base.sources
	$(SCALAC) $(SLFLAGS) $(SEP2FLAGS) runtime.scala separate1.scala
	$(SCALAC) $(SLFLAGS) $(SEP2FLAGS) runtime.scala separate2.scala
	touch irfiles/separate2.stamp

## Compiling even runtime.scala separately -- requires boot class
## manipulation to compile, and even then doesn't run properly. Try
## make bin/separate3.bc
BOOTCP=/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/rt.jar
SEP3FLAGS=-Dsun.boot.class.path=irfiles/separate3:$(BOOTCP) -d irfiles/separate3
irfiles/separate3.stamp: separate1.scala separate2.scala runtime.scala
	mkdir -p irfiles/separate3
	$(SCALAC) $(SLFLAGS) $(SEP3FLAGS) $(FFISOURCES)
	$(SCALAC) $(SLFLAGS) $(SEP3FLAGS) runtime.scala
	$(SCALAC) $(SLFLAGS) $(SEP3FLAGS) @base.sources
	$(SCALAC) $(SLFLAGS) $(SEP3FLAGS) separate1.scala
	$(SCALAC) $(SLFLAGS) $(SEP3FLAGS) separate2.scala
	touch irfiles/separate3.stamp

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
	$(SCALAC) -d classes example.scala

a.out.ll: a.out.bc
	llvm-dis $<

%.bc: %.ll
	llvm-as -o $@ $<

force:
	touch example.scala
	$(MAKE) a.out

clean:
	rm -rf irfiles/* bin/*.bc bin/*.aot

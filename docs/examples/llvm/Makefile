SOURCELISTS = lib.sources ffi.sources

SCALAC=JAVA_OPTS=-Xmx500M ../../../build/quick/bin/scalac
SLFLAGS=-no-specialization -target:llvm

FORCE:

run-sample:
	make -C ../../../src/llvm/runtime llvmrt.a runscala
	rm -rf irfiles/example irfiles/example.stamp
	rm -rf bin/example.bc bin/example.aot
	make irfiles/example.stamp bin/example.bc
	../../../src/llvm/runtime/runscala bin/example.bc example

run-sample-jvm:
	make classes/example.stamp
	../../../build/quick/bin/scala -cp classes/example example

lib.sources: FORCE
	find minlib -name '*.scala' | sort > lib.sources.tmp
	if cmp -s lib.sources lib.sources.tmp ; then : ; else cp lib.sources.tmp lib.sources ; fi
	rm lib.sources.tmp

irfiles/%.stamp: %.scala runtime.scala $(SOURCELISTS)
	mkdir -p irfiles/$*
	$(SCALAC) $(SLFLAGS) -sourcepath minlib -d irfiles/$* $(filter %.scala,$^) $(patsubst %,@%,$(filter %.sources,$^))
	touch $@

.PRECIOUS: irfiles/%.stamp

irfiles/example.stamp: apr.scala

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

bin/%.noopt.bc: irfiles/%.stamp ../../../src/llvm/runtime/llvmrt.a
	mkdir -p `dirname $@`
	for x in irfiles/$*/*.ll ; do \
	  llvm-as $$x || exit 1; \
	done
	llvm-ld -disable-opt -o bin/$*.noopt.bc -r -disable-internalize irfiles/$*/*.bc ../../../src/llvm/runtime/llvmrt.a

.PRECIOUS: bin/%.noopt.bc

%.opt.bc: %.noopt.bc
	opt -O3 -o $@ $<

.PRECIOUS: %.opt.bc

bin/%.aot: bin/%.opt.bc
	../../../src/llvm/runtime/linkscala $< `basename $*`
	llvm-ld -v -native -o $@ b.out.bc ../../../src/llvm/runtime/llvmrt.a -lapr-1 -L/usr/lib64 `icu-config --ldflags-libsonly --ldflags-searchpath` -Xlinker=-undefined -Xlinker=dynamic_lookup ../../../src/llvm/runtime/unwind.o
	rm -f b.out.bc

classes/%.stamp: %.scala
	mkdir -p classes/$*
	$(SCALAC) -d classes/$* $*.scala
	touch classes/$*.stamp

a.out.ll: a.out.bc
	llvm-dis $<

%.bc: %.ll
	llvm-as -o $@ $<

force:
	touch example.scala
	$(MAKE) a.out

clean:
	rm -rf irfiles/* bin/*.bc bin/*.aot

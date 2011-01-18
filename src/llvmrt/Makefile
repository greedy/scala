COMPONENTS = core jit bitreader native interpreter archive
WARNINGS = -Wall
CPPFLAGS = `icu-config --cppflags`
CFLAGS = -g $(WARNINGS) -std=c99 `llvm-config --cflags $(COMPONENTS)`
CXXFLAGS = -g $(WARNINGS) `llvm-config --cxxflags $(COMPONENTS)`
LDFLAGS = `icu-config --ldflags --ldflags-icuio` `llvm-config --ldflags --libs $(COMPONENTS)` -all_load

RTOBJECTS = runtime.bc object.bc boxes.bc arrays.bc strings.bc fp.bc

%.bc: %.c
	clang -std=c99 `icu-config --cppflags` -O4 -emit-llvm -c -o $@ $^

llvmrt.a: $(RTOBJECTS)
	llvm-ar cr $@ $^

llvmrt_native.a: $(patsubst %.bc,%.o,$(RTOBJECTS))
	ar cr $@ $^

runscala: runscala.o unwind.o
	$(CXX) $(LDFLAGS) -o $@ $^

clean:
	rm -f *.o *.bc llvmrt.a runscala

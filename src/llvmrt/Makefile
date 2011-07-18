COMPONENTS = core jit bitreader native interpreter archive bitwriter
WARNINGS = -Wall
CPPFLAGS = `icu-config --cppflags`
CFLAGS = -g $(WARNINGS) -std=c99 `llvm-config --cflags $(COMPONENTS)`
CXXFLAGS = -g $(WARNINGS) `llvm-config --cxxflags $(COMPONENTS)`
LDFLAGS = `icu-config --ldflags-searchpath --ldflags-icuio` `llvm-config --ldflags $(COMPONENTS)` `apr-1-config --link-ld --libs` -all_load
LDLIBS = `icu-config --ldflags-libsonly --ldflags-icuio` `llvm-config --libs $(COMPONENTS)`

RTOBJECTS = runtime.bc object.bc boxes.bc arrays.bc strings.bc fp.bc io.bc

%.bc: %.c
	clang -std=c99 `icu-config --cppflags` -O4 -emit-llvm -c -o $*.sret.bc $^
	opt -sretpromotion -o $@ $*.sret.bc
	rm -f $*.sret.bc

llvmrt.a: $(RTOBJECTS)
	llvm-ar cr $@ $^

llvmrt_native.a: $(patsubst %.bc,%.o,$(RTOBJECTS))
	ar cr $@ $^

runscala: runscala.o wrapper.o unwind.o
	$(CXX) $(LDFLAGS) -o $@ $^ $(LDLIBS)

linkscala: linkscala.o wrapper.o
	$(CXX) $(LDFLAGS) -o $@ $^ $(LDLIBS)

clean:
	rm -f *.o *.bc llvmrt.a runscala

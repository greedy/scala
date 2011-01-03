COMPONENTS = core jit bitreader native interpreter archive
WARNINGS = -Wall
CFLAGS = $(WARNINGS) `llvm-config --cflags $(COMPONENTS)`
CXXFLAGS = $(WARNINGS) `llvm-config --cxxflags $(COMPONENTS)`
LDFLAGS = `llvm-config --ldflags --libs $(COMPONENTS)` -all_load

RTOBJECTS = runtime.bc object.bc boxes.bc arrays.bc strings.bc

%.bc: %.c
	clang -std=c99 -O4 -emit-llvm -c -o $@ $^

llvmrt.a: $(RTOBJECTS)
	llvm-ar cr $@ $^

runscala: runscala.o unwind.o
	$(CXX) $(LDFLAGS) -o $@ $^

clean:
	rm -f *.o *.bc llvmrt.a runscala

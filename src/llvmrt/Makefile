WARNINGS = -Wall
CFLAGS = $(WARNINGS) `llvm-config --cflags`
CXXFLAGS = $(WARNINGS) `llvm-config --cxxflags`
LDFLAGS = `llvm-config --ldflags --libs core jit bitreader native interpreter` -all_load

RTOBJECTS = runtime.o object.o boxes.o unwind.o exceptions.o serializable.o

llvmrt.a: $(RTOBJECTS)
	ar cr $@ $^

runscala: runscala.o llvmrt.a
	$(CXX) $(LDFLAGS) -o $@ $^

clean:
	rm -f *.o llvmrt.a runscala

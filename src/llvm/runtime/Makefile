COMPONENTS = core jit bitreader native interpreter archive bitwriter
WARNINGS = -Wall
CPPFLAGS = `icu-config --cppflags`
CFLAGS = -g $(WARNINGS) -std=c99 -fexceptions `llvm-config --cflags $(COMPONENTS)`
CXXFLAGS = -g $(WARNINGS) -fexceptions `llvm-config --cxxflags $(COMPONENTS)`
LDFLAGS = -g `icu-config --ldflags-searchpath --ldflags-icuio` `llvm-config --ldflags $(COMPONENTS)` `apr-1-config --link-ld --libs`
LDLIBS = `icu-config --ldflags-libsonly --ldflags-icuio` `llvm-config --libs $(COMPONENTS)` -lm

RTSOURCES = runtime.c object.c boxes.c arrays.c strings.c fp.c io.c gc.c
RTOBJECTS = $(patsubst %.c,%.bc,$(RTSOURCES))
RTDEPFILES = $(patsubst %.c,%.d,$(RTSOURCES))

all: llvmrt.a runscala

%.bc: %.c
	clang -std=c99 -fexceptions `icu-config --cppflags` -O4 -emit-llvm -c -o $@ $<

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

%.d: %.c
	@set -e; rm -f $@; \
	 $(CC) -M $(CPPFLAGS) $< > $@.$$$$; \
	 sed -e 's,\($*\)\.o[ :]*,\1.o \1.bc $@ : ,g' < $@.$$$$ > $@; \
	 rm -f $@.$$$$

include $(RTDEPFILES)

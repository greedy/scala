examplec.ll example\#module.ll Magic.ll: example.scala
	../../../build/quick/bin/scalac -target:llvm -Xmainclass example example.scala

%.bc: %.ll
	llvm-as $<

a.out a.out.bc: examplec.bc runtime.bc example\#module.bc Magic.bc
	llvm-ld $^

a.out.ll: a.out.bc
	llvm-dis $<

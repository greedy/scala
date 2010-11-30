%.bc: %.ll
	llvm-as $<

a.out: examplec.bc runtime.bc example\#module.bc Magic.bc
	llvm-ld $^

#include "llvm/LLVMContext.h"
#include "llvm/Target/TargetData.h"
#include "llvm/Module.h"
#include "llvm/Type.h"
#include "llvm/ADT/Triple.h"
#include "llvm/Bitcode/ReaderWriter.h"
#include "llvm/CodeGen/LinkAllCodegenComponents.h"
#include "llvm/ExecutionEngine/GenericValue.h"
#include "llvm/ExecutionEngine/Interpreter.h"
#include "llvm/ExecutionEngine/JIT.h"
#include "llvm/ExecutionEngine/JITEventListener.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/PassManager.h"
#include "llvm/Support/ManagedStatic.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/PluginLoader.h"
#include "llvm/Support/PrettyStackTrace.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/System/Process.h"
#include "llvm/System/Signals.h"
#include "llvm/Target/TargetSelect.h"
#include "llvm/Target/TargetOptions.h"
#include <cerrno>

using namespace llvm;

static ExecutionEngine *EE = 0;

static void do_shutdown() {
  delete EE;
  llvm_shutdown();
}

int main(int argc, char *argv[], char * const *envp)
{
  sys::PrintStackTraceOnErrorSignal();
  llvm::JITExceptionHandling = true;
  InitializeNativeTarget();
  LLVMContext &Context = getGlobalContext();
  atexit(do_shutdown);
  cl::ParseCommandLineOptions(argc, argv, "scala runner");
  std::string ErrorMsg;
  Module *Mod = NULL;
  if (MemoryBuffer *Buffer = MemoryBuffer::getFileOrSTDIN("a.out.bc",&ErrorMsg)) {
    Mod = getLazyBitcodeModule(Buffer, Context, &ErrorMsg);
    if (!Mod) delete Buffer;
  }
  if (!Mod) {
    errs() << argv[0] << ": error loading program '" << "a.out.bc" << "': "
           << ErrorMsg << "\n";
    exit(1);
  }

  EngineBuilder builder(Mod);
  builder.setErrorStr(&ErrorMsg);
  builder.setEngineKind(EngineKind::JIT);
  builder.setOptLevel(CodeGenOpt::None);

  EE = builder.create();
  if (!EE) {
    if (!ErrorMsg.empty())
      errs() << argv[0] << ": error creating EE: " << ErrorMsg << "\n";
    else
      errs() << argv[0] << ": unknown error creating EE!\n";
    exit(1);
  }

  Function *EntryFn = Mod->getFunction("main");
  if (!EntryFn) {
    errs() << '\'' << "main" << "\' function not found in module.\n";
    return -1;
  }

  // Run static constructors.
  EE->runStaticConstructorsDestructors(false);

  EE->runFunctionAsMain(EntryFn, std::vector<std::string>(), envp);

  EE->runStaticConstructorsDestructors(true);

  exit(0);
}

extern "C" {
  void printi32(int32_t n) {
    fprintf(stderr,"%d\n", n);
    fflush(stderr);
  }
}

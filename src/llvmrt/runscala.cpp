#include "llvm/Bitcode/Archive.h"
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

extern "C" {
#include "object.h"
#include "klass.h"
}

using namespace llvm;

static ExecutionEngine *EE = 0;

static void do_shutdown() {
  delete EE;
  llvm_shutdown();
}

std::string encodeName(const std::string &s)
{
  std::string res;
  std::string::const_iterator it;
  for (it = s.begin(); it < s.end(); it++) {
    switch (*it) {
      case '_':
        res += "__";
        break;
      case '.':
        res += "_D";
        break;
      case '<':
        res += "_L";
        break;
      case '>':
        res += "_G";
        break;
      default:
        res += *it;
        break;
    }
  }
  return res;
}

int main(int argc, char *argv[], char * const *envp)
{
  sys::PrintStackTraceOnErrorSignal();
  llvm::JITExceptionHandling = true;
  InitializeNativeTarget();
  LLVMContext &Context = getGlobalContext();
  atexit(do_shutdown);
  //cl::ParseCommandLineOptions(argc, argv, "scala runner");
  std::string ErrorMsg;

  Module *Mod = NULL;
  if (MemoryBuffer *Buffer = MemoryBuffer::getFileOrSTDIN(argv[1],&ErrorMsg)) {
    Mod = getLazyBitcodeModule(Buffer, Context, &ErrorMsg);
    if (!Mod) delete Buffer;
  }
  if (!Mod) {
    errs() << argv[0] << ": error loading program '" << argv[1] << "': "
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

  std::string modid(argv[2]);

  std::string modulename;
  modulename += "module__O";
  modulename += encodeName(modid);

  std::string moduleinitfnname;
  moduleinitfnname += "initmodule_module__O";
  moduleinitfnname += encodeName(modid);

  std::string mainfnname;
  mainfnname += "method__O";
  mainfnname += encodeName(modid);
  mainfnname += "_Mmain_Rscala_DUnit";

  Function *EntryFn = Mod->getFunction(mainfnname);
  if (!EntryFn) {
    errs() << '\'' << mainfnname << "\' function not found in module.\n";
    return -1;
  }

  Function *InitFn = Mod->getFunction(moduleinitfnname);

  if (!InitFn) {
    errs() << modid << " module initializer not found.\n";
    return -1;
  }

  GlobalVariable *ModuleInstance = Mod->getNamedGlobal(modulename);

  if (!ModuleInstance) {
    errs() << modid << " module instance not found.\n";
    return -1;
  }

  // Run static constructors.
  EE->runStaticConstructorsDestructors(false);

  std::vector<GenericValue> args;

  EE->runFunction(InitFn, args);

  args.clear();
  args.push_back(PTOGV(EE->getPointerToGlobal(ModuleInstance)));
  EE->runFunction(EntryFn, args);

  EE->runStaticConstructorsDestructors(true);

  exit(0);
}

extern "C" {
  void printi32(int32_t n) {
    fprintf(stderr,"%d\n", n);
    fflush(stderr);
  }
}

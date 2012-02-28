#include "config.h"

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
#if LLVM_MAJOR_VERSION >=2 && LLVM_MINOR_VERSION >= 9
# include "llvm/ADT/OwningPtr.h"
# include "llvm/Support/Process.h"
# include "llvm/Support/Signals.h"
# include "llvm/Support/system_error.h"
#else
# include "llvm/System/Process.h"
# include "llvm/System/Signals.h"
#endif
#include "llvm/Target/TargetSelect.h"
#include "llvm/Target/TargetOptions.h"
#include <cerrno>
#include <cstdlib>
#include <iostream>

#include "wrapper.h"

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

extern void* getExceptionObject;
extern void* scalaPersonality;

static void *makeFuns(const std::string &name)
{
  if (name == "createOurException") {
    return (void*)createOurException;
  } else if (name == "getExceptionObject") {
    return getExceptionObject;
  } else if (name == "scalaPersonality") {
    return scalaPersonality;
  } else {
    errs() << "Missing function " << name << "\n";
    return (void*)abort;
  }
}

class LogFuns : public JITEventListener {
  public:
  void NotifyFunctionEmitted(const Function &F, void *Code, size_t Size, const EmittedFunctionDetails &details)
  {
    //std::cerr << "Emitted function " << F.getNameStr() << " to address range " << Code << " - " << (void*)(((char*)Code) + Size) << std::endl;
  }
  void NotifyFreeingMachineCode(void *OldPtr)
  {
  }
};

int main(int argc, char *argv[], char * const *envp)
{
  sys::PrintStackTraceOnErrorSignal();
  llvm::JITExceptionHandling = true;
  llvm::JITEmitDebugInfo = true;
  llvm::JITEmitDebugInfoToDisk = true;
  InitializeNativeTarget();
  LLVMContext &Context = getGlobalContext();
  atexit(do_shutdown);
  //cl::ParseCommandLineOptions(argc, argv, "scala runner");
  std::string ErrorMsg;
  Module *Mod = NULL;

#if LLVM_MAJOR_VERSION >= 2 && LLVM_MINOR_VERSION >= 9
  OwningPtr<MemoryBuffer> Buffer;
  error_code errc = MemoryBuffer::getFileOrSTDIN(argv[1], Buffer);
  if (errc) {
    errs() << argv[0] << ": error load program '" << argv[1] << "': " << errc.message() << "\n";
    exit(1);
  } else {
    Mod = getLazyBitcodeModule(Buffer.get(), Context, &ErrorMsg);
  }
  if (!Mod) {
    errs() << argv[0] << ": error loading program '" << argv[1] << "': "
           << ErrorMsg << "\n";
    exit(1);
  }
#else
  if (MemoryBuffer *Buffer = MemoryBuffer::getFileOrSTDIN(argv[1],&ErrorMsg)) {
    Mod = getLazyBitcodeModule(Buffer, Context, &ErrorMsg);
    if (!Mod) delete Buffer;
  }
  if (!Mod) {
    errs() << argv[0] << ": error loading program '" << argv[1] << "': "
           << ErrorMsg << "\n";
    exit(1);
  }
#endif


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

  EE->InstallLazyFunctionCreator(makeFuns);
  EE->DisableLazyCompilation(false);
  EE->RegisterJITEventListener(new LogFuns());

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
  mainfnname += "_Mmain_A_Njava_Dlang_DString_Rscala_DUnit";

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

  Function *wrapper = createMainWrapperFunction(*Mod, EntryFn, ModuleInstance, InitFn, "main_wrapper");

  //Mod->MaterializeAllPermanently();

  args.clear();

  std::vector<std::string> jitargv;
  for (int i = 3; i < argc; i++) {
    jitargv.push_back(std::string(argv[i]));
  }
  EE->runFunctionAsMain(wrapper, jitargv, envp);

  EE->runStaticConstructorsDestructors(true);

  //Mod->dump();

  exit(0);
}

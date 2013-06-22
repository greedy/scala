#include "config.h"

#include <string>

#include "llvm/Support/ManagedStatic.h"
#include "llvm/Bitcode/Archive.h"
#include "llvm/Bitcode/ReaderWriter.h"
#include "llvm/LLVMContext.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/Module.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/MemoryBuffer.h"
#if LLVM_MAJOR_VERSION >=2 && LLVM_MINOR_VERSION >= 9
# include "llvm/ADT/OwningPtr.h"
# include "llvm/Support/Process.h"
# include "llvm/Support/Signals.h"
# include "llvm/Support/system_error.h"
#else
# include "llvm/System/Process.h"
# include "llvm/System/Signals.h"
#endif
#include "wrapper.h"

using namespace llvm;

int main(int argc, char *argv[], char * const *envp)
{
  sys::PrintStackTraceOnErrorSignal();
  LLVMContext &Context = getGlobalContext();
  atexit(llvm_shutdown);
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
  outs() << "loaded module " << Mod << " " << Buffer << " " << Buffer.get() << "\n";
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


  Function *wrapper = createMainWrapperFunction(*Mod, EntryFn, ModuleInstance, InitFn, "main");

  Mod->MaterializeAllPermanently();

  raw_fd_ostream out("b.out.bc", ErrorMsg);
  if (!ErrorMsg.empty()) {
    errs() << "Error opening output file:" << ErrorMsg << "\n";
    return -1;
  }
  WriteBitcodeToFile(Mod, out);

  return 0;
}

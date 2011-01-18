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
#include "llvm/Support/IRBuilder.h"
#include "llvm/System/Process.h"
#include "llvm/System/Signals.h"
#include "llvm/Target/TargetSelect.h"
#include "llvm/Target/TargetOptions.h"
#include "llvm/Intrinsics.h"
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

static Function*
createMainWrapperFunction(
    Module &module,
    Function *realMain,
    GlobalVariable *moduleGlobal)
{
  LLVMContext &ctx = module.getContext();

  IRBuilder<> builder(ctx);
  std::vector<const Type*> argtypes;
  argtypes.push_back(Type::getInt32Ty(ctx));
  argtypes.push_back(Type::getInt8PtrTy(ctx)->getPointerTo());
  FunctionType *funtype = FunctionType::get(Type::getVoidTy(ctx), argtypes, false);
  Function *ret = Function::Create(funtype, Function::InternalLinkage, "main_wrapper", &module);

  Function::arg_iterator funargs = ret->arg_begin();
  Value* argc = funargs++;
  Value* argv = funargs++;

  BasicBlock *entryBlock = BasicBlock::Create(ctx, "entry", ret);
  BasicBlock *normalBlock = BasicBlock::Create(ctx, "normal", ret);
  BasicBlock *exceptionBlock = BasicBlock::Create(ctx, "exception", ret);

  std::vector<Value*> args;

  builder.SetInsertPoint(entryBlock);

  args.push_back(builder.CreateBitCast(moduleGlobal, realMain->getFunctionType()->getParamType(0)));
  args.push_back(builder.CreateBitCast(builder.CreateCall2(module.getFunction("rt_argvtoarray"), argc, argv), realMain->getFunctionType()->getParamType(1)));

  builder.CreateInvoke(realMain, normalBlock, exceptionBlock, args.begin(), args.end());

  args.clear();

  builder.SetInsertPoint(normalBlock);
  builder.CreateRetVoid();
  builder.SetInsertPoint(exceptionBlock);
  Value *uwx = builder.CreateCall(Intrinsic::getDeclaration(&module, Intrinsic::eh_exception));
  Value *personality = builder.CreateBitCast(
      module.getFunction("scalaPersonality"), builder.getInt8Ty()->getPointerTo());
  Value *throwableClass = builder.CreateBitCast(
      module.getNamedGlobal("class_java_Dlang_DThrowable"), builder.getInt8Ty()->getPointerTo());

  args.push_back(uwx);
  args.push_back(personality);
  args.push_back(throwableClass);
  args.push_back(ConstantInt::get(builder.getInt32Ty(), 1));
  args.push_back(ConstantInt::get(builder.getInt32Ty(), 0));

  builder.CreateCall(
      Intrinsic::getDeclaration(&module, Intrinsic::eh_selector), 
      args.begin(), args.end());

  args.clear();

  builder.CreateCall(
      module.getFunction("rt_printexception"),
      builder.CreateCall(
        module.getFunction("getExceptionObject"),
        uwx));
  builder.CreateRetVoid();

  return ret;
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

  EE->runFunction(InitFn, args);

  Function *wrapper = createMainWrapperFunction(*Mod, EntryFn, ModuleInstance);

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

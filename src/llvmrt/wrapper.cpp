#include "wrapper.h"

#include "llvm/Function.h"
#include "llvm/Support/IRBuilder.h"
#include "llvm/Intrinsics.h"

using namespace llvm;

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

Function*
createMainWrapperFunction(
    Module &module,
    Function *realMain,
    GlobalVariable *moduleGlobal,
    Function *modInitFn,
    const char *name)
{
  LLVMContext &ctx = module.getContext();

  IRBuilder<> builder(ctx);
  std::vector<const Type*> argtypes;
  argtypes.push_back(Type::getInt32Ty(ctx));
  argtypes.push_back(Type::getInt8PtrTy(ctx)->getPointerTo());
  FunctionType *funtype = FunctionType::get(Type::getVoidTy(ctx), argtypes, false);
  Function *ret = Function::Create(funtype, Function::ExternalLinkage, name, &module);

  Function *makeref = module.getFunction("rt_makeref");

  Function::arg_iterator funargs = ret->arg_begin();
  Value* argc = funargs++;
  Value* argv = funargs++;

  BasicBlock *entryBlock = BasicBlock::Create(ctx, "entry", ret);
  BasicBlock *normalBlock = BasicBlock::Create(ctx, "normal", ret);
  BasicBlock *exceptionBlock = BasicBlock::Create(ctx, "exception", ret);

  std::vector<Value*> args;

  builder.SetInsertPoint(entryBlock);

  builder.CreateCall(modInitFn);

  args.push_back(builder.CreateCall(makeref, builder.CreateBitCast(moduleGlobal, makeref->getFunctionType()->getParamType(0))));
  args.push_back(builder.CreateCall(makeref, builder.CreateBitCast(builder.CreateCall2(module.getFunction("rt_argvtoarray"), argc, argv), makeref->getFunctionType()->getParamType(0))));

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


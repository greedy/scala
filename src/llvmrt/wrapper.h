#include <string>
#include "llvm/Module.h"

class llvm::Function;
class llvm::GlobalVariable;

std::string encodeName(const std::string &s);

llvm::Function*
createMainWrapperFunction(
    llvm::Module &module,
    llvm::Function *realMain,
    llvm::GlobalVariable *moduleGlobal,
    llvm::Function *modInitFn,
    const char *name);

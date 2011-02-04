/* ===-- exceptionDemo.cpp - Implement exceptionDemo.cpp -----------------===  
*                                                                               
* This file is distributed under the University of Illinois Open Source         
* License. See following included University of Illinois/NCSA Open Source 
* License.
*                                                                               
* ===----------------------------------------------------------------------===  
*                                                                               
*                                                
==============================================================================
License Start
==============================================================================

University of Illinois/NCSA
Open Source License

Copyright (c) 2003-2010 University of Illinois at Urbana-Champaign.
All rights reserved.

Developed by:

    Garrison Venn - Objective Implementation & Design, inc.
    The LLVM Compiler Infrastructure compiler-rt Project
    The LLVM Compiler Kaleidoscope Example


Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal with
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimers.

    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimers in the
      documentation and/or other materials provided with the distribution.

    * Neither the names of the LLVM Team, University of Illinois at
      Urbana-Champaign, nor the names of its contributors may be used to
      endorse or promote products derived from this Software without specific
      prior written permission.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE
SOFTWARE.

==============================================================================
License End
==============================================================================
*
*/

//===----------------------------------------------------------------------===//
//
// Demo program which implements an example LLVM exception implementation, and
// shows several test cases including the handling of foreign exceptions.
// It is run with type info types arguments to throw. A test will
// be run for each given type info type. While type info types with the value 
// of -1 will trigger a foreign C++ exception to be thrown; type info types
// <= 6 and >= 1 will cause the associated generated exceptions to be thrown 
// and caught by generated test functions; and type info types > 6
// will result in exceptions which pass through to the test harness. All other
// type info types are not supported and could cause a crash. In all cases,
// the "finally" blocks of every generated test functions will executed 
// regardless of whether or not that test function ignores or catches the
// thrown exception.
//
// examples:
//
// exceptionDemo
//
//     causes a usage to be printed to stderr
// 
// exceptionDemo 2 3 7 -1
//
//     results in the following cases:
//         - Value 2 causes an exception with a type info type of 2 to be thrown
//           and caught by an inner generated test function.
//         - Value 3 causes an exception with a type info type of 3 to be thrown
//           and caught by an outer generated test function.
//         - Value 7 causes an exception with a type info type of 7 to be thrown
//           and NOT be caught by any generated function.
//         - Value -1 causes a foreign C++ exception to be thrown and not be
//           caught by any generated function
//
//     Cases -1 and 7 are caught by a C++ test harness where the validity of
//         of a C++ catch(...) clause catching a generated exception with a 
//         type info type of 7 is questionable.
//
//===----------------------------------------------------------------------===//


// Note: On OS X this file is included from the Xcode Developer file 
//       directories.
//
#include <unwind.h>

#include "llvm/Support/Dwarf.h"

#include <cstdio>
#include <string>
#include <sstream>
#include <map>
#include <vector>
#include <stdexcept>
#include <cassert>
#include <stdint.h>
#include <cstdlib>
#include <cstring>


#ifndef USE_GLOBAL_STR_CONSTS
#define USE_GLOBAL_STR_CONSTS true
#endif

//===----------------------------------------------------------------------===//
// Code Generation
//===----------------------------------------------------------------------===//


typedef struct _Unwind_Context* _Unwind_Context_t;

/// This is our Exception class which relies on a negative offset to calculate
/// pointers to its instances from pointers to its unwindException member.
/// 
/// Note: The above unwind.h defines struct _Unwind_Exception to be aligned
///       on a double word boundary. This is necessary to match the standard:
///       http://refspecs.freestandards.org/abi-eh-1.21.html
///
struct OurBaseException_t
{
    uint8_t *obj;

    // Note: This is properly aligned in unwind.h
    //
    struct _Unwind_Exception unwindException;
};


// Note: Not needed since we are C++
//
typedef struct OurBaseException_t OurException;
typedef struct _Unwind_Exception OurUnwindException;

//
// Various globals used to support typeinfo and generatted exceptions in general
//

int64_t baseFromUnwindOffset;

const unsigned char ourBaseExcpClassChars[] = 
                                {'e', 'p', 'f', 'l', 's', 'c', 'a', 'l'};


static uint64_t ourBaseExceptionClass = 0x4550464c7363616cLL;

extern "C" {
/// Deletes the true previosly allocated exception whose address
/// is calculated from the supplied OurBaseException_t::unwindException
/// member address. Handles (ignores), NULL pointers.
/// @param expToDelete exception to delete
///
void deleteOurException (OurUnwindException* expToDelete)
{
#ifdef DEBUG
    fprintf(stderr,
            "deleteOurException(...).\n");
#endif

    if (expToDelete)
    {
        if (expToDelete->exception_class == ourBaseExceptionClass)
        {
            free(((char*) expToDelete) + baseFromUnwindOffset);
        }
    }
}


/// struct _Unwind_Exception API mandated delete function used by foreign
/// exception handlers when deleting our exception (OurException), instances.
/// @param reason @link http://refspecs.freestandards.org/abi-eh-1.21.html 
/// @unlink
/// @param expToDelete exception instance to delete
///
void deleteFromUnwindOurException (_Unwind_Reason_Code reason,
                                   OurUnwindException* expToDelete)
{
#ifdef DEBUG
    fprintf(stderr,
            "deleteFromUnwindOurException(...).\n");
#endif

    deleteOurException(expToDelete);
}


/// Creates (allocates on the heap), an exception (OurException instance),
/// of the supplied type info type.
/// @param type type info type
///
OurUnwindException* createOurException (uint8_t *obj)
{
    size_t size = sizeof(OurException);

    OurException* ret = (OurException*) memset(malloc(size), 0, size);

    (ret->obj) = obj;

    (ret->unwindException).exception_class = ourBaseExceptionClass;
    (ret->unwindException).exception_cleanup = deleteFromUnwindOurException;

#ifdef DEBUG
    fprintf(stderr, "createOurException obj = %p ret = %p &ret->unwindException = %p\n", obj, ret, &(ret->unwindException));
#endif
    return(&(ret->unwindException));
}


/// read a uleb128 encoded value and advance pointer 
/// See Variable Length Data in: 
/// @link http://dwarfstd.org/Dwarf3.pdf @unlink
/// @param data reference variable holding memory pointer to decode from
/// @returns decoded value
/// 
static uintptr_t readULEB128(const uint8_t** data)
{
    uintptr_t result = 0;
    uintptr_t shift = 0;
    unsigned char byte;
    const uint8_t* p = *data;

    do 
    {
        byte = *p++;
        result |= (byte & 0x7f) << shift;
        shift += 7;
    } 
    while (byte & 0x80);

    *data = p;

    return result;
}


/// read a sleb128 encoded value and advance pointer 
/// See Variable Length Data in: 
/// @link http://dwarfstd.org/Dwarf3.pdf @unlink
/// @param data reference variable holding memory pointer to decode from
/// @returns decoded value
/// 
static uintptr_t readSLEB128(const uint8_t** data)
{
    uintptr_t result = 0;
    uintptr_t shift = 0;
    unsigned char byte;
    const uint8_t* p = *data;

    do 
    {
        byte = *p++;
        result |= (byte & 0x7f) << shift;
        shift += 7;
    } 
    while (byte & 0x80);

    *data = p;

    // Note: Not sure this works
    //
    if ((byte & 0x40) && (shift < (sizeof(result) << 3)))
    {
        // Note: I wonder why I can't do: ~0 << shift
        //
        // result |= -(1L << shift);
        result |= (~0 << shift);
    }

    return result;
}


/// read a pointer encoded value and advance pointer 
/// See Variable Length Data in: 
/// @link http://dwarfstd.org/Dwarf3.pdf @unlink
/// @param data reference variable holding memory pointer to decode from
/// @param encoding dwarf encoding type
/// @returns decoded value
///
static uintptr_t readEncodedPointer(const uint8_t** data, uint8_t encoding)
{
    const uint8_t* p = *data;
    uintptr_t result = 0;

    if (encoding != llvm::dwarf::DW_EH_PE_omit) 
    {
        /* first get value 
         */
        switch (encoding & 0x0F) 
        {
            case llvm::dwarf::DW_EH_PE_absptr:
                result = *((uintptr_t*)p);
                p += sizeof(uintptr_t);
                break;
            case llvm::dwarf::DW_EH_PE_uleb128:
                result = readULEB128(&p);
                break;
            // Note: This case has not been tested
            //
            case llvm::dwarf::DW_EH_PE_sleb128:
                result = readSLEB128(&p);
                break;
            case llvm::dwarf::DW_EH_PE_udata2:
                result = *((uint16_t*)p);
                p += sizeof(uint16_t);
                break;
            case llvm::dwarf::DW_EH_PE_udata4:
                result = *((uint32_t*)p);
                p += sizeof(uint32_t);
                break;
            case llvm::dwarf::DW_EH_PE_udata8:
                result = *((uint64_t*)p);
                p += sizeof(uint64_t);
                break;
            case llvm::dwarf::DW_EH_PE_sdata2:
                result = *((int16_t*)p);
                p += sizeof(int16_t);
                break;
            case llvm::dwarf::DW_EH_PE_sdata4:
                result = *((int32_t*)p);
                p += sizeof(int32_t);
                break;
            case llvm::dwarf::DW_EH_PE_sdata8:
                result = *((int64_t*)p);
                p += sizeof(int64_t);
                break;
            default:
                /* not supported 
                 */
                abort();
                break;
        }

        /* then add relative offset 
         */
        switch ( encoding & 0x70 ) 
        {
            case llvm::dwarf::DW_EH_PE_absptr:
                /* do nothing 
                 */
                break;
            case llvm::dwarf::DW_EH_PE_pcrel:
                result += (uintptr_t)(*data);
                break;
            case llvm::dwarf::DW_EH_PE_textrel:
            case llvm::dwarf::DW_EH_PE_datarel:
            case llvm::dwarf::DW_EH_PE_funcrel:
            case llvm::dwarf::DW_EH_PE_aligned:
            default:
                /* not supported 
                 */
                abort();
                break;
        }

        /* then apply indirection 
         */
        if (encoding & llvm::dwarf::DW_EH_PE_indirect) 
        {
            result = *((uintptr_t*)result);
        }

        *data = p;
    }

    return result;
}


/// Functionality which deals with Dwarf actions matching our 
/// type infos (OurExceptionType_t instances). Returns whether or
/// not a dwarf emitted action matches the supplied exception type.
/// If such a match succeeds, the resultAction argument will be set
/// with > 0 index value. Only corresponding llvm.eh.selector type info
/// arguments, cleanup arguments are supported. Filters are not supported.
/// See Variable Length Data in: 
/// @link http://dwarfstd.org/Dwarf3.pdf @unlink
/// Also see @link http://refspecs.freestandards.org/abi-eh-1.21.html @unlink
/// @param resultAction reference variable which will be set with result
/// @param classInfo our array of type info pointers (to globals)
/// @param actionEntry index into above type info array or 0 (clean up). 
///        We do not support filters.
/// @param exceptionClass exception class (_Unwind_Exception::exception_class)
///        of thrown exception.
/// @param exceptionObject thrown _Unwind_Exception instance.
/// @returns whether or not a type info was found. False is returned if only
///          a cleanup was found
///
static bool handleActionValue (int64_t *resultAction,
                               struct OurExceptionType_t **classInfo, 
                               uintptr_t actionEntry, 
                               uint64_t exceptionClass, 
                               struct _Unwind_Exception *exceptionObject)
{
    bool ret = false;

    if (resultAction && 
        exceptionObject && 
        (exceptionClass == ourBaseExceptionClass))
    {
        struct OurBaseException_t* excp = (struct OurBaseException_t*)
                            (((char*) exceptionObject) + baseFromUnwindOffset);

        uint8_t *obj = excp->obj;

#ifdef DEBUG
        fprintf(stderr,
                "handleActionValue(...): exceptionObject = <%p>, "
                    "excp = <%p>.\n",
                exceptionObject,
                excp);
#endif

        const uint8_t *actionPos = (uint8_t*) actionEntry,
                      *tempActionPos;

        int64_t typeOffset = 0,
                actionOffset;

        for (int i = 0; true; ++i)
        {
            // Each emitted dwarf action corresponds to a 2 tuple of
            // type info address offset, and action offset to the next
            // emitted action.
            //
            typeOffset = readSLEB128(&actionPos);
            tempActionPos = actionPos;
            actionOffset = readSLEB128(&tempActionPos);

#ifdef DEBUG
            fprintf(stderr,
                    "handleActionValue(...):typeOffset: <%lld>, "
                        "actionOffset: <%lld>.\n",
                    typeOffset,
                    actionOffset);
#endif

            assert((typeOffset >= 0) && 
                   "handleActionValue(...):filters are not supported.");

            // Note: A typeOffset == 0 implies that a cleanup llvm.eh.selector
            //       argument has been matched.
            //
            // TODO: check classes here
            if ((typeOffset > 0)/* &&
                (type == (classInfo[-typeOffset])->type)*/)
            {
#ifdef DEBUG
                fprintf(stderr,
                        "handleActionValue(...):actionValue <%d> found.\n",
                        i);
#endif

                *resultAction = i + 1;
                ret = true;
                break;
            }
            else
            {
#ifdef DEBUG
                fprintf(stderr,
                        "handleActionValue(...):actionValue not found.\n");
#endif

                if (actionOffset)
                {
                    actionPos += actionOffset;
                }
                else
                {
                    break;
                }
            }
        }
    }

    return(ret);
}


/// Deals with the Language specific data portion of the emitted dwarf code.
/// See @link http://refspecs.freestandards.org/abi-eh-1.21.html @unlink
/// @param version unsupported (ignored), unwind version
/// @param lsda language specific data area
/// @param _Unwind_Action actions minimally supported unwind stage 
///        (forced specifically not supported)
/// @param exceptionClass exception class (_Unwind_Exception::exception_class)
///        of thrown exception.
/// @param exceptionObject thrown _Unwind_Exception instance.
/// @param context unwind system context
/// @returns minimally supported unwinding control indicator 
///
static _Unwind_Reason_Code handleLsda 
                           (
                               int version, 
                               const uint8_t* lsda,
                               _Unwind_Action actions,
                               uint64_t exceptionClass, 
                               struct _Unwind_Exception* exceptionObject,
                               _Unwind_Context_t context
                           )
{
    _Unwind_Reason_Code ret = _URC_CONTINUE_UNWIND;

    if (lsda)
    {
#ifdef DEBUG
        fprintf(stderr, 
                "handleLsda(...):lsda is non-zero.\n");
#endif

        // Get the current instruction pointer and offset it before next
        // instruction in the current frame which threw the exception.
        //
        uintptr_t pc = _Unwind_GetIP(context)-1;

        // Get beginning current frame's code (as defined by the 
        // emitted dwarf code)
        //
        uintptr_t funcStart = _Unwind_GetRegionStart(context);
        uintptr_t pcOffset = pc - funcStart;

        struct OurExceptionType_t** classInfo = NULL;

        //
        // Note: See JITDwarfEmitter::EmitExceptionTable(...) for corresponding
        //       dwarf emittion
        //

        /* Parse LSDA header. */
        //
        uint8_t lpStartEncoding = *lsda++;

        if (lpStartEncoding != llvm::dwarf::DW_EH_PE_omit) 
        {
            readEncodedPointer(&lsda, lpStartEncoding); 
        }

        uint8_t ttypeEncoding = *lsda++;
        uintptr_t classInfoOffset;

        if (ttypeEncoding != llvm::dwarf::DW_EH_PE_omit) 
        {
            // Calculate type info locations in emitted dwarf code which
            // were flagged by type info arguments to llvm.eh.selector
            // intrinsic
            //
            classInfoOffset = readULEB128(&lsda);
            classInfo = (struct OurExceptionType_t**) (lsda + classInfoOffset);
        }


        /* Walk call-site table looking for range that 
         * includes current PC. 
         */
        uint8_t         callSiteEncoding = *lsda++;
        uint32_t        callSiteTableLength = readULEB128(&lsda);
        const uint8_t*  callSiteTableStart = lsda;
        const uint8_t*  callSiteTableEnd = callSiteTableStart + 
                                                        callSiteTableLength;
        const uint8_t*  actionTableStart = callSiteTableEnd;

        const uint8_t*  callSitePtr = callSiteTableStart;

        bool foreignException = false;


        while (callSitePtr < callSiteTableEnd) 
        {
            uintptr_t start = readEncodedPointer(&callSitePtr, 
                                                 callSiteEncoding);

            uintptr_t length = readEncodedPointer(&callSitePtr, 
                                                  callSiteEncoding);

            uintptr_t landingPad = readEncodedPointer(&callSitePtr, 
                                                      callSiteEncoding);

            // Note: Action value
            //
            uintptr_t actionEntry = readULEB128(&callSitePtr);

            if (exceptionClass != ourBaseExceptionClass)
            {
                // We have been notified of a foreign exception being thrown,
                // and we therefore need to execute cleanup landing pads
                //
                actionEntry = 0;
                foreignException = true;
            }

            if (landingPad == 0)
            {
#ifdef DEBUG
                fprintf(stderr,
                        "handleLsda(...): No landing pad found.\n");
#endif

                continue; /* no landing pad for this entry */
            }

            if (actionEntry)
            {
                actionEntry += ((uintptr_t) actionTableStart) - 1;
            }
            else
            {
#ifdef DEBUG
                fprintf(stderr,
                        "handleLsda(...):No action table found.\n");
#endif
            }

            bool exceptionMatched = false;

            if ((start <= pcOffset) && (pcOffset < (start + length))) 
            {
#ifdef DEBUG
                fprintf(stderr,
                        "handleLsda(...): Landing pad found.\n");
#endif

                int64_t actionValue = 0;

                if (actionEntry)
                {
                    exceptionMatched = handleActionValue
                                       (
                                           &actionValue,
                                           classInfo, 
                                           actionEntry, 
                                           exceptionClass, 
                                           exceptionObject
                                       );
                }

                if (!(actions & _UA_SEARCH_PHASE))
                {
#ifdef DEBUG
                    fprintf(stderr,
                            "handleLsda(...): installed landing pad "
                                "context.\n");
#endif

                    /* Found landing pad for the PC.
                     * Set Instruction Pointer to so we re-enter function 
                     * at landing pad. The landing pad is created by the 
                     * compiler to take two parameters in registers.
                     */
                    _Unwind_SetGR(context, 
                                  __builtin_eh_return_data_regno(0), 
                                  (uintptr_t)exceptionObject);

                    // Note: this virtual register directly corresponds
                    //       to the return of the llvm.eh.selector intrinsic
                    //
                    if (!actionEntry || !exceptionMatched)
                    {
                        // We indicate cleanup only
                        //
                        _Unwind_SetGR(context, 
                                      __builtin_eh_return_data_regno(1), 
                                      0);
                    }
                    else
                    {
                        // Matched type info index of llvm.eh.selector intrinsic
                        // passed here.
                        //
                        _Unwind_SetGR(context, 
                                      __builtin_eh_return_data_regno(1), 
                                      actionValue);
                    }

                    // To execute landing pad set here
                    //
                    _Unwind_SetIP(context, funcStart + landingPad);

                    ret = _URC_INSTALL_CONTEXT;

                    break;
                }
                else
                {
                    if (exceptionMatched)
                    {
#ifdef DEBUG
                        fprintf(stderr,
                                "handleLsda(...): setting handler found.\n");
#endif

                        ret = _URC_HANDLER_FOUND;
                    }
                    else
                    {
                        //
                        // Note: Only non-clean up handlers are marked as
                        //       found. Otherwise the clean up handlers will be 
                        //       re-found and executed during the clean up 
                        //       phase.
                        //
#ifdef DEBUG
                        fprintf(stderr,
                                "handleLsda(...): cleanup handler found.\n");
#endif
                    }

                    break;
                }
            }
        }
    }

    return(ret);
}


/// This is the personality function which is embedded (dwarf emitted), in the
/// dwarf unwind info block. Again see: JITDwarfEmitter.cpp.
/// See @link http://refspecs.freestandards.org/abi-eh-1.21.html @unlink
/// @param version unsupported (ignored), unwind version
/// @param _Unwind_Action actions minimally supported unwind stage 
///        (forced specifically not supported)
/// @param exceptionClass exception class (_Unwind_Exception::exception_class)
///        of thrown exception.
/// @param exceptionObject thrown _Unwind_Exception instance.
/// @param context unwind system context
/// @returns minimally supported unwinding control indicator 
///
_Unwind_Reason_Code scalaPersonality 
                    (
                        int version, 
                        _Unwind_Action actions,
                        uint64_t exceptionClass, 
                        struct _Unwind_Exception* exceptionObject,
                        _Unwind_Context_t context
                    )
{
#ifdef DEBUG
  fprintf(stderr, "scalaPersonality exceptionObject = %p\n", exceptionObject);
#endif
    _Unwind_Reason_Code ret = _URC_CONTINUE_UNWIND;

#ifdef DEBUG
    fprintf(stderr, 
            "We are in ourPersonality(...):actions is <%d>.\n",
            actions);
#endif

#ifdef DEBUG
    if (actions & _UA_SEARCH_PHASE)
    {

        fprintf(stderr, "ourPersonality(...):In search phase.\n");
    }
    else
    {
        fprintf(stderr, "ourPersonality(...):In non-search phase.\n");

    }
#endif

    const uint8_t* lsda = (uint8_t*) 
                            _Unwind_GetLanguageSpecificData(context);

#ifdef DEBUG
    fprintf(stderr, 
            "ourPersonality(...):lsda = <%p>.\n",
            lsda);
#endif

    // The real work of the personality function is captured here
    //
    ret = handleLsda(version,
                     lsda,
                     actions,
                     exceptionClass,
                     exceptionObject,
                     context);

#ifdef DEBUG
    fprintf(stderr, "ourPersonality returning %d\n", ret);
#endif

    return(ret);
}

uint8_t *getExceptionObject(_Unwind_Exception *uwx)
{
#ifdef DEBUG
  fprintf(stderr, "getExceptionObject uwx = %p\n", uwx);
#endif
  struct OurBaseException_t dummyException;

  // Calculate offset of OurException::unwindException member.
  //
  baseFromUnwindOffset = ((uintptr_t) &dummyException) - 
    ((uintptr_t) &(dummyException.unwindException));

  struct OurBaseException_t* excp = (struct OurBaseException_t*)
    (((char*) uwx) + baseFromUnwindOffset);
  return excp->obj;
}
} // extern "C"

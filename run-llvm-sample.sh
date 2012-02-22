#!/bin/bash
set -x
autoreconf
./configure
ant build
cd docs/examples/llvm
make run-sample

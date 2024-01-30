#!/bin/bash

echo "No OpenACC. Notice how user time == real time => no parallelization"
ACC=" " TGT=" " bash build.sh && time .foo/MatrixMultiplyJNI

echo "OpenACC multicore. Notice how user time > real time => running multiple CPU threads, but at poor parallel efficiency. 'real' should be less than before, 'user' higher."
TGT="-ta=multicore" bash build.sh && time .foo/MatrixMultiplyJNI

echo "OpenACC gpu. Copies may slow this down to (less-than) CPU speed"
TGT="-gpu=nordc" bash build.sh && time .foo/MatrixMultiplyJNI


#!/bin/bash

set -e

TGT="${TGT:--ta=multicore}"
TGT="-gpu=nordc" #CUDA GPU
ACC="${ACC:--acc}"
# ACC="" # disable openacc

nvc++ -shared $ACC $TGT MatrixMultiply.cpp -o libMatrixMultiplyJNI.so -Minfo
g++ MatrixMultiplyTest.cpp -L$PWD -lMatrixMultiplyJNI -o MatrixMultiplyTest

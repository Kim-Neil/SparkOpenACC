#!/bin/bash

set -e

TGT="${TGT:--ta=multicore}"
# TGT="-gpu=nordc" #CUDA GPU
ACC="${ACC:--acc}"
#ACC="" # disable openacc

# put the paths where jni.h and jni_md.h are here
JNI_INCLUDE="-I/usr/lib/jvm/java-8-openjdk-amd64/include -I/usr/lib/jvm/java-8-openjdk-amd64/include/linux"

javac cz/adamh/utils/NativeUtils.java
javac -h . foo/MatrixMultiplyJNI.java
scalac foo/main.scala
nvc++ -shared $ACC $TGT MatrixMultiply.cpp foo_MatrixMultiplyJNI.cpp -o foo/libMatrixMultiplyJNI.so -Minfo $JNI_INCLUDE

jar -cvf foo.jar foo/ cz

echo "Run using: java -cp foo.jar foo.MatrixMultiplyJNI"
echo "Run Spark test using: java -cp foo.jar -cp \"\$CLASSPATH\" foo.MatrixMultiplyJNI"

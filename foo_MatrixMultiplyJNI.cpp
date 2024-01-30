#include <iostream>
#include <cstdlib>
#include <ctime>
#include <chrono>
#include <jni.h>

// Define a macro to convert a 2D array index to a 1D array index
#define IDX(i, j, numCols) ((i) * (numCols) + (j))

extern "C" {

JNIEXPORT void JNICALL Java_foo_MatrixMultiplyJNI_matrixMultiply(JNIEnv *env, jobject obj, jint rowsA, jint colsA, jint rowsB, jint colsB, jdoubleArray matrixA, jdoubleArray matrixB, jdoubleArray resultMatrix) {
    // Obtain native arrays from Java arrays
    jdouble *matrixAElements = env->GetDoubleArrayElements(matrixA, nullptr);
    jdouble *matrixBElements = env->GetDoubleArrayElements(matrixB, nullptr);
    jdouble *resultMatrixElements = env->GetDoubleArrayElements(resultMatrix, nullptr);

    // C++ matrix multiplication logic
    // Copy matrixA and matrixB to the GPU
    #pragma acc data copyin(matrixAElements[0:rowsA*colsA], matrixBElements[0:rowsB*colsB]) copyout(resultMatrixElements[0:rowsA*colsB])
    {
        // Matrix multiplication on the GPU
        #pragma acc parallel loop collapse(2)
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                resultMatrixElements[IDX(i, j, colsB)] = 0;
                for (int k = 0; k < colsA; k++) {
                    resultMatrixElements[IDX(i, j, colsB)] += matrixAElements[IDX(i, k, colsA)] * matrixBElements[IDX(k, j, colsB)];
                }
            }
        }
    }

    // Release native arrays
    env->ReleaseDoubleArrayElements(matrixA, matrixAElements, JNI_ABORT);
    env->ReleaseDoubleArrayElements(matrixB, matrixBElements, JNI_ABORT);
    env->ReleaseDoubleArrayElements(resultMatrix, resultMatrixElements, 0);
}

JNIEXPORT void JNICALL Java_foo_MatrixMultiplyJNI_isGPUAvailable(JNIEnv *env, jobject obj) {
    // Implement GPU availability check logic if needed
    // For the sake of this example, we assume GPU is available.
}

}  // extern "C"

#include <iostream>
#include <cstdlib>
#include <ctime>
#include <chrono>
#include <openacc.h>
#include "MatrixMultiply.h"


// Define a macro to convert a 2D array index to a 1D array index
#define IDX(i, j, numCols) ((i) * (numCols) + (j))

void matrixMultiply(int rowsA, int colsA, int rowsB, int colsB, double* matrixA, double* matrixB, double* resultMatrix) {
    // Copy matrices to the GPU
    #pragma acc data copyin(matrixA[0:rowsA*colsA], matrixB[0:rowsB*colsB]) copyout(resultMatrix[0:rowsA*colsB])
    {
        // Matrix multiplication on the GPU
        #pragma acc parallel loop collapse(2)
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                resultMatrix[IDX(i, j, colsB)] = 0;
                for (int k = 0; k < colsA; k++) {
                    resultMatrix[IDX(i, j, colsB)] += matrixA[IDX(i, k, colsA)] * matrixB[IDX(k, j, colsB)];
                }
            }
        }
    }
}


int main() {
    // Define the dimensions of the matrices
    int dimensions[] = {400, 600, 800, 1000};

    for (int N : dimensions) {
        // Allocate memory for matrices
        double* matrixA = new double[N * N];
        double* matrixB = new double[N * N];
        double* resultMatrix = new double[N * N];

        // Initialize matrices with random values
        std::srand(static_cast<unsigned int>(std::time(nullptr)));
        for (int i = 0; i < N * N; i++) {
            matrixA[i] = static_cast<double>(std::rand()) / RAND_MAX;
            matrixB[i] = static_cast<double>(std::rand()) / RAND_MAX;
        }

        // Measure execution time
        auto start = std::chrono::high_resolution_clock::now();

        // Perform matrix multiplication
        matrixMultiply(N, N, N, N, matrixA, matrixB, resultMatrix);

        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);

        // Print the result and execution time
        std::cout << "Matrix dimension N=" << N << std::endl;
        std::cout << "Execution time: " << duration.count() << " ms." << std::endl;
        // You can also add code to print or save the resultMatrix

        // Cleanup and deallocate memory
        delete[] matrixA;
        delete[] matrixB;
        delete[] resultMatrix;
    }

    return 0;
}

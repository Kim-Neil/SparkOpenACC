//#pragma once

extern "C" {
  void matrixMultiply(int rowsA, int colsA, int rowsB, int colsB, double* matrixA, double* matrixB, double* resultMatrix);
  bool isGPUAvailable();
}

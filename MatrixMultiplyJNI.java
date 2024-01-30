package foo;

import java.util.Random;

public class MatrixMultiplyJNI {
    // Load the JNI library for matrix multiplication
    static {
        System.loadLibrary("MatrixMultiplyJNI");
    }

    // Define native methods
    private native void matrixMultiply(int rowsA, int colsA, int rowsB, int colsB, double[] matrixA, double[] matrixB, double[] resultMatrix);
    private native void isGPUAvailable();

    public static void main(String[] args) {
        // Define the dimensions of the matrices
        int[] dimensions = {40, 60, 80, 100};

        for (int N : dimensions) {
            long startTime = System.nanoTime();

            // Generate random matrices
            double[] matrixA = generateRandomMatrix(N, N);
            double[] matrixB = generateRandomMatrix(N, N);
            double[] resultMatrix = new double[N * N];

            // Call the native function to execute matrix multiplication
            new MatrixMultiplyJNI().matrixMultiply(N, N, N, N, matrixA, matrixB, resultMatrix);

            // Calculate the overall execution time
            long endTime = System.nanoTime();
            double executionTime = (endTime - startTime) / 1e9; // in seconds

            // Print the result or save it as needed
            System.out.println("Matrix dimension N=" + N);
            System.out.println("Execution time: " + executionTime + " seconds");
            // Print or save resultMatrix
        }
    }

    // Function to generate a random matrix
    public static double[] generateRandomMatrix(int rows, int cols) {
        double[] matrix = new double[rows * cols];
        Random random = new Random();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i * cols + j] = random.nextDouble();
            }
        }

        return matrix;
    }
}

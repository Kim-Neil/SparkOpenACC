import scala.collection.mutable.ArrayBuffer
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import java.util.Random
import java.io._

object SparkMatrixMultiplyFT {
  System.loadLibrary("MatrixMultiplyJNI")

  @native def matrixMultiply(rowsA: Int, colsA: Int, rowsB: Int, colsB: Int, matrixA: Array[Array[Double]], matrixB: Array[Array[Double]]): Array[Array[Double]]
  @native def performGPUAcceleration(matrixA: Array[Array[Double]], matrixB: Array[Array[Double]], numRowsA: Int, numColsA: Int, numRowsB: Int, numColsB: Int): Array[Array[Double]]
  @native def performCPUProcessing(matrixA: Array[Array[Double]], matrixB: Array[Array[Double]], numRowsA: Int, numColsA: Int, numRowsB: Int, numColsB: Int): Array[Array[Double]]

  var iterationCounter = 0
  val gpuThreshold = 2 // Adjust the threshold based on your requirements

  def isGPUAvailable(): Boolean = {
    System.getProperty("gpu.available") == "true"
  }

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setAppName("MatrixMultiply")
      .setMaster("local[*]")
      .set("spark.driver.memory", "12g")
      .set("spark.driver.gpu.core", "3")
      .set("spark.executor.memory", "12g")
      .set("spark.executor.instances", "2")

    val sc = new SparkContext(conf)

    val dimensions = Seq(1000, 4000, 5000, 8000, 10000)

    for (N <- dimensions) {
      val startTime = System.nanoTime()

      val matrixA = generateRandomMatrix(N, N)
      val matrixB = generateRandomMatrix(N, N)

      // Partition matrices into submatrices
      val matrixAPartitioned = partitionMatrices(matrixA)
      val matrixBPartitioned = partitionMatrices(matrixB)

      // Create RDDs for input matrices
      val matrixARDD = createRDD(sc, matrixAPartitioned)
      val matrixBRDD = createRDD(sc, matrixBPartitioned)

      // Perform matrix multiplication for each submatrix with retries
      val resultMatrixRDD = matrixARDD.zip(matrixBRDD).map {
        case (submatrixA, submatrixB) =>
          try {
            if (isGPUAvailable() && gpuThresholdReached()) {
              // Perform GPU-accelerated matrix multiplication
              performGPUAcceleration(submatrixA, submatrixB, submatrixA.length, submatrixA(0).length, submatrixB.length, submatrixB(0).length)
            } else {
              // Perform matrix multiplication on CPU
              performCPUProcessing(submatrixA, submatrixB, submatrixA.length, submatrixA(0).length, submatrixB.length, submatrixB(0).length)
            }
          } catch {
            case e: Exception =>
              println(s"Error during matrix multiplication: ${e.getMessage}")
              // Retry logic: Perform the same computation again
              performCPUProcessing(submatrixA, submatrixB, submatrixA.length, submatrixA(0).length, submatrixB.length, submatrixB(0).length)
          }
      }

      // Calculate the overall execution time
      val endTime = System.nanoTime()
      val executionTime = (endTime - startTime) / 1e9 // in seconds

      // Print the result or save it as needed
      println(s"Matrix dimension N=$N")
      println(s"Execution time: $executionTime seconds")

      // Save sizes to a text file
      val sizesFile = new PrintWriter(new File(s"memory_usage_N$N.txt"))
      matrixARDD.collect().foreach(submatrix => {
        val bytesUsed = submatrix.map(_.map(element => 8).sum).sum // Assuming each element is a double (8 bytes)
        sizesFile.println(s"Submatrix Bytes Used: $bytesUsed bytes")
      })
      sizesFile.close()
    }

    sc.stop()
  }

  def generateRandomMatrix(rows: Int, cols: Int): Array[Array[Double]] = {
    val random = new Random
    Array.fill(rows, cols)(random.nextDouble())
  }

  def getGPUMemory(): Long = {
  // Read GPU memory from system properties or environment variables
  val gpuMemoryString = System.getProperty("spark.executor.resource.gpu.amount")
  if (gpuMemoryString != null) {
    return gpuMemoryString.toLong
  } else {
    // Add a return statement with a default value or a result from a fallback method:
    return if (gpuMemoryString != null) gpuMemoryString.toLong else 1048576L  // Example default value
  }
}

def getCPUCores(): Int = {
  // Retrieve available CPU cores using Runtime methods
  Runtime.getRuntime.availableProcessors()
}

def calculateOptimalSize(matrix: Array[Array[Double]], gpuMemory: Long, cpuCores: Int): (Int, Int) = {
    // Estimate optimal submatrix sizes for GPU and CPU based on available resources and workload characteristics
    val gpuSubmatrixSize = estimateGPUSubmatrixSize(matrix, gpuMemory)
    val cpuSubmatrixSize = estimateCPUSubmatrixSize(matrix, cpuCores)

    // Consider heterogeneous partitioning if GPU and CPU submatrix sizes differ significantly
    if (gpuSubmatrixSize > cpuSubmatrixSize * 2) {  // Adjust threshold as needed
        // Partition matrix into larger submatrices for GPU and smaller submatrices for CPU
        val gpuPartitions = matrix.grouped(gpuSubmatrixSize).toArray
        val cpuPartitions = gpuPartitions.flatMap(submatrix => submatrix.grouped(cpuSubmatrixSize))
        return (gpuSubmatrixSize, cpuSubmatrixSize)
    } else {
        // Use a single submatrix size for both GPU and CPU
        return (gpuSubmatrixSize, gpuSubmatrixSize)
    }
}

// Helper functions for estimating submatrix sizes
  def estimateGPUSubmatrixSize(matrix: Array[Array[Double]], gpuMemory: Long): Int = {
    // Calculate estimated GPU submatrix size based on available memory and matrix dimensions
    // Example implementation, adjust as needed:
    val matrixSize = matrix.length * matrix(0).length * 8  // Assuming double-precision
    val maxSubmatrixSize = (gpuMemory / 2).toInt  // Leave some memory for overhead
    val estimatedSize = math.min(maxSubmatrixSize, matrixSize)  // Ensure it fits in memory
    // ... consider other factors like number of GPUs, overhead, etc. ...
    // Return estimated size
    estimatedSize
  }

  def estimateCPUSubmatrixSize(matrix: Array[Array[Double]], cpuCores: Int): Int = {
    // Calculate estimated CPU submatrix size based on available cores and matrix dimensions
    // Example implementation, adjust as needed:
    val matrixSize = matrix.length * matrix(0).length
    val maxSubmatrixSize = matrixSize / cpuCores  // Simple distribution for multi-core
    // ... consider matrix density, cache sizes, etc. ...
    // Return estimated size
    maxSubmatrixSize
  }

  def partitionMatrices(matrix: Array[Array[Double]]): Array[Array[Array[Double]]] = {
    val gpuMemory = getGPUMemory()
    val cpuCores = getCPUCores()
    val (gpuSubmatrixSize, cpuSubmatrixSize) = calculateOptimalSize(matrix, gpuMemory, cpuCores)

    // Partition based on heterogeneous partitioning decision
    if (gpuSubmatrixSize > cpuSubmatrixSize * 2) {
      // Partition into larger submatrices for GPU and smaller submatrices for CPU
      val gpuPartitions = matrix.grouped(gpuSubmatrixSize).toArray
      val cpuPartitions = gpuPartitions.flatMap(submatrix => submatrix.grouped(cpuSubmatrixSize))
      return gpuPartitions ++ cpuPartitions  // Combine both partitions
    } else {
      // Use a single submatrix size for both GPU and CPU
      return matrix.grouped(gpuSubmatrixSize).toArray
    }
  }

    def createRDD(sc: SparkContext, matrices: Array[Array[Array[Double]]]): RDD[Array[Array[Double]]] = {
    sc.parallelize(matrices)
  }

  def gpuThresholdReached(): Boolean = {
    // Increment the counter with each iteration
    iterationCounter += 1

    // Check if the threshold is reached
    iterationCounter % gpuThreshold == 0
  }
}

package loaders

import java.io.File
import java.io.FileInputStream

import scala.util.Random

/**
 * Loads images from the CIFAR-10 Dataset. The string path points to a directory where the files data_batch_1.bin, etc. are stored.
 *
 * TODO: Implement loading of test images, and distinguish between training and test data
 */
class CifarLoader(path: String) {
  // We hardcode this because these are properties of the CIFAR-10 dataset.
  val width = 32
  val height = 32
  val channels = 3
  val size = channels * height * width
  val batchSize = 10000
  val nBatches = 5
  val nData = nBatches * batchSize

  val trainImages = new Array[Array[Float]](nData)
  val trainLabels = new Array[Float](nData)

  val testImages = new Array[Array[Float]](batchSize)
  val testLabels = new Array[Float](batchSize)

  val r = new Random()
  // val perm = Vector() ++ r.shuffle(1 to (nData - 1) toIterable)
  val indices = Vector() ++ (0 to nData - 1) toIterable
  val trainPerm = Vector() ++ r.shuffle(indices)
  val testPerm = Vector() ++ ((0 to batchSize) toIterable)

  def getListOfFiles(dir: String) : List[File] = {
    val d = new File(dir)
  	if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.getName().split('.').last == "bin").toList
  	} else {
      List[File]()
  	}
  }

  val fullFileList = getListOfFiles(path)

  val testFile = fullFileList.find(x => x.getName().split('/').last == "test_batch.bin").head

  val fileList = fullFileList diff List(testFile)

  for (i <- 0 to nBatches - 1) {
  	readBatch(fileList(i), i, trainImages, trainLabels, trainPerm)
  }
  readBatch(testFile, 0, testImages, testLabels, testPerm)

  val meanImage = new Array[Float](size)

  for (i <- 0 to nData - 1) {
  	for(j <- 0 to size - 1) {
  		meanImage(j) += trainImages(i)(j) / nData
  	}
  }

  subtractMean(trainImages)
  subtractMean(testImages)

  def subtractMean(images: Array[Array[Float]]) {
  	for(i <- 0 to images.length - 1) {
  		for(j <- 0 to size - 1) {
  			images(i)(j) -= meanImage(j)
  		}
  	}
  }

  def readBatch(file: File, batch: Int, images: Array[Array[Float]], labels: Array[Float], perm: Vector[Int]) {
    val buffer = new Array[Byte](1 + size);
    val inputStream = new FileInputStream(file);

    var i = 0
  	var nRead = inputStream.read(buffer)

  	while(nRead != -1) {
      assert(i < batchSize)
      labels(perm(batch * batchSize + i)) = (buffer(0) & 0xff) * 1.0F // convert to unsigned
      images(perm(batch * batchSize + i)) = new Array[Float](size)
      for(j <- 1 to size) {
        images(perm(batch * batchSize + i))(j - 1) = (buffer(j) & 0xff) * 1.0F // convert to unsigned
      }
      nRead = inputStream.read(buffer)
      i += 1
    }
  }
}
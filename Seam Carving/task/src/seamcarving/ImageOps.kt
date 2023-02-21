package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.sqrt

class Model {
    // objects properties
    private val msgList = mutableListOf<Messages>()

    //------------------------------------------------------------------------------------------------------------------
    // File operations: File -> Image and Image -> File
    //------------------------------------------------------------------------------------------------------------------
    fun getOriginalImage(path: String): BufferedImage? {
        val fileExists = File(path).run { exists() && isFile }
        val fileIsImage = path.substring(path.length - 4, path.length) in listOf(".jpg", ".png")
        if (fileExists) {
            if (fileIsImage) {
                return ImageIO.read(File(path))
            }
            // todo: set msg
        }
        // todo: set msg
        return null
    }

    fun saveImageFile(outputImage: BufferedImage, path: String) {
        ImageIO.write(outputImage, "png", File(path))
    }

    fun createImage(width: Int, height: Int): BufferedImage {
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    }
    //------------------------------------------------------------------------------------------------------------------

    //------------------------------------------------------------------------------------------------------------------
    // Core public methods. Get path, get energy matrix, get adjacency matrix
    //------------------------------------------------------------------------------------------------------------------
    fun getPath(
        energyMatrixIn: MutableList<MutableList<Double>>,
        workMatrixIn: MutableList<MutableList<Node>>,
        horizontalSeam: Boolean = false
    ): MutableList<Position>? {
        // 0. Transpose if horizontal seam
        var energyMatrix = energyMatrixIn
        var workMatrix = workMatrixIn
        if (horizontalSeam){
            energyMatrix = transpose(energyMatrix)
            workMatrix = transpose(workMatrixIn)
        }
        // 1. Loop at every row of the image and calculate the lowest energy neighbors (top to bottom)
        for (y in 0 until energyMatrix.size) {
            val yPrim = y - 1
            for (x in 0 until energyMatrix[y].size) {
                val currentNodeEnergy = energyMatrix[y][x]
                if (y == 0) {
                    workMatrix[y][x].minDistance = currentNodeEnergy
                } else {
                    for (xPrim in (x - 1).coerceAtLeast(0)..
                            (x + 1).coerceAtMost(energyMatrix[y].lastIndex)) {
                        val pretendingDistance = workMatrix[yPrim][xPrim].minDistance + currentNodeEnergy
                        if (pretendingDistance < workMatrix[y][x].minDistance) {
                            workMatrix[y][x].minDistance = pretendingDistance
                            workMatrix[y][x].nodePredecessor = workMatrix[yPrim][xPrim]
                        }
                    }
                }
            }
        }

        // 2. Traverse back from the lowest energy pixel from the bottom side to the top - get path
        val lastBestNode = workMatrix[workMatrix.lastIndex].minByOrNull { it.minDistance }
        return if (lastBestNode != null) {
            val seamPath = mutableListOf<Position>()
            var currentNode = lastBestNode!!
            while (true) {
                seamPath.add(currentNode.position)
                if (currentNode.nodePredecessor != null) {
                    currentNode = currentNode.nodePredecessor!!
                } else {
                    break
                }
            }
            seamPath
        } else {
            null
        }
    }

    fun drawSeam(image: BufferedImage, seamPath: MutableList<Position>): BufferedImage {
        val tempImage = imageDeepCopy(image)
        seamPath.forEach {
            tempImage.setRGB(it.x, it.y, Color.RED.rgb)
        }
        return tempImage
    }

    fun buildEnergyMatrix(image: BufferedImage): MutableList<MutableList<Double>> {
        val energyMatrix = mutableListOf<MutableList<Double>>()
        for (h in 0 until image.height) {
            energyMatrix.add(mutableListOf())
            for (w in 0 until image.width) {
                energyMatrix[h].add(calculateEnergy(h, w, image))
            }
        }
        return energyMatrix
    }

    fun buildWorkMatrix(image: BufferedImage): MutableList<MutableList<Node>> {
        val workMatrix = mutableListOf<MutableList<Node>>()
        for (h in 0 until image.height) {
            workMatrix.add(mutableListOf())
            for (w in 0 until image.width) {
                workMatrix[h].add(Node(Position(w, h)))
            }
        }
        return workMatrix
    }

    //------------------------------------------------------------------------------------------------------------------
    // Internal private methods
    //------------------------------------------------------------------------------------------------------------------
    private fun calculateEnergy(height: Int, width: Int, image: BufferedImage): Double {
        return sqrt(
            (calculateWidthComponent(height, width, image) + calculateHeightComponent(height, width, image)).toDouble()
        )
    }

    private fun calculateWidthComponent(height: Int, width: Int, image: BufferedImage): Int {
        val newWidth = width.coerceIn(1 until image.width - 1)
        val newHeight = height
        val leftPixelColor = Color(image.getRGB(newWidth - 1, newHeight))
        val rightPixelColor = Color(image.getRGB(newWidth + 1, newHeight))
        val redComp = leftPixelColor.red - rightPixelColor.red
        val greenComp = leftPixelColor.green - rightPixelColor.green
        val blueComp = leftPixelColor.blue - rightPixelColor.blue
        val result = redComp * redComp + greenComp * greenComp + blueComp * blueComp
        return result
    }

    private fun calculateHeightComponent(height: Int, width: Int, image: BufferedImage): Int {
        val newHeight = height.coerceIn(1 until image.height - 1)
        val newWidth = width
        val upperPixelColor = Color(image.getRGB(newWidth, newHeight - 1))
        val lowerPixelColor = Color(image.getRGB(newWidth, newHeight + 1))
        val redComp = upperPixelColor.red - lowerPixelColor.red
        val greenComp = upperPixelColor.green - lowerPixelColor.green
        val blueComp = upperPixelColor.blue - lowerPixelColor.blue
        val result = redComp * redComp + greenComp * greenComp + blueComp * blueComp
        return result
    }

    private fun imageDeepCopy(bi: BufferedImage): BufferedImage {
        val cm = bi.colorModel
        val isAlpha = cm.isAlphaPremultiplied
        val raster = bi.copyData(null)
        return BufferedImage(cm, raster, isAlpha, null)
    }
    //------------------------------------------------------------------------------------------------------------------

    private inline fun <reified T> transpose(xs: MutableList<MutableList<T>>): MutableList<MutableList<T>> {
        val cols = xs[0].size
        val rows = xs.size
        return MutableList(cols) { j ->
            MutableList(rows) { i ->
                xs[i][j]
            }
        }
    }

//    fun addMessage(message: String, msgCode: Int) {
//        // msgCode description
//        // 100 - 199 -> Info
//        // 500 - 599 -> Warning
//        // 600 - 699 -> Error
//        msgList.add(Messages(message, msgCode))
//    }

}

data class Messages(val message: String, val msgCode: Int)

data class Position(val x: Int, val y: Int)

data class Node(
    val position: Position,
    var minDistance: Double = Double.MAX_VALUE,
    var nodePredecessor: Node? = null
)
package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.sqrt


class Model {
    companion object {

    }

    private val msgList = mutableListOf<Messages>()

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

    fun createImage(width: Int, height: Int): BufferedImage {
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    }

    fun saveImageFile(outputImage: BufferedImage, path: String) {
        ImageIO.write(outputImage, "png", File(path))
    }

    fun getIntensityMatrix(image: BufferedImage): MutableList<MutableList<Int>> {
        return this.fromEnergyToIntensity(this.buildEnergyMatrix(image))
    }

    fun calculateShortestPath(
        energyMatrix: MutableList<MutableList<Double>>,
        adjacencyMatrix: List<MutableList<MutableList<Position>>>
    ): Unit {

    }

    fun generateAdjacencyMatrix(intensityMatrix: MutableList<MutableList<Int>>): List<MutableList<MutableList<Position>>> {
        val matrix = mutableListOf<MutableList<MutableList<Position>>>()
        val baseNode = mutableListOf<MutableList<Position>>(mutableListOf())
        for (y in 0 until intensityMatrix.size) {
            matrix.add(mutableListOf())
            // Base node adjacent to all nodes in the first vertical line
            baseNode[0].add(Position(0, y))
            for (x in 0 until intensityMatrix[0].size - 1) {
                val edges = mutableListOf<Position>()
                val minimumHeight = (y - 1).coerceAtLeast(0)
                val maximumHeight = (y + 1).coerceAtMost(intensityMatrix.size - 1)
                for (e in minimumHeight..maximumHeight) {
                    edges.add(Position(x + 1, e))
                }
                matrix[y].add(edges)
            }
            // All nodes in the last vertical line should point on the final node (marked with position Int.Max)
            matrix[y].add(mutableListOf(Position(Int.MAX_VALUE, Int.MAX_VALUE)))
        }
        matrix.add(baseNode)
        return matrix.toList()
    }

    fun testDijkstra(path: String): MutableList<Node> {
        val image = this.getOriginalImage(path)!!
        val intensityMatrix = this.getIntensityMatrix(image)
        val adjacencyMatrix = this.generateAdjacencyMatrix(intensityMatrix)
        return dijkstraAlgorithm(intensityMatrix, adjacencyMatrix)
    }

    fun dijkstraAlgorithm(
        intensityMatrix: List<MutableList<Int>>,
        adjacencyMatrix: List<MutableList<MutableList<Position>>>
    ): MutableList<Node> {
//    ): List<Position> {
        var baseNode = Node(Position(0, adjacencyMatrix.lastIndex), 0)
        val nodes = mutableListOf<Node>(baseNode)

        while (true) {
            // 1. Find first unprocessed node
            val currentNode =
                nodes.filter { !it.processed && it.nodeLocation.x != Int.MAX_VALUE }.minByOrNull { it.minDistance }

            if (currentNode != null) {

                // 2. Find all unprocessed neighbors
                val neighbors = adjacencyMatrix[currentNode.nodeLocation.y][currentNode.nodeLocation.x]

                // 3. Process neighbors
                neighbors.forEach {

                    // 3a. Get distance from current node to neighbor
                    val distanceToNeighbor =
                        if (it.y in intensityMatrix.indices && it.x in intensityMatrix[0].indices) {
                            intensityMatrix[it.y][it.x]
                        } else {
                            0
                        }

                    // 3b. Check if neighbor is already on the list of nodes to process
                    var neighbor = nodes.find { node -> node.nodeLocation.x == it.x && node.nodeLocation.y == it.y }
                    if (neighbor != null) {

                        // 3b'. Neighbor already on the list, let's check if we have better distance to it
                        if (!neighbor.processed && neighbor.minDistance > currentNode.minDistance + distanceToNeighbor) {
                            neighbor.minDistance = currentNode.minDistance + distanceToNeighbor
                        }
                    } else {

                        // 3b''. Neighbor not on the list yet, let's create new entry and add it to the list
                        neighbor =
                            Node(Position(it.x, it.y), currentNode.minDistance + distanceToNeighbor, false, currentNode)
                        nodes.add(neighbor)
                    }
                }

                // 4. All neighbors are considered -> this node is processed
                currentNode.processed = true

            } else {
                // 5. There is no more unprocessed nodes end of the story
                break
            }
        }
        return nodes

    }

    private fun fromEnergyToIntensity(energyMatrix: MutableList<MutableList<Double>>): MutableList<MutableList<Int>> {
        val maxEnergyValue = getMaxEnergyValue(energyMatrix)
        val intensityMatrix = mutableListOf<MutableList<Int>>()

        for (h in 0 until energyMatrix.size) {
            intensityMatrix.add(mutableListOf())
            for (w in 0 until energyMatrix[h].size) {
                intensityMatrix[h].add((255.0 * energyMatrix[h][w] / maxEnergyValue).toInt())
            }
        }
        return intensityMatrix
    }

    private fun getMaxEnergyValue(energyMatrix: MutableList<MutableList<Double>>): Double {
        return energyMatrix.maxOf { it.maxOrNull() ?: Double.MAX_VALUE }
    }


    private fun buildEnergyMatrix(image: BufferedImage): MutableList<MutableList<Double>> {
        val energyMatrix = mutableListOf<MutableList<Double>>()
        for (h in 0 until image.height) {
            energyMatrix.add(mutableListOf())
            for (w in 0 until image.width) {
                energyMatrix[h].add(calculateEnergy(h, w, image))
            }
        }
        return energyMatrix
    }

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

    inline fun <reified T> transpose(xs: Array<Array<T>>): Array<Array<T>> {
        val cols = xs[0].size
        val rows = xs.size
        return Array(cols) { j ->
            Array(rows) { i ->
                xs[i][j]
            }
        }
    }

    //    fun drawCross(image: BufferedImage): BufferedImage {
//
//        val processedImage = imageDeepCopy(image)
//        val graphics = processedImage.graphics
//        graphics.color = Color.red
//        graphics.drawLine(0, 0, processedImage.width - 1, processedImage.height - 1)
//        graphics.drawLine(processedImage.width - 1, 0, 0, processedImage.height - 1)
//        graphics.dispose()
//        return processedImage
//    }
//
//    fun drawNegative(image: BufferedImage): BufferedImage {
//        val processedImage = imageDeepCopy(image)
//        for (h in 0 until processedImage.height) {
//            for (w in 0 until processedImage.width) {
//                val color = Color(image.getRGB(w, h)).run { Color(255 - red, 255 - green, 255 - blue) }
//                processedImage.setRGB(w, h, color.rgb)
//            }
//        }
//        return processedImage
//    }
//    fun addMessage(message: String, msgCode: Int) {
//        // msgCode description
//        // 100 - 199 -> Info
//        // 500 - 599 -> Warning
//        // 600 - 699 -> Error
//        msgList.add(Messages(message, msgCode))
//    }
//
//    fun drawIntensity(image: BufferedImage): BufferedImage {
//        val processedImage = imageDeepCopy(image)
//        val energyMatrix = buildEnergyMatrix(processedImage)
//        val intensityMatrix = fromEnergyToIntensity(energyMatrix)
//
//        for (h in 0 until processedImage.height) {
//            for (w in 0 until processedImage.width) {
//                val intensity = intensityMatrix[h][w]
//                val color = Color(intensity, intensity, intensity)
//                processedImage.setRGB(w, h, color.rgb)
//            }
//        }
//        return processedImage
//    }

}

data class Messages(val message: String, val msgCode: Int)

data class Position(val x: Int, val y: Int)

data class Node(
    val nodeLocation: Position,
    var minDistance: Int = Int.MAX_VALUE,
    var processed: Boolean = false,
    var nodePredecessor: Node? = null
)
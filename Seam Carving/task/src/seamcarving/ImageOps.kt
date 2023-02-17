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

    fun addMessage(message: String, msgCode: Int) {
        // msgCode description
        // 100 - 199 -> Info
        // 500 - 599 -> Warning
        // 600 - 699 -> Error
        msgList.add(Messages(message, msgCode))
    }

    fun drawCross(image: BufferedImage): BufferedImage {

        val processedImage = deepCopy(image)
        val graphics = processedImage.graphics
        graphics.color = Color.red
        graphics.drawLine(0, 0, processedImage.width - 1, processedImage.height - 1)
        graphics.drawLine(processedImage.width - 1, 0, 0, processedImage.height - 1)
        graphics.dispose()
        return processedImage
    }

    fun drawNegative(image: BufferedImage): BufferedImage {
        val processedImage = deepCopy(image)
        for (h in 0 until processedImage.height) {
            for (w in 0 until processedImage.width) {
                val color = Color(image.getRGB(w, h)).run { Color(255 - red, 255 - green, 255 - blue) }
                processedImage.setRGB(w, h, color.rgb)
            }
        }
        return processedImage
    }

    fun drawIntensity(image: BufferedImage): BufferedImage {
        val processedImage = deepCopy(image)
        val energyMatrix = buildEnergyMatrix(processedImage)
        val intensityMatrix = fromEnergyToIntensity(energyMatrix)

        for (h in 0 until processedImage.height) {
            for (w in 0 until processedImage.width) {
                val intensity = intensityMatrix[h][w]
                val color = Color(intensity, intensity, intensity)
                processedImage.setRGB(w, h, color.rgb)
            }
        }
        return processedImage
    }

    private fun fromEnergyToIntensity(energyMatrix: MutableList<MutableList<Double>>): MutableList<MutableList<Int>>{
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

    fun calculateEnergy(height: Int, width: Int, image: BufferedImage): Double {
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

    private fun deepCopy(bi: BufferedImage): BufferedImage {
        val cm = bi.colorModel
        val isAlpha = cm.isAlphaPremultiplied
        val raster = bi.copyData(null)
        return BufferedImage(cm, raster, isAlpha, null)
    }

}

data class Messages(val message: String, val msgCode: Int)
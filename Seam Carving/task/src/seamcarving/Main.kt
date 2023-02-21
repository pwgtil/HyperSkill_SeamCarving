package seamcarving

fun main(args : Array<String>) {
    View.start(args)
}

class View {
    companion object {

        fun start(args : Array<String>) {

            val inputPath: String = args[1]
            val outputPath: String = args[3]

            val model = Model()
            val image = model.getOriginalImage(inputPath)
            if (image != null) {
                val energyMatrix = image.let { model.buildEnergyMatrix(it) }
                val workMatrix = image.let { model.buildWorkMatrix(it) }
                val path = model.getPath(energyMatrix, workMatrix, true)
                if (path != null) {
                    val imageWithSeam = model.drawSeam(image, path)
                    model.saveImageFile(imageWithSeam, outputPath)
                }
            }
        }
    }
}

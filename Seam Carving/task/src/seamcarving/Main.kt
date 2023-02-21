package seamcarving

fun main(args : Array<String>) {
    View.start(args)
}

class View {
    companion object {


//        const val MSG_ENTER_RECT_WIDTH = "Enter rectangle width:"
//        const val MSG_ENTER_RECT_HEIGHT = "Enter rectangle height:"
//        const val MSG_ENTER_OUTPUT_IMAGE_NAME = "Enter output image name:"

        fun start(args : Array<String>) {

            val width: Int
            val height: Int
            val inputPath: String = args[1]
            val outputPath: String = args[3]



            val model = Model()

            val test = model.testDijkstra(inputPath)
            println()

//            model.saveImageFile(model.drawIntensity(model.getOriginalImage(inputPath)!!), outputPath)

//            model.saveImageFile(model.drawCross(model.createImage(width, height)), path)

        }

    }
}

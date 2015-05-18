import org.apache.spark.{SparkConf, SparkContext}
import scopt.OptionParser
import techniques._
import utils.{SubTechniques, HDFSHandler}
import utils.Launcher._

/**
 * From Valentin with love on 21/04/15.
 */
object SparkCommander {

  val INPUT = "hdfs:///projects/temporal-profiles/data-generation/clean-1gram"
  val BASE_PROFILE = "hdfs:///projects/temporal-profiles/data-generation/baseProfile"

  private def createOutput(mode: String, words: Seq[String], technique: String, params: Seq[Double]): String = {
    s"hdfs:///projects/temporal-profiles/results/${
      if (mode != null) s"${mode}_"
      else ""
    }${words.mkString("-")}${
      if (params.nonEmpty) {
        s"_${technique.toLowerCase}_${
          params.mkString("-")
        }"
      } else {
        ""
      }
    }/"

  }


  /**
   * Arguments parsing representation class
   * @param words the words to search
   * @param technique the technique to use
   * @param parameters the parameter for that technique
   */
  private case class Config(mode: String = "", words: Seq[String] = Seq[String](), technique: String = "",
                            parameters: Seq[Double] = Seq[Double]())


  private val parser = new OptionParser[Config]("scopt") {
    head("SparkCommander", "1.0")

    opt[String]('m', "mode") optional() action { (mode, config) => config.copy(mode = mode)
    } text "optional mode of working, for comparing two words"
    opt[Seq[String]]('w', "words") valueName "<word1>,<word2>,..." action { (words, config) => config.copy(words = words)
    } text "The words you want to search"
    opt[String]('t', "technique") action { (technique, config) => config.copy(technique = technique)
    } text "The technique you want to use"
    opt[Seq[Double]]('p', "parameters") valueName "<param1>,<param2>..." optional() action { (parameters, config) => config.copy(parameters = parameters)
    } text "Optional parameters for this technique"
  }

  /**
   *
   * @param args must be in the format: -w word1,word2?,...  -t technique_name -p param1?,param2?,...
   */
  def main(args: Array[String]) = {
    val conf = new SparkConf().setAppName("SparkCommander").setMaster("yarn-cluster").set("num-executors", "25")

    @transient val sc = new SparkContext(conf)

    parser.parse(args, Config(mode = null, words = Seq(), technique = null, parameters = Seq())) match {
      case Some(Config(mode, words, technique, parameters)) =>
        val output = createOutput(mode, words, technique, parameters)

        val hdfs = new HDFSHandler(sc.hadoopConfiguration)
        // Create folder for results
        hdfs.createFolder(output)
        hdfs.close()

        val tech: Technique = technique match {
          // Add your technique methods here. All lowercase for the name pliz
          case "naivedifference" => NaiveComparisons.naiveDifferenceTopKScalingAverage
          case "naivedivision" => NaiveComparisons.naiveDivisionTopKScalingAverage
          case "naivedifferencesquared" => NaiveComparisons.naiveDifferenceSquaredTopKScalingAverage
          case "naivedivisionvar" => NaiveComparisons.naiveDivisionVarianceTopKScalingAverage
          case "inverse" => NaiveComparisons.naiveInverseDifference
          case "shift" => NaiveComparisons.naiveDifferenceScalingAverageWithShifting
          case "divergence" => Divergence.naiveDifferenceDivergence
          case "smarterdivergence" => SubTechniques.smarterDivergence
          case "peaks" => PeakComparison.peakComparisonWithMeanDerivative
          case "dtw" => DynamicTimeWrapping.dtwComparison
          case "dtwtopk" => DynamicTimeWrapping.dtwSimpleTopK
          case "dtwscaleavgtopk" => DynamicTimeWrapping.dtwComparisonScaleAvgTopK
          case "dtwscalemaxtopk" => DynamicTimeWrapping.dtwComparisonScaleMaxTopK
          case "peakstopk" => PeakComparison.peaksTopK

        }


        mode match {
          case "compare" => runCompare(words, INPUT, BASE_PROFILE, output, parameters.toList, tech, sc)
          case "findparams" => runParamsFinding(sc, INPUT, BASE_PROFILE)
          case "preprocessing" => runPreprocessing(sc, INPUT, BASE_PROFILE, output)
          case _ => runList(words, INPUT, BASE_PROFILE, output, parameters.toList, tech, sc)
        }
      //runCompare(words, INPUT, BASE_PROFILE, output, parameters.toList, tech, sc)

      case None => // Bad arguments
    }

    sc.stop()
  }
}

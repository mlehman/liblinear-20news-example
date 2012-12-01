package example

import java.io._
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.shingle.ShingleFilter
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.util.Version
import de.bwaldvogel.liblinear._

object NewsgroupExample {

  val dataSoureUrl = "http://qwone.com/~jason/20Newsgroups/20news-bydate.tar.gz"
  val dataDirName = "20news-bydate"
  val trainDirName = dataDirName + "-train"
  val testDirName = dataDirName + "-test"

  val C = 1.0 //Cost of constraints violation. (usually use 1 to 1000)
  val eps = 0.01 //Stopping criterion. (usually use 0.01)
  val featureCutOff = 2 //Minimum threshold for number of feature occurrences.
  val numOfFolds = 5 //Number of folds for cross-validation

  def main(args: Array[String]) {

    val dataDir = new File(dataDirName)
    verifyData(dataDir)

    val (trainingFeatures, trainingTargets, featureIntMap, targetIntMap) = processTrainingData(dataDir)

    val numTrainingExamples = trainingFeatures.size
    val numOfFeatures = featureIntMap.size

    val problem = new Problem();
    problem.l = numTrainingExamples
    problem.n = numOfFeatures
    problem.x = trainingFeatures
    problem.y = trainingTargets

    println("Training on " + numTrainingExamples + " examples with " + numOfFeatures + " features...")
    val param = new Parameter(SolverType.L2R_LR, C, eps)
    Linear.disableDebugOutput
    val results = new Array[Double](numTrainingExamples)
    Linear.crossValidation(problem, param, numOfFolds, results)

    printResults(targetIntMap.map(_.swap), trainingTargets.zip(results))

  }

  def verifyData(dataDir: File) {
    if (!dataDir.exists()) {
      println("ERROR: Unable to find data directory ' + dataDirName + '.")
      println("Download and place the unzipped 20 Newsgroups data set in the root of this project.")
      println("  " + dataSoureUrl)
      System.exit(-1)
    }
  }

  def processTrainingData(dataDir: File) = {
    val trainDir = new File(dataDir, trainDirName)
    val extractor = new PostFeatureExtractor

    val newsGroupDirs = trainDir.listFiles.filter(_.isDirectory)

    val traininingData = for {
      newsGroupDir <- newsGroupDirs
      val newsGroupName = newsGroupDir.getName
      post <- newsGroupDir.listFiles
    } yield extractor(post) -> newsGroupName

    val featureCounts = (for {
      (features, _) <- traininingData
      (feature, _) <- features
    } yield feature).groupBy(identity).mapValues(_.size)

    val featureIntMap = featureCounts.filter(_._2 > featureCutOff).keys.zipWithIndex.toMap
    val targetIntMap = newsGroupDirs.map(_.getName).zipWithIndex.toMap

    val trainingExamples = traininingData.map {
      case (features, target) =>
        val featureNodes = features.flatMap {
          case (feature, value) => featureIntMap.get(feature) match {
            case Some(i) => Some(new FeatureNode(i + 1, value))
            case None => None
          }
        }
        (featureNodes.sortBy(_.index).toArray[Feature], targetIntMap(target).toDouble)
    }

    val trainingFeatures = trainingExamples.map(_._1)
    val trainingTargets = trainingExamples.map(_._2)

    (trainingFeatures, trainingTargets, featureIntMap, targetIntMap)
  }

  def printResults(labels: Map[Int, String], results: Array[(Double, Double)]) {

    def printSection(s: String) {
      List("-" * 40, s, "-" * 40).foreach(println)
    }

    printSection("Summary")
    val numCorrect = results.filter(r => r._1 == r._2).size
    println("  Correct: " + numCorrect)
    println("Incorrect: " + (results.size - numCorrect))
    println(" Accuracy: " + numCorrect.toDouble / results.size)

    val groupedByTarget = results.groupBy(_._1)

    val labelCount = labels.size
    val labelWidth = labels.values.map(_.length).max + 2
    val labelFormat = "%" + labelWidth + "s %2d"
    val headerFormat = "%" + (labelWidth + 3) + "s"
    val countFormat = "%5d"

    printSection("Confusion Matrix")
    print(headerFormat.format("Classified ->"))
    (1 to labelCount).foreach {
      i => print(countFormat.format(i))
    }
    println()

    (0 until labelCount).foreach {
      rowIndex =>
        val categoryTotals = groupedByTarget(rowIndex)
        val countsByPrediction = categoryTotals.groupBy(_._2).mapValues(_.size).withDefaultValue(0)

        print(labelFormat.format(labels(rowIndex), rowIndex + 1))
        (0 until labelCount).foreach {
          j => print(countFormat.format(countsByPrediction(j)))
        }
        println()
    }
  }

}

class PostFeatureExtractor {

  def apply(post: File): Array[(String, Double)] = {
    val reader = new BufferedReader(new FileReader(post))
    val headers = extractHeaders(reader)
    val subjectFeatures = tokenize("subject", new StringReader(headers("subject")))
    val bodyFeatures = tokenize("body", reader)
    reader.close()
    (subjectFeatures ++ bodyFeatures).groupBy(identity).mapValues(_.size.toDouble).toArray
  }

  def extractHeaders(reader: BufferedReader) = {
    Stream.continually(reader.readLine()).takeWhile(_ != "").flatMap {
      l =>
        val i = l.indexOf(":")
        if (i > 0 && i < l.length - 1) Some(l.substring(0, i).toLowerCase -> l.substring(i + 2))
        else None
    }.toMap
  }

  def tokenize(field: String, reader: Reader) = {
    val analyzer = new StandardAnalyzer(Version.LUCENE_36)
    val tokenStream: TokenStream = analyzer.tokenStream(field, reader)
    val shingledStream = new ShingleFilter(tokenStream, 2)
    val charTermAttribute = shingledStream.addAttribute(classOf[CharTermAttribute]);

    val tokens = (Stream.continually {
      if (shingledStream.incrementToken()) {
        field + ":" + charTermAttribute.toString
      } else null
    }).takeWhile(_ != null).force

    shingledStream.close()
    analyzer.close()
    tokens.toArray
  }
}
package main.scala.contest

import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{Row,SQLContext}
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import scala.util.hashing.MurmurHash3

/**
 * Created by bodong on 2015/12/29.
 */
object contest {
  val conf = new SparkConf().setAppName("contest")
  val sc = new SparkContext(conf)
  val sqlContext = new SQLContext(sc)

  def praise(x:Row,i:Int):Int={
    if (x.isNullAt(i))    return 0
    x(i).toString.toInt
  }

  def verify_predict(model:RandomForestModel,f:String) :Unit ={
    val testF = sqlContext.read.json("s3n://lloyd-hadoop/contest_out/"+f)
    val testData = testF.map(x => LabeledPoint(praise(x,3),
      Vectors.dense(x(0).toString.toInt,MurmurHash3.stringHash(x(1).toString),
        MurmurHash3.stringHash(x(2).toString),MurmurHash3.stringHash(x(4).toString),
        x(5).toString.toInt)))
    // verify test result
    val labelAndPreds = testData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    labelAndPreds.saveAsTextFile("s3n://lloyd-hadoop/contest_out/verify/"+f )
    val testErr = labelAndPreds.filter(r => r._1 != r._2).count().toDouble / testData.count()
    println("Test Error for "+f+" = " + testErr)
    println(labelAndPreds.groupBy(x=>x).map(x=>x._1))
    println(labelAndPreds.groupBy(x=>x).count())
    println("Learned classification forest model:\n" + model.toDebugString)
  }
  def main(args: Array[String]): Unit = {
    // load data
    val data = sqlContext.read.json("s3n://lloyd-hadoop/contest_out/pt_1_24/")
    //0: view, 1:d_id, 2:p_id 3:praise,4:date 5:click
    val trainingData = data.map(x => LabeledPoint(praise(x,3),
      Vectors.dense(x(0).toString.toInt,
        MurmurHash3.stringHash(x(1).toString),
        MurmurHash3.stringHash(x(2).toString),
        MurmurHash3.stringHash(x(4).toString),
        x(5).toString.toInt)))

    val numClasses = 3
    // categoricalFeaturesInfo is empty, means all feature are continuous
    val categoricalFeaturesInfo = Map[Int, Int]()
    val numTrees = 100
    //auto calculate feature importance
    val featureSubsetStrategy = "auto"
    val impurity = "entropy"
    val maxDepth = 5
    val maxBins = 32
    //return RandomForestModel
    val model = RandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)
    model.save(sc, "s3n://lloyd-hadoop/contest_out/model")
    //val sameModel = RandomForestModel.load(sc, "myModelPath")

    verify_predict(model,"pt_25_27")
    verify_predict(model,"pt_28_30")
  }
}

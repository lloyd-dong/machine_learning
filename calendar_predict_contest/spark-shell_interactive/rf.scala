
object ml {

//the following 4 lines are for compile in activator
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.SparkContext._
val conf = new SparkConf()
val sc = new SparkContext(conf)


import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.sql.Row
import scala.util.hashing.MurmurHash3

def praise(x:Row,i:Int):Int={
	if (x.isNullAt(i)){
		return 0
	}
	return x(i).toString().toInt
}

def verify_predict(f:String) :Unit ={
    val testF = sqlContext.jsonFile("s3n://lloyd-hadoop/contest_out/"+f)
    val testData = testF.map(x => LabeledPoint(praise(x,3),Vectors.dense(x(0),x(1),x(2),x(4),x(5))))
    // 测试数据评价训练好的分类器并计算错误率
    val labelAndPreds = testData.map { point =>
    	val prediction = model.predict(point.features)
     		(point.label, prediction)
    }
    labelAndPreds.save("s3n://lloyd-hadoop/contest_out/verify/"+f,source="json" )

    //groupBy().count()
    val testErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / testData.count()
    println("Test Error for "+f+" = " + testErr)
    println("Learned classification forest model:\n" + model.toDebugString)

}
def main(args: Array[String]): Unit = {
	// 加载数据
	val data = sqlContext.jsonFile("s3n://lloyd-hadoop/contest_out/pt_1_24/")       
    //0: view, 1:d_id, 2:p_id 3:praise,4:date 5:click
    val trainingData = data.map(x => LabeledPoint(praise(x,3),Vectors.dense(x(0).toString().toInt,MurmurHash3.stringHash(x(1).toString),MurmurHash3.stringHash(x(2).toString),MurmurHash3.stringHash(x(4).toString),x(5).toString().toInt)))

    // 随机森林训练参数设置
    //分类数
    val numClasses = 3
    // categoricalFeaturesInfo 为空，意味着所有的特征为连续型变量
    val categoricalFeaturesInfo = Map[Int, Int]()
    //树的个数
    val numTrees = 100 
    //特征子集采样策略，auto 表示算法自主选取
    val featureSubsetStrategy = "auto" 
    //纯度计算
    val impurity = "entropy"
    //树的最大层次
    val maxDepth = 5
    //特征最大装箱数
    val maxBins = 32
    //训练随机森林分类器，trainClassifier 返回的是 RandomForestModel 对象
    val model = RandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
     numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)
 	// 将训练后的随机森林模型持久化
    model.save(sc, "s3n://lloyd-hadoop/contest_out/model")   
   
	verify_predict("pt_25_27")
	verify_predict("pt_28_30")          
    //加载随机森林模型到内存
    //val sameModel = RandomForestModel.load(sc, "myModelPath")
  }
}

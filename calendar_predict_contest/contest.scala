import sqlContext.implicits._

//PV file strutre 
/*
 0:dev_id:String,
 1:stat_date:String,
 2:stat_time:String, //no use dropped
 3:post_id:String,
 4:post_type:Int,  //no use, dropped
 5:stat_view:Int,  //sum
 6:stat_click:Int  //sum
*/
case class PV(dev_id:String,stat_date:String,post_id:String,stat_view:Int,stat_click:Int)
//in EMR, sql 1.3 uses DataFrame instead of SchemaRDD
//distinck 去掉重复
//007_0:2950338 line rdd.count
//007_0:2750063, duplicate:20,0275 
//007_0 dev_id no null
val pvf=sc.textFile("s3n://lloyd-hadoop/contest/pv/000007_0").distinct().map(_.split("\t")).filter(_(0).toString().trim().length >0)
//val simplifiedPV = pvf.map(x=>(x(0)+x(3)+x(1),Array(x(5),x(6))))
//val accumulativePV =simplifiedPV.reduceByKey(_(0)+_(0), _(1)+_(1))
val pvDF = pvf.map(p => PV(p(0),p(1),p(3),p(5).toInt,p(6).toInt)).toDF()
pvDF.registerTempTable("pv")

val pvT = sqlContext.sql("select pv.dev_id,pv.stat_date,pv.post_id,sum(pv.stat_view) as view_n, sum(pv.stat_click) as click_n from pv group by pv.dev_id,pv.post_id,pv.stat_date")

pvT.save(path="s3n://lloyd-hadoop/contest/cleanPV",source="json")

//----explore data----
val ptype = pvf.map(x=>(x(3),x(4).toInt)).distinct() //44553 <-2750063
val ptype = ptype.reduceByKey(_+_).filter(_._2>2) //count 1267 有些post_id相同，但是type 不同

val s1=sqlContext.sql("select max(stat_date), min(stat_date) from pv").collect
s1.foreach(println) 
//[20151129,20151101] in 007_0

case class Post(p_id:String,title:String,content:String) 
val pF =sc.textFile("s3n://lloyd-hadoop/contest/post_data.txt")
//count 83837 = .distinct(), no duplicate
val pf1=pF.map(_.split("\t")).map(x=>x(0)).distinct()
//still 83837, no post_id is duplicated
//so, the type could be ignored, cause it shows one post could apear at card and lizhi forum
//------------------------

//case class Train(dev_id:String,post_id:String,praise:Int,stat_date:String)
case class Train(dev_id:String,post_id:String,praise:Int,stat_date:String) 
val trainF =sc.textFile("s3n://lloyd-hadoop/contest/train.txt").distinct().map(_.split("\t")).filter(_(0).toString().trim().length >0)
//count 473773
//only get date, fist 8 char
val trainDF = trainF.map(p => Train(p(0),p(1),p(2).toInt,p(3).substring(0,8)).toDF()

trainDF.registerTempTable("train")

val joinSql = sqlContext.sql("select t.dev_id, t.post_id,t.praise, pv.stat_date, pv.stat_time,t.stat_time,pv.stat_view,pv.stat_click from train t inner join pv on t.post_id=pv.post_id and t.dev_id=pv.dev_id and t.stat_date=pv.stat_date order by t.stat_date limit 100").collect
joinSql.foreach(println)

val b=joinSql.sortBy(x =>x(0).toString()+x(1).toString())
b.foreach(println)

//------- practice -----------------
//filter by value, _._1 is the key
val a=sc.makeRDD(Array((2,1),(2,2),(3,1),(4,2))).reduceByKey(_+_).filter(_._2==3).collect 

def myReduce(pre:Array[Int], aft: Array[Int]): Array[Int]={
  	val a =new Array[Int](2)
  	a(0)=pre(0)+aft(0)
	a(1)=pre(1)+aft(1)
	return a
}
val a=sc.makeRDD(Array(("2,2",Array(1,2)),("2,2",Array(1,2)),("3,3",Array(1,2)),("4,4",Array(1,2)))).reduceByKey(myReduce)
a.collect 
// Array((3,3,Array(1, 2)), (2,2,Array(2, 4)), (4,4,Array(1, 2)))
val b=a.map(x=>Array(x._1.split(","),x._2(0),x._2(1))

//use " in sql
val s1=sqlContext.sql("select count(*) from pv where dev_id=\"\"").collect 

//save to s3 as CSV
val uniqePV=sqlContext.sql("select distinct * from pv").collect.saveAsTextFile("s3n://b/folderName")

//DataFrame
pvDF.printSchema
// root
// |-- name: string (nullable = false)
// |-- age: integer (nullable = true)

val s1 = sqlContext.sql("SELECT name FROM people WHERE age >= 13 AND age <= 19")
val s1_r=s1.collect //now s1_r is an Array
s1_r.foreach(println)
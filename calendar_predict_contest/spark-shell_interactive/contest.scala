import sqlContext.implicits._
//!!!!! aws confiure first to access s3n
val log=List("log start") //s3n://lloyd-hadoop/contest_out/log

/*
data: PV file strutre 
 0:dev_id:String,
 1:stat_date:String,
 2:stat_time:String, //no use dropped
 3:post_id:String,
 4:post_type:Int,  //no use, dropped
 5:stat_view:Int,  //sum
 6:stat_click:Int  //sum
*/

/*data: 重复. 
  007_0:2,950,338(rdd.count).distinct -> 2,750,063 = 20,0275(duplicated) 
  train data 无重复
  post 有重复吗？read post id.count ?= distinct().count
*/
//data: 缺失。No null dev_id in 007_0. But it is said 922 dev_ids are lost in other files.
/*data: 去噪音。
  type: pv 中 type 不同，但是post id相同的有几千条（007_0)。
    但是post文件中id不重复???
    说明同一个帖子可以发布在card 和 lizhi论坛中。那这个type就没有标志意义。

  Time: page view中的 time 与 train 中的 time 对应不上。所以只保留date 来做关联。
    todo:思考一下如何利用time.把pv.time与 train time 的差作为一个输入???
*/
//去了噪音后只有5个有用的字段

//click or view 都有值，没有全0字段
case class PV(dev_id:String,post_id:String,stat_date:String,stat_view:Int,stat_click:Int)
val pvf=sc.textFile("s3n://lloyd-hadoop/contest/pv/").distinct().map(_.split("\t")).filter(_(0).toString().trim().length >0)


// 去掉噪音字段, 0:dev_id, 1:post_id,2:date, 3:view, 4:click
val cleanPV= pvf.map(x => PV(x(0),x(3),x(1),x(5).toInt,x(6).toInt))

val pvDF = cleanPV.toDF()
pvDF.registerTempTable("pv")

//---------train data ---------
//Train 文件(0：dev_id, 1：post_id,2：praise，3：stat_date data+time)
case class Train(dev_id:String,post_id:String,stat_date:String,praise:Int,stat_time:String) 
//已经验证过无重复，dev_id 无空记录,
//only get date, fist 8 char
val trainRDD =sc.textFile("s3n://lloyd-hadoop/contest/train.txt").map(_.split("\t")).map(x => Train(x(0),x(1),x(3).substring(0,8),x(2).toInt,x(3).substring(8,14))) //474564
val trainDF = trainRDD.toDF()
trainDF.registerTempTable("train")

val t_maxTime =sqlContext.sql("select t.dev_id,t.post_id,t.stat_date, max(stat_time) as max_time from train t  group by dev_id,post_id,stat_date ")
t_maxTime.registerTempTable("t_maxTime")

val cleanT = sqlContext.sql("select t.dev_id,t.post_id,t.stat_date, praise from train t inner join t_maxTime m on t.dev_id=m.dev_id and t.post_id = m.post_id and t.stat_date=m.stat_date and t.stat_time = m.max_time")
//cleanT.count = 474292
cleanT.registerTempTable("cleanTrain")


val aggregatedPV = sqlContext.sql("select pv.dev_id,pv.post_id,pv.stat_date,sum(pv.stat_view) as view_n, sum(pv.stat_click) as click_n from pv group by pv.dev_id,pv.post_id,pv.stat_date")

aggregatedPV.registerTempTable("pvSum")

val joinPT= sqlContext.sql("select p.dev_id, p.post_id, p.stat_date, p.view_n,p.click_n,t.praise from pvSum p left join cleanTrain t on p.post_id=t.post_id and p.dev_id=t.dev_id and p.stat_date=t.stat_date")
joinPT.registerTempTable("pvT")

val pt_1_24 = sqlContext.sql("select * from pvT where stat_date <= \"20151124\"")
val pt_25_27 = sqlContext.sql("select * from pvT where stat_date > \"20151124\" and stat_date <= \"20151127\"")
val pt_28 = sqlContext.sql("select * from pvT where stat_date > \"20151127\"")

pt_1_24.save("s3n://lloyd-hadoop/contest_out/pt_1_24",source="json")
pt_25_27.save("s3n://lloyd-hadoop/contest_out/pt_25_27",source="json")
pt_28.save("s3n://lloyd-hadoop/contest_out/pt_28_30",source="json")

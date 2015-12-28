//tech spike

/* tech: DataFrame
	in EMR, spark-sql 1.3 uses DataFrame instead of SchemaRDD
	so no createSchemaRDD in sqlContext.
*/

DataFrame.save(path="s3n://lloyd-hadoop/contest/cleanPV",source="json")
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
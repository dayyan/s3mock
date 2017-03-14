package io.findify.s3mock

import java.util

import com.amazonaws.services.s3.model.{AmazonS3Exception, ListObjectsRequest, S3ObjectSummary}

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

/**
  * Created by shutty on 8/9/16.
  */
class ListBucketTest extends S3MockTest {
  "s3 mock" should "list bucket" in {
    s3.createBucket("foo")
    s3.listObjects("foo").getObjectSummaries.isEmpty shouldBe true
  }
  it should "list bucket with prefix" in {
    s3.createBucket("list")
    s3.putObject("list", "foo1", "xxx")
    s3.putObject("list", "foo2", "xxx")
    s3.putObject("list", "xfoo3", "xxx")
    val list = s3.listObjects("list", "foo").getObjectSummaries.asScala.toList
    list.map(_.getKey).forall(_.startsWith("foo")) shouldBe true
  }
  it should "list objects in subfolders with prefix" in {
    s3.createBucket("list2")
    s3.putObject("list2", "one/foo1/1", "xxx")
    s3.putObject("list2", "one/foo2/2", "xxx")
    s3.putObject("list2", "one/foo2/3", "xxx")
    s3.putObject("list2", "one/foo2/4", "xxx")
    s3.putObject("list2", "one/xfoo3", "xxx")
    val ol = s3.listObjects("list2", "one/f").getObjectSummaries.asScala.toList
    ol.size shouldBe 4
    ol.map(_.getKey).forall(_.startsWith("one/foo")) shouldBe true
  }
  it should "return empty list if prefix is incorrect" in {
    s3.createBucket("list3")
    s3.putObject("list3", "one/foo1", "xxx")
    s3.putObject("list3", "one/foo2", "xxx")
    s3.putObject("list3", "one/xfoo3", "xxx")
    s3.listObjects("list3", "qaz/qax").getObjectSummaries.asScala.isEmpty shouldBe true

  }
  it should "return keys with valid keys (when no prefix given)" in {
    s3.createBucket("list4")
    s3.putObject("list4", "one", "xxx")
    val summaries: util.List[S3ObjectSummary] = s3.listObjects("list4").getObjectSummaries
    summaries.size() shouldBe 1
    val summary = summaries.get(0)
    summary.getBucketName shouldBe "list4"
    summary.getKey shouldBe "one"
    summary.getSize shouldBe 3
    summary.getStorageClass shouldBe "STANDARD"

    val returnedKey = summaries.last.getKey
    s3.getObject("list4", returnedKey).getKey shouldBe "one"
  }

 it should "produce NoSuchBucket if bucket does not exist" in {
    val exc = intercept[AmazonS3Exception] {
      s3.listObjects("aws-404", "qaz/qax")
    }
    exc.getStatusCode shouldBe 404
    exc.getErrorCode shouldBe "NoSuchBucket"
  }

  it should "obey delimiters && prefixes v1" in {
    s3.createBucket("list5")
    s3.putObject("list5", "sample.jpg", "xxx")
    s3.putObject("list5", "photos/2006/January/sample.jpg", "yyy")
    s3.putObject("list5", "photos/2006/February/sample2.jpg", "zzz")
    s3.putObject("list5", "photos/2006/February/sample3.jpg", "zzz")
    s3.putObject("list5", "photos/2006/February/sample4.jpg", "zzz")
    val req1 = new ListObjectsRequest()
    req1.setBucketName("list5")
    req1.setDelimiter("/")
    val list1  = s3.listObjects(req1)
    val summaries1 = list1.getObjectSummaries.map(_.getKey).toList
    list1.getCommonPrefixes.asScala.toList shouldBe List("photos/")
    summaries1 shouldBe List("sample.jpg")
  }
  it should "obey delimiters && prefixes v2" in {
    s3.createBucket("list5")
    s3.putObject("list5", "sample.jpg", "xxx")
    s3.putObject("list5", "photos/2006/January/sample.jpg", "yyy")
    s3.putObject("list5", "photos/2006/February/sample2.jpg", "zzz")
    s3.putObject("list5", "photos/2006/February/sample3.jpg", "zzz")
    s3.putObject("list5", "photos/2006/February/sample4.jpg", "zzz")
    val req2 = new ListObjectsRequest()
    req2.setBucketName("list5")
    req2.setDelimiter("/")
    req2.setPrefix("photos/2006/")
    val list2  = s3.listObjects(req2)
    val summaries2 = list2.getObjectSummaries.map(_.getKey).toList
    list2.getCommonPrefixes.asScala.toList shouldBe List("photos/2006/February/", "photos/2006/January/")
    summaries2 shouldBe List("photos/2006/")
  }
}

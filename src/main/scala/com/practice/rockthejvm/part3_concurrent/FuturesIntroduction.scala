package com.practice.rockthejvm.part3_concurrent

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

//noinspection ScalaWeakerAccess,TypeAnnotation
object FuturesIntroduction {

  def calculateMeaningOfLife(): Int = {
    // simulate long compute
    Thread.sleep(1000)
    42
  }

  // thread pool (Java-specific)
  val executor = Executors.newFixedThreadPool(4)
  // thread pool (Scala-specific)
  given executionContext: ExecutionContext = ExecutionContext.fromExecutorService(executor)

  // a future = an async computation that will finish at some point
  // given executionContext will be passed here
  val aFuture: Future[Int] = Future.apply(calculateMeaningOfLife())

  // Option[Try[Int]], because
  // - we don't know if we have a value
  // - if we do, that can be a failed computation
  // inspect the value of the future RIGHT NOW
  val futureInstantResult: Option[Try[Int]] = aFuture.value

  // callbacks
  aFuture.onComplete {
    case Success(value) => println(s"I've completed with the meaning of life: $value")
    case Failure(ex)    => println(s"My async computation failed: $ex")
  } // on SOME other thread

  def main(args: Array[String]): Unit = {
    println(futureInstantResult)
    Thread.sleep(2000)
    executor.shutdown()
  }
}

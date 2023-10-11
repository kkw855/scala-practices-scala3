package com.practice.rockthejvm.part3_concurrent

import java.util.concurrent.Executors

//noinspection ScalaWeakerAccess,TypeAnnotation
object ParallelProgrammingOnTheJVM {

  def basicThreads(): Unit = {
    val runnable = new Runnable {
      override def run(): Unit = {
        println("waiting...")
        Thread.sleep(2000)
        println("running on some thread")
      }
    }

    // threads on JVM
    val aThread = new Thread(runnable)
    // one-to-one mapping with an operating system thread
    aThread.start()
    // JVM thread == OS thread (soon to change via Project Loom)

    aThread.join() // block main JVM thread until thread finishes
  }

  // order of operations is NOT guaranteed
  // different run = different results!
  def orderOfExecution(): Unit = {
    val threadHello   = new Thread(() => (1 to 5).foreach(_ => println("hello")))
    val threadGoodbye = new Thread(() => (1 to 5).foreach(_ => println("goodbye")))
    threadHello.start() // start method is a non-blocking operation
    threadGoodbye.start()
  }

  // executors
  def demoExecutors(): Unit = {
    val threadPool = Executors.newFixedThreadPool(4)
    // submit a computation
    threadPool.execute(() => println("something in the thread pool"))

    threadPool.execute { () =>
      Thread.sleep(1000)
      println("done after one second")
    }

    threadPool.execute { () =>
      Thread.sleep(1000)
      println("almost done")
      Thread.sleep(1000)
      println("done after 2 seconds")
    }

    threadPool.shutdown()
    // threadPool.execute(() => println("this should NOT appear")) // should throw an exception in the calling thread
  }

  def main(args: Array[String]): Unit = {
    demoExecutors()
  }
}

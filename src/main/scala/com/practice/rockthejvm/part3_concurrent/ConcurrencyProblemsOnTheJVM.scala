package com.practice.rockthejvm.part3_concurrent

import scala.annotation.tailrec

//noinspection ScalaWeakerAccess,TypeAnnotation
object ConcurrencyProblemsOnTheJVM {

  def runInParallel(): Unit = {
    var x = 0

    val thread1 = new Thread(() => {
      x = 1
    })

    val thread2 = new Thread(() => {
      x = 2
    })

    thread1.start()
    thread2.start()

    println(x) // race condition
  }

  case class BankAccount(var amount: Int)

  def buy(bankAccount: BankAccount, thing: String, price: Int): Unit = {
    /*
      involves 3 steps:
      - read old value
      - computes result
      - write new value
     */
    bankAccount.amount -= price // not atomic, there are three steps
  }

  def buySafe(bankAccount: BankAccount, thing: String, price: Int): Unit = {
    // does not allow multiple threads to run the critical section AT THE SAME TIME
    bankAccount.synchronized {
      bankAccount.amount -= price // critical section
    }
  }

  /*
    Example race condition:
    thread1 (shoes)
      - reads amount 50000
      - computes result 50000 - 3000 = 47000
    thread2 (iPhone)
      - reads amount 50000
      - computes result 50000 - 4000 = 46000
    thread1 (shoes)
      - write amount 47000
    thread2 (iPhone)
      - write amount 46000
   */
  def demoBankingProblem(): Unit = {
    (1 to 1000).foreach { _ =>
      val account = BankAccount(50000)
      val thread1 = new Thread(() => buy(account, "shoes", 3000))
      val thread2 = new Thread(() => buy(account, "iPhone", 4000))
      thread1.start()
      thread2.start()
      thread1.join()
      thread2.join()
      if (account.amount != 43000) println(s"AHA! I've just broken the bank: ${account.amount}")
    }
  }

  /*
    Exercise 1
    create "inception threads"
      thread 1
        -> thread 2
            -> thread 3
                ....
      each thread prints "hello from thread $i"
      Print all messsages IN REVERSE ORDER
   */
  def inceptionThreads(maxThreads: Int, i: Int = 1): Thread =
    new Thread(() => {
      if (i < maxThreads) {
        val newThread = inceptionThreads(maxThreads, i + 1)
        newThread.start()
        newThread.join()
      }
      println(s"Hello from thread $i")
    })

  /*
    Exercise 2
    What's the max/min value of x?
   */
  def minMax(): Unit = {
    var x   = 0
    var min = 1
    var max = 1
    val threads = (1 to 100).map(_ =>
      new Thread(() => {
        x.synchronized {
          val next = x + 1
          min = Math.min(min, next)
          max = Math.max(max, next)
          x = next
        }
      })
    )
    threads.foreach(_.start())
    println(s"$min, $max")
  }

  /*
    Exercise 3
    sleep fallacy
   */
  def demoSleepFallacy(): Unit = {
    var message = ""
    val awesomeThread = new Thread(() => {
      Thread.sleep(1000)
      message = "Scala is awesome"
    })

    message = "Scala sucks"
    awesomeThread.start()
    Thread.sleep(1001)

    // solution: join the worker thread
    awesomeThread.join()

    println(message)
  }

  def main(args: Array[String]): Unit = {
    inceptionThreads(50).start()
    // minMax()
    demoSleepFallacy()
  }
}

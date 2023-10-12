package com.practice.rockthejvm.part3_concurrent

object JVMThreadCommunicationLevel1 {
  def main(args: Array[String]): Unit = {
    ProdConsV2.start()
  }
}

// example: the producer-consumer problems
class SimpleContainer {
  private var value: Int = 0

  def isEmpty: Boolean =
    value == 0

  def set(newValue: Int): Unit =
    value = newValue

  def get: Int = {
    val result = value
    value = 0
    result
  }
}

// Producer-Consumer part 1: one producer, one consumer
object ProdConsV1 {
  def start(): Unit = {
    val container = new SimpleContainer

    val consumer = new Thread(() => {
      println("[consumer] waiting...")
      // busy wating
      while (container.isEmpty) {
        println("[consumer] waiting for a value..")
      }

      println(s"[consumer] I have consumed a value: ${container.get}")
    })

    val producer = new Thread(() => {
      println("[producer] computing...")
      Thread.sleep(500)
      val value = 42
      println(s"[producer] I am producing, after LONG work, the value $value")
      container.set(value)
    })

    consumer.start()
    producer.start()
  }
}

// wait -> notify
object ProdConsV2 {
  def start(): Unit = {
    val container = new SimpleContainer

    val consumer = new Thread(() => {
      println("[consumer] waiting...")

      // block all other threads trying to "lock" this object
      container.synchronized {
        // thread-safe code
        if (container.isEmpty)
          container.wait() // release the lock + suspend the thread indefinitely

        // require the lock here
        // continue execution
        println(s"[consumer] I have consumed a value: ${container.get}")
      }
    })

    val producer = new Thread(() => {
      println("[producer] computing...")
      Thread.sleep(500)
      val value = 42

      container.synchronized {
        println(s"[producer] I am producing, after LONG work, the value $value")
        container.set(value)
        container.notify() // awaken ONE suspended thread on this object
      }                    // release the lock
    })

    consumer.start()
    producer.start()
  }
}

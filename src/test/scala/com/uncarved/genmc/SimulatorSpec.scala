package com.uncarved.genmc

import org.specs2.mutable._

class SimulatorSpec extends Specification {
  "Simulator" should {
    "be able to calculate pi by shooting bullets at a unit circle inside a unit square" in {
      type path = (Double, Double)
      type seed = Int
      type result = Int

      class PGen extends PathGenerator[seed, path] {
        val gen = new scala.util.Random
      
        def apply(s: seed) : path = {
          gen.setSeed(s)
          val x = gen.nextDouble()
          val y = gen.nextDouble()

          x->y
        }
      }

      class Eval extends PathEvaluator[path, result] {
        import math.pow
        var sum = 0

        def apply(p: path) = {
          val r = p match {
            case (x, y) => if((pow(x,2) + pow(y,2)) <= 1.0) 1 else 0
          }

          sum = sum + r
        }

        def result = sum
      }

      val sim = new Simulator[seed, path, result, PGen, Eval](new PGen, new Eval)
      val npaths = 20000000
      
      val mypi = sim.run(0 to npaths-1).sum.toDouble / npaths.toDouble * 4.0
      math.abs(math.Pi-mypi) must beLessThan(0.001)
    }
  }
}


package com.uncarved.genmc

import org.specs2.mutable._

class SimulatorSpec extends Specification {
  "Simulator" should {
    "be able to calculate pi by shooting bullets at a unit circle inside a unit square" in {
      type path = (Double, Double)
      type seed = Long
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
        def apply(p: path) : result = 
          p match {
            case (x, y) => 
              val len = math.sqrt(math.pow((x*2-1),2) + math.pow((y*2-1),2))

              if(len <= 1) 1 else 0
          }
      }

      val sim = new Simulator[seed, path, result, PGen, Eval](new PGen, new Eval)
      val npaths : Long = 10000000
      
      val mypi = sim.run(0 to npaths-1).foldLeft[Long](0)((b,a)=>b+a) / npaths
      math.abs(math.Pi-mypi) must beLessThan 0.001
    }

}


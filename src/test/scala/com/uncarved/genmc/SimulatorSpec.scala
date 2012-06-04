package com.uncarved.genmc

import org.specs2.mutable._

class SimulatorSpec extends Specification {
  "Simulator" should {
    "be able to calculate pi by shooting bullets at a unit circle inside a unit square" in {
      import math.pow
      val randGen = new scala.util.Random
      
      def genPath(s: Int) = {
        randGen.setSeed(s)
        val x = randGen.nextDouble()
        val y = randGen.nextDouble()

        x->y
      }

      def evalPath(p: (Double, Double)) = 
        p match {
          case (x, y) => if((pow(x,2) + pow(y,2)) <= 1.0) 1 else 0
        }

      val sim = new Simulator[Int, (Double, Double), Int, Double](
        genPath,
        evalPath,
        (a: Double, r: Int) => a + r
      )
      val npaths = 20000000
      
      val mypi = sim.run(0 to npaths-1, 0.) / npaths.toDouble * 4.0
      math.abs(math.Pi-mypi) must beLessThan(0.001)
    }
  }
}


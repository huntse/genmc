package com.uncarved.genmc

import org.specs2.mutable._

class SimulatorSpec extends Specification {
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

  "Simulator" should {
    "be able to calculate pi by shooting bullets at a unit quarter circle" in {
      val sim = Simulator(
        genPath,
        evalPath,
        (a: Double, r: Int) => a + r
      )
      val npaths = 20000000
      
      val mypi = sim.run(0 to npaths-1, 0.) / npaths.toDouble * 4.0
      math.abs(math.Pi-mypi) must beLessThan(0.0025)
    }
  }

  "ParallelMC" should {
    "be able to calculate pi by shooting bullets at a unit quarter circle (in parallel)" in {
      val sim = new ParallelMC[Int, (Double, Double), Int, Double](
        8,
        10000,
        10,
        genPath,
        evalPath,
        (a: Double, r: Int) => a + r,
        (a: Double, p: Double) => a + p
      )
      val npaths = 20000000
      
      val mypi = sim.run(0 to npaths-1, 0.) / npaths.toDouble * 4.0
      math.abs(math.Pi-mypi) must beLessThan(0.0025)
    }
  }

  "be able to flip coins like a beast" in {
    val sim = new ParallelMC[Int, Boolean, Int, Int](
      8,
      10000,
      10,
      { s: Int => randGen.setSeed(s); randGen.nextBoolean() },
      { p: Boolean => if(p) 1 else 0 },
      _+_,
      _+_
    )

    val npaths = 2000000
    
    val boutHalf = sim.run(0 to npaths-1, 0) / npaths.toDouble
    math.abs(boutHalf-0.5) must beLessThan(0.005)

  }
}


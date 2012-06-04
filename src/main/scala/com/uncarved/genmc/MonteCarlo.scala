package com.uncarved.genmc

class Simulator[
  Seed, 
  Path,
  Result,
  Accumulator
](
  val pgen: Seed => Path,                           //A path generator
  val eval: Path => Result,                         //A path evaluator
  val reduce: (Accumulator, Result) => Accumulator  //A reduction func
)
{
  def run(seeds: Seq[Seed], initial: Accumulator) : Accumulator = 
    seeds.foldLeft(initial) { 
      (a: Accumulator, s: Seed) => 
        reduce(a, eval(pgen(s)))
      }
}


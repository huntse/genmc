package com.uncarved.genmc

trait PathGenerator[Seed, Path] {
  def apply(s: Seed) : Path
}

trait PathEvaluator[Path, Result] {
  def apply(p: Path) : Unit
  def result : Result
}

class Simulator[
  Seed, 
  Path,
  Result,
  PathGen<:PathGenerator[Seed, Path], 
  Eval<:PathEvaluator[Path, Result]
](
  val pgen: PathGen,  //A path generator
  val eval: Eval)     //A path evaluator
{
  def run(seeds: Seq[Seed]) : Eval = {
    seeds foreach { s: Seed => eval(pgen(s)) }

    eval
  }
}


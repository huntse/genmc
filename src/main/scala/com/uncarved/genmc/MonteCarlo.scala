package com.uncarved.genmc

import akka.actor._
import akka.dispatch.Await
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import akka.util.duration._
import akka.util.Duration
import akka.util.Timeout

/** The heart of any Monte Carlo framework is of course the Simulator.
  * Here we split the simulator into three functions.
  *
  * @param pgen Generates a Path given some Seed
  * @param eval Evaluates a single Path
  * @param reduce Accumulates a number of the results of evaluation
  *
  */
class Simulator[
  Seed,                                             
  Path,
  Result,
  Accumulator
] private(
  val pgen: Seed => Path,                           //A path generator
  val eval: Path => Result,                         //A path evaluator
  val reduce: (Accumulator, Result) => Accumulator  //A reduction func
)
{
  /** run a simulation
    * @param seeds inputs to the path generator
    * @param initial the start value for the reduction operation
    */
  def run(seeds: Seq[Seed], initial: Accumulator) : Accumulator = 
    seeds.foldLeft(initial) { 
      (a: Accumulator, s: Seed) => 
        reduce(a, eval(pgen(s)))
      }
}


/** The heart of any Monte Carlo framework is of course the Simulator.
  * Here we split the simulator into three functions.
  *
  * @param pgen Generates a Path given some Seed
  * @param eval Evaluates a single Path
  * @param reduce Accumulates a number of the results of evaluation
  *
  * This companion object is simply there to make it simpler to create
  * a simulator instance without having to specify a lot of redundant
  * type information that can already be deduced from the types of the 
  * three functions.
  */
object Simulator {
  /** Create a simulator given the three functions.
    *
    * @param pgen Generates a Path given some Seed
    * @param eval Evaluates a single Path
    * @param reduce Accumulates a number of the results of evaluation
    */
  def apply[Seed, Path, Result, Accumulator](
    pgen: Seed => Path,                           //A path generator
    eval: Path => Result,                         //A path evaluator
    reduce: (Accumulator, Result) => Accumulator  //A reduction func
  ) = new Simulator[Seed, Path, Result, Accumulator](pgen, eval, reduce)
}

/** A parallel Monte Carlo simulator
  *
  * @param nWorkers How many worker threads to run
  * @param chunkSize Number of paths to distribute as a single job
  * @param calcTimeout How long to wait for a result (in seconds)
  * @param pgen Generates a Path given some Seed
  * @param eval Evaluates a single Path
  * @param reduce Accumulates a number of the results of evaluation
  *
  */
class ParallelMC[
  Seed,                                             
  Path,
  Result,
  Accumulator
] private (
  val nrOfWorkers: Int,                             //number of worker threads
  val chunkSize:   Int,                             //num of paths in a chunk
  val calcTimeout: Int,                             //A calculation timeout (in secs)
  val pgen: Seed => Path,                           //A path generator
  val eval: Path => Result,                         //A path evaluator
  val accumulate: (Accumulator, Result) => Accumulator,    
  val reduce: (Accumulator, Accumulator)  => Accumulator   
)
{
  /** Base for actor messages */
  sealed trait MCMessage

  /** signal to begin a simulation */
  case class RunSimulation(seeds: Seq[Seed]) extends MCMessage

  /** unit of work */
  case class Work(seeds: Seq[Seed]) extends MCMessage

  /** result of one unit of work */
  case class Done(value: Accumulator) extends MCMessage

  /** worker class used to run simulation chunks in parallel */
  class Worker(val initial: Accumulator) extends Actor {
    private[this] val simulator = 
      Simulator(pgen, eval, accumulate)

    /** message handler for worker class */
    def receive = {
      case Work(seeds) =>
        val res = simulator.run(seeds, initial)
        sender ! Done(res)
    }
  }

  /** coordinates workers running simulation chunks, sending pieces to them,
    * receiving results and reducing them. 
    * @param initial The starting state of the accumulator object.
    */
  class Master(initial: Accumulator) extends Actor {
    /** worker pool.  Jobs are given out on a round robin basis */
    private[this] val workerRouter = context.actorOf(
      Props(new Worker(initial)).withRouter(
        RoundRobinRouter(nrOfWorkers)),
        name = "workerRouter"
      )

    /** our accumulator */
    private[this] var accum = initial

    /** the number of outstanding jobs */
    private[this] var outstanding = 0
     
    /** a reference to our caller so we can send the results when done */
    private[this] var parent : Option[ActorRef] = None

    /** the message handler for this master class */
    def receive = {
      case RunSimulation(seeds) =>
        val nSplits = seeds.length / chunkSize
        outstanding = nSplits
        var toDo = seeds
        for(i<- 0 until nSplits) {
          val (thisChunk, res) = toDo splitAt chunkSize
          workerRouter ! Work(thisChunk)
          toDo = res
        }
        parent = Some(sender)

      case Done(value) => 
        accum = reduce(accum, value)
        outstanding = outstanding - 1
        if(outstanding == 0) {
          //All jobs are done.  Send the result and shut down the workers
          parent.get ! accum
          context.stop(self)
        }
    }
  }

  /** run a simulation */
  def run(seeds: Seq[Seed], initial: Accumulator) = {
    val system = ActorSystem("GenMCSystem")
    val master = system.actorOf(Props(new Master(initial)), name = "master")
    implicit val timeout = Timeout(calcTimeout seconds)
    val future = master ? RunSimulation(seeds)
    
    val result = Await.result(future, timeout.duration).asInstanceOf[Accumulator]
    system.shutdown()

    result
  }
}

/** Companion object to simplify the creation of parallel Monte Carlo 
  * simulators given the appropriate parameters.
  */
object ParallelMC {
  /** Creates parallel Monte Carlo simulators given the appropriate parameters.
    *
    * @param nWorkers How many worker threads to run
    * @param chunkSize Number of paths to distribute as a single job
    * @param calcTimeout How long to wait for a result (in seconds)
    * @param pgen Generates a Path given some Seed
    * @param eval Evaluates a single Path
    * @param reduce Accumulates a number of the results of evaluation
    *
    */
  def apply[Seed, Path, Result, Accumulator](
      nrOfWorkers: Int,                             //number of worker threads
      chunkSize:   Int,                             //num of paths in a chunk
      calcTimeout: Int,                             //A calculation timeout (in secs)
      pgen: Seed => Path,                           //A path generator
      eval: Path => Result,                         //A path evaluator
      accumulate: (Accumulator, Result) => Accumulator,    
      reduce: (Accumulator, Accumulator)  => Accumulator   
    ) =
    new ParallelMC[Seed, Path, Result, Accumulator](
      nrOfWorkers,
      chunkSize,
      calcTimeout,
      pgen,
      eval,
      accumulate,
      reduce
    )
}

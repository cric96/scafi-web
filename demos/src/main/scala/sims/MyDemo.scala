package sims

import it.unibo.scafi.simulation.gui._
import it.unibo.scafi.simulation.gui.model._
import it.unibo.scafi.simulation.gui.BasicSpatialIncarnation.Builtins._
import it.unibo.scafi.simulation.gui.BasicSpatialIncarnation.ID
import it.unibo.scafi.simulation.gui.model.implementation.SensorEnum

//import it.unibo.scafi.simulation.gui.BasicSpatialIncarnation.{AggregateProgram => _, _}

object BasicDemo extends Launcher {
  // Configuring simulation
  Settings.Sim_ProgramClass = "sims.Basic" // starting class, via Reflection
  Settings.ShowConfigPanel = false // show a configuration panel at startup
  Settings.Sim_NbrRadius = 0.15 // neighbourhood radius
  Settings.Sim_NumNodes = 100 // number of nodes
  launch()
}

class Basic extends AggregateProgram {
  override def main() = rep(0)(_ + 1) // the aggregate program to run
}

object ChannelDemo extends Launcher {
  // Configuring simulation
  Settings.Sim_ProgramClass = "sims.Channel" // starting class, via Reflection
  Settings.ShowConfigPanel = false // show a configuration panel at startup
  Settings.Sim_NbrRadius = 0.1 // neighbourhood radius
  Settings.Sim_NumNodes = 200 // number of nodes
  Settings.Led_Activator = (b: Any) => b.asInstanceOf[Boolean]
  Settings.To_String = (b: Any) => ""
  launch()
}

trait SensorDefinitions { self: AggregateProgram =>
  def sense1 = sense[Boolean](SensorEnum.SENS1.name)
  def sense2 = sense[Boolean](SensorEnum.SENS2.name)
  def sense3 = sense[Boolean](SensorEnum.SENS3.name)
  def nbrRange() = nbrvar[Double]("nbrRange") * 100
}

trait BlockG { self: AggregateProgram with SensorDefinitions =>

  def G[V: OrderingFoldable](source: Boolean, field: V, acc: V => V, metric: => Double): V =
    rep((Double.MaxValue, field)) { dv =>
      mux(source) {
        (0.0, field)
      } {
        minHoodPlus {
          val (d, v) = nbr {
            (dv._1, dv._2)
          }
          (d + metric, acc(v))
        }
      }
    }._2

  def G2[V: OrderingFoldable](source: Boolean)(field: V)(acc: V => V)(metric: => Double = nbrRange): V =
    rep((Double.MaxValue, field)) { case (d,v) =>
      mux(source) { (0.0, field) } {
        minHoodPlus { (nbr{d} + metric, acc(nbr{v})) }
      }
    }._2

  def distanceTo(source: Boolean): Double =
    G2(source)(0.0)(_ + nbrRange)()

  def broadcast[V: OrderingFoldable](source: Boolean, field: V): V =
    G2(source)(field)(v=>v)()

  def distanceBetween(source: Boolean, target: Boolean): Double =
    broadcast(source, distanceTo(target))
}

class Channel extends AggregateProgram  with SensorDefinitions with BlockG {

  def channel(source: Boolean, target: Boolean, width: Double): Boolean =
    distanceTo(source) + distanceTo(target) <= distanceBetween(source, target) + width

  override def main() = branch(sense3){false}{channel(sense1, sense2, 1)}
}

object CollectionDemo extends Launcher {
  // Configuring simulation
  Settings.Sim_ProgramClass = "sims.Collection" // starting class, via Reflection
  Settings.ShowConfigPanel = false // show a configuration panel at startup
  Settings.Sim_NbrRadius = 0.15 // neighbourhood radius
  Settings.Sim_NumNodes = 100 // number of nodes
  launch()
}


trait BlockC { self: AggregateProgram  =>
  def findParent[V:OrderingFoldable](potential: V): ID = {
    mux(implicitly[OrderingFoldable[V]].compare(minHood { nbr(potential) }, potential) < 0) {
      minHood { nbr { (potential, mid()) } }._2
    } {
      Int.MaxValue
    }
  }

  def C[V: OrderingFoldable](potential: V, acc: (V, V) => V, local: V, Null: V): V = {
    rep(local) { v =>
      acc(local, foldhood(Null)(acc) {
        mux(nbr(findParent(potential)) == mid()) {
          nbr(v)
        } {
          nbr(Null)
        }
      })
    }
  }
}

trait BlocksWithGC { self: BlockC with BlockG =>
  def summarize(sink: Boolean, acc:(Double,Double)=>Double, local:Double, Null:Double): Double =
    broadcast(sink, C(distanceTo(sink), acc, local, Null))

  def average(sink: Boolean, value: Double): Double =
    summarize(sink, (a,b)=>{a+b}, value, 0.0) / summarize(sink, (a,b)=>a+b, 1, 0.0)
}

class Collection extends AggregateProgram with SensorDefinitions with BlockC with BlockG {

  def summarize(sink: Boolean, acc:(Double,Double)=>Double, local:Double, Null:Double): Double =
    broadcast(sink, C(distanceTo(sink), acc, local, Null))

  override def main() = summarize(sense1, _+_, if (sense2) 1.0 else 0.0, 0.0)
}

class CExample extends AggregateProgram with SensorDefinitions with BlockC with BlockG {

  def summarize(sink: Boolean, acc:(Double,Double)=>Double, local:Double, Null:Double): Double =
    broadcast(sink, C(distanceTo(sink), acc, local, Null))

  def p = distanceTo(sense1)

  override def main() = (SettingsSpace.ToStrings.Default_Double(p), mid()+"->"+findParent(p), C[Double](p, _+_, 1, 0.0))
}
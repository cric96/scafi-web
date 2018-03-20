/*
 * Copyright (C) 2016-2017, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENCE.txt file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package sims

import it.unibo.scafi.incarnations.BasicSimulationIncarnation._
import it.unibo.scafi.simulation.gui.{Launcher, Settings}

import scala.concurrent.duration.DurationInt

object ReplicatedGossipDemo extends Launcher {
  // Configuring simulation
  Settings.Sim_ProgramClass = "sims.ReplicatedGossip" // starting class, via Reflection
  Settings.ShowConfigPanel = false // show a configuration panel at startup
  Settings.Sim_NbrRadius = 0.25 // neighbourhood radius
  Settings.Sim_NumNodes = 100 // number of nodes
  Settings.ConfigurationSeed = 0
  launch()
}

class ReplicatedGossip extends AggregateProgram with StateManagement with SensorDefinitions with Gradients with Processes with BlockT
  with Spawn {
  def main: String = {
    val g = classic(sense1)
    val grepl = replicatedGossip2(sense1, numActiveProcs = 5, startEvery = 10, considerAfter = 20)
    f"$g%5.1f; " + (if(grepl.isDefined) f"${grepl.get}%5.1f" else "X")
  }

  def replicatedGossip(src: => Boolean, numActiveProcs: Int, startEvery: Int, considerAfter: Int): Option[Double] = {
    val generators = remember{Set(
      ProcessGenerator(
        trigger = () => sense2 && impulsesEvery(startEvery),
        generator = () => ProcessDef(PID("1"), comp = () => mux[Option[Double]](timer(considerAfter)==0){ Some(classic(src)) }{ None },
        stopCondition = () => timer(startEvery*numActiveProcs+startEvery/2+considerAfter)==0))
    )}
    val replicated = processExecution(generators).values.collect{ case Some(x) => x }
    if(replicated.isEmpty) None else Some(replicated.max)
  }

  def replicatedGossip2(src: => Boolean, numActiveProcs: Int, startEvery: Int, considerAfter: Int): Option[Double] = {
    try {
      val procs = spawn[Unit,Double]( (_) => {
        val status = mux[Status](timer(startEvery*numActiveProcs+startEvery/2+considerAfter)!=0){
          mux[Status](timer(considerAfter)!=0){ Bubble }{ Output }
        }{ External }
        (classic(src), status)
      }, mux(sense2 & impulsesEvery(startEvery)){ List(()) }{ List() })
      val max = procs.maxBy(_._1.puid)
      //if(mid==1) println(procs.size + " " + max._1)
      Some(max._2)
    } catch { case _ => Some(Double.PositiveInfinity) }
  }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.unibo.scafi.space.optimization.distances

import it.unibo.scafi.space.optimization.helper.MultiVector

/** This class is like [[EuclideanDistanceMetric]] but it does not take the square root.
  *
  * The value calculated by this class is not exact Euclidean distance, but it saves on computation
  * when you need the value for only comparison.
  */
class SquaredEuclideanDistanceMetric extends DistanceMetric {
  override def distance(a: MultiVector, b: MultiVector): Double = {
    checkValidArguments(a, b)
    (0 until a.size).map(i => math.pow(a(i) - b(i), 2)).sum
  }
}

object SquaredEuclideanDistanceMetric {
  def apply() = new SquaredEuclideanDistanceMetric()
}

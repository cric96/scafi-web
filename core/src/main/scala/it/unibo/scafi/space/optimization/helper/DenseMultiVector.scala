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

package it.unibo.scafi.space.optimization.helper

/**
 * Dense vector implementation of [[MultiVector]]. The data is represented in a continuous array of
 * doubles.
 *
 * @param data Array of doubles to store the vector elements
 */
case class DenseMultiVector(data: Array[Double]) extends MultiVector with Serializable {

  /**
   * Number of elements in a vector
   * @return the number of the elements in the vector
   */
  override def size: Int = {
    data.length
  }

  /**
   * Element wise access function
   *
   * @param index index of the accessed element
   * @return element at the given index
   */
  override def apply(index: Int): Double = {
    require(0 <= index && index < data.length, index + " not in [0, " + data.length + ")")
    data(index)
  }

  override def toString: String = {
    s"DenseVector(${data.mkString(", ")})"
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case dense: DenseMultiVector => data.length == dense.data.length && data.sameElements(dense.data)
      case _ => false
    }
  }

  override def hashCode: Int = {
    java.util.Arrays.hashCode(data)
  }

  /**
   * Copies the vector instance
   *
   * @return Copy of the vector instance
   */
  override def copy: DenseMultiVector = {
    DenseMultiVector(data.clone())
  }

  /** Updates the element at the given index with the provided value
    *
    * @param index Index whose value is updated.
    * @param value The value used to update the index.
    */
  override def update(index: Int, value: Double): Unit = {
    require(0 <= index && index < data.length, index + " not in [0, " + data.length + ")")

    data(index) = value
  }

  /** Returns the dot product of the recipient and the argument
    *
    * @param other a Vector
    * @return a scalar double of dot product
    */
  override def dot(other: MultiVector): Double = {
    require(size == other.size, "The size of vector must be equal.")

    other match {
      case SparseMultiVector(_, otherIndices, otherData) =>
        otherIndices.zipWithIndex.map {
          case (idx, sparseIdx) => data(idx) * otherData(sparseIdx)
        }.sum
      case _ => (0 until size).map(i => data(i) * other(i)).sum
    }
  }

  /** Returns the outer product (a.k.a. Kronecker product) of `this`
    * with `other`. The result will given in [[org.apache.flink.ml.math.SparseMatrix]]
    * representation if `other` is sparse and as [[org.apache.flink.ml.math.DenseMatrix]] otherwise.
    *
    * @param other a Vector
    * @return the [[org.apache.flink.ml.math.Matrix]] which equals the outer product of `this`
    *         with `other.`
    */
  override def outer(other: MultiVector): Matrix = {
    val numRows = size
    val numCols = other.size

    other match {
      case sv: SparseMultiVector =>
        val entries = for {
          i <- 0 until numRows
          (j, k) <- sv.indices.zipWithIndex
          value = this(i) * sv.data(k)
          if value != 0
        } yield (i, j, value)

        SparseMatrix.fromCOO(numRows, numCols, entries)
      case _ =>
        val values = for {
          i <- 0 until numRows
          j <- 0 until numCols
        } yield this(i) * other(j)

        DenseMatrix(numRows, numCols, values.toArray)
    }
  }

  /** Magnitude of a vector
    *
    * @return The length of the vector
    */
  override def magnitude: Double = {
    math.sqrt(data.map(x => x * x).sum)
  }

  /** Convert to a [[SparseMultiVector]]
    *
    * @return Creates a SparseVector from the DenseVector
    */
  def toSparseVector: SparseMultiVector = {
    val nonZero = (0 until size).zip(data).filter(_._2 != 0)

    SparseMultiVector.fromCOO(size, nonZero)
  }
}

object DenseMultiVector {

  def apply(values: Double*): DenseMultiVector = {
    new DenseMultiVector(values.toArray)
  }

  def apply(values: Array[Int]): DenseMultiVector = {
    new DenseMultiVector(values.map(_.toDouble))
  }

  def zeros(size: Int): DenseMultiVector = {
    init(size, 0.0)
  }

  def eye(size: Int): DenseMultiVector = {
    init(size, 1.0)
  }

  def init(size: Int, value: Double): DenseMultiVector = {
    new DenseMultiVector(Array.fill(size)(value))
  }

}

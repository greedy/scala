// Copyright 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

object LoopTesterApp {
  import collection.mutable.{ArrayBuffer,HashSet,HashMap}

  // mixing UnionFind directly into the parameter class
  // is somewhat questionable but works nicely here.
  // but we mix it in "protected", since calls to union/find
  // affect the LSG structure
  trait UnionFind[X <: UnionFind[X]] {
    this : X =>

      // control client access to the parent structure
      private var _parent = this
    def parent = _parent
    private def parent_=(newParent: X) = _parent = newParent

    //var parent = this

    protected def find : X  = {
      if (this != parent) parent = parent.find
      parent
    }

    protected def union(block : X) = {
      if (parent != this) print("!")
      parent = block
    }

    protected def clear = parent = this
  }

  class Block(val id: Int) extends UnionFind[Block] {

    val inEdges  = ArrayBuffer[Block]()
    val outEdges = ArrayBuffer[Block]()

    var first           = Block.UNVISITED
    var last            = Block.UNVISITED

    val nonBackPreds    = new HashSet[Block]()
    val backPreds       = new ArrayBuffer[Block](inEdges.size)
    var kind            = Block.NONHEADER

    var header : Block = null
    val children = new HashSet[Block]()
    var depth = -1
    var nesting = -1

    override def clear {
      super.clear
      first           = Block.UNVISITED
      last            = Block.UNVISITED
      nonBackPreds.clear
      backPreds.clear
      kind            = Block.NONHEADER
      header          = null
      children.clear
      depth = -1
      nesting = -1
    }

    override def toString = "BB#" + id
    def getNumPred  : Int = inEdges.size
    def getNumSucc  : Int = outEdges.size
    def addInEdge (block : Block) = {inEdges += block; block.outEdges += this}
    def addOutEdge(block : Block) = {outEdges += block; block.inEdges += this}
    def isReducible = kind != Block.IRREDUCIBLE

    def isAncestorOf(other : Block) : Boolean = {
      (first <= other.first) && (last >= other.last)
    }

    def DFS(current : Int,
            ordered : ArrayBuffer[Block]) : Int = {
      first = current
      ordered += this
      last = outEdges.foldLeft(current) ((lastid, target) =>
        if (target.first == Block.UNVISITED) target.DFS(lastid + 1, ordered)
        else lastid)
      return last
    }

    def classifyPreds = {
      header = null
      kind  = Block.NONHEADER
      inEdges.foreach( v => {
        if (v.first != Block.UNVISITED) {
          if (isAncestorOf(v)) {
            backPreds += v
          } else {
            nonBackPreds += v
          }}} ) }

    def collectScc(scc: Block) {
      val scc2 = find
      if (scc2 != scc) {
        if (!scc.isAncestorOf(scc2)) {
          scc.kind = Block.IRREDUCIBLE
          scc.nonBackPreds += scc2
        } else {
          scc.children += scc2
          scc2.header = scc
          scc2.union(scc)
          // Step e:
          scc2.nonBackPreds.foreach(_.collectScc(scc))
        }}}

    def collectLoop = {
      val outer = find
      // Step d:
      if (!backPreds.isEmpty) kind = Block.REDUCIBLE
      backPreds.foreach(
        v =>
          if (v != this) v.collectScc(outer)
          else kind = Block.SELF)
      !children.isEmpty
    }

    def dump = {
      var res = "BB#" + id + " "
      if (inEdges.length > 0)
        res += "\tin : " + inEdges
      if (outEdges.length > 0)
        res += "\tout: " + outEdges
      println(res)
    }

    def calculateNesting(_depth: Int) : Int = {
      depth = _depth
      nesting = children.foldLeft(-1)(
        (nest, node) => nest.max(node.calculateNesting(_depth + 1) + 1))
      nesting
    }

    def dumpLSG {
      if (!children.isEmpty) {
        (1 to depth). foreach(_=>print("  "))
        println("loop-%d nest: %d depth %d %s".format(id, nesting, depth,
                                                      if (isReducible) ""
                                                      else "(Irreducible) " ))
        children.foreach(_.dumpLSG)
      }}
  }

  object Block extends Enumeration {
    val UNVISITED = -1
    val TOP,      // uninitialized
    NONHEADER,    // a regular BB
    REDUCIBLE,    // reducible loop
    SELF,         // single BB loop
    IRREDUCIBLE,  // irreducible loop
    DEAD,         // a dead BB
    LAST = Value  // Sentinel
  }

  class CFG {
    val nodes = new ArrayBuffer[Block]()

    lazy val  startNode = {
      val tmp = nodes(0)
      // Start node is root of all other loops.
      tmp.header = tmp
      tmp
    }

    def createNode = {
      nodes += new Block(numNodes);
      nodes.last
    }

    def numNodes : Int = nodes.size
    def edgeList = { for (n1 <- nodes; n2 <- n1.outEdges) yield (n1, n2) }
    def dump = nodes.foreach(_.dump)
    def clear = nodes.foreach(_.clear)

    def DFS = {
      val ordered = new ArrayBuffer[Block](numNodes)
      startNode.DFS(0,ordered)
      ordered
    }

    def findLoops = {
      clear

      // Step a:
      val nodes = DFS

      // Step b:
      nodes.foreach(_.classifyPreds)

      // Step c:
     // +1 bc the original code creates an additional outer root
      nodes.reverseIterator.count(_.collectLoop) + 1
    }

    def LSG = {
      clear
      findLoops
      val root = new Block(-1)
      nodes.foreach(n => if (n == n.parent) root.children += n)
      root.calculateNesting(0)
      root
    }

    def dumpLSG = LSG.dumpLSG
  }

  // DSL for specifying CFGs

  implicit def RichBlock (start: Block) = new RichBlock(start)

  def node(implicit cfg: CFG) = cfg.createNode

  class RichBlock(start: Block) {

    def connect(to: Block)(implicit cfg: CFG) = {
      start.addOutEdge(to)
      to
    }

    def edge(implicit cfg: CFG) = {
      val to = node
      start.connect(to)
    }

    def diamond(implicit cfg: CFG) = start.edge.connect(start.edge.edge)

    def back(construct : Block => Block)(implicit cfg: CFG) = {
      val to = construct(start)
      to.connect(start)
      to
    }

    def repeat(n: Int, construct : Block => Block)(implicit cfg: CFG) = {
      (0 until n).foldLeft(start)((n,_) => construct(n))
    }

    def path(len: Int)(implicit cfg: CFG) = start.repeat(len,_.edge)
  }

  //======================================================

  def main(args: Array[String]) {

    implicit val cfg = new CFG

    def baseLoop(start: Block) = {
      start
      .back(
        _.repeat(2,_.edge.back(_.diamond))
        .edge)
      .edge
    }

    val n0 = node
    val n1 = node
    val n2 = n0.edge

    println("15000 dummy loops")
    (0 until 15000).foreach(_ => cfg.findLoops)

    println("Constructing CFG...")
    (1 to 10).foreach(
      _ => {
        n2
        .edge
        .repeat(100,
                _.back(_.edge
                       .repeat(25,
                               baseLoop(_)))
                .edge)
        .connect(n1)
      })

    var numLoops = 0;
    for (i <- 0 until 50) {
      print(".")
      numLoops = cfg.findLoops
    }
    println("Done. " + numLoops + " Loops")
    cfg.LSG
    //cfg.dumpLSG
  }
}

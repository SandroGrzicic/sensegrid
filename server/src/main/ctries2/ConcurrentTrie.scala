package ctries2

import java.util.concurrent.atomic._
import collection.Map
import collection.mutable.ConcurrentMap
import collection.immutable.ListMap
import annotation.tailrec
import annotation.switch

/**
 * Ctrie - A Lock-Free Concurrent Hash Array Mapped Trie
 *
 * Aleksandar Prokopec
 *
 * https://github.com/axel22/Ctries
 *
 */
final class INode[K, V](bn: MainNode[K, V], g: Gen) extends INodeBase[K, V](g) {
  import INodeBase._
  
  WRITE(bn)
  
  def this(g: Gen) = this(null, g)
  
  @inline final def WRITE(nval: MainNode[K, V]) = INodeBase.updater.set(this, nval)
  
  @inline final def CAS(old: MainNode[K, V], n: MainNode[K, V]) = INodeBase.updater.compareAndSet(this, old, n)
  
  @inline final def GCAS_READ(ct: ConcurrentTrie[K, V]): MainNode[K, V] = {
    val m = /*READ*/mainnode
    val prevval = /*READ*/m.prev
    if (prevval eq null) m
    else GCAS_Complete(m, ct)
  }
    
  @tailrec private def GCAS_Complete(m: MainNode[K, V], ct: ConcurrentTrie[K, V]): MainNode[K, V] = if (m eq null) null else {
    // complete the GCAS
    val prev = /*READ*/m.prev
    val ctr = ct.RDCSS_READ_ROOT(true)
    
    prev match {
      case null =>
        m
      case fn: FailedNode[_, _] => // try to commit to previous value
        if (CAS(m, fn.prev)) fn.prev
        else GCAS_Complete(/*READ*/mainnode, ct)
      case vn: MainNode[_, _] =>
        // Assume that you've read the root from the generation G.
        // Assume that the snapshot algorithm is correct.
        // ==> you can only reach nodes in generations <= G.
        // ==> `gen` is <= G.
        // We know that `ctr.gen` is >= G.
        // ==> if `ctr.gen` = `gen` then they are both equal to G.
        // ==> otherwise, we know that either `ctr.gen` > G, `gen` < G,
        //     or both
        if ((ctr.gen eq gen) && ct.nonReadOnly) {
          // try to commit
          if (m.CAS_PREV(prev, null)) m
          else GCAS_Complete(m, ct)
        } else {
          // try to abort
          m.CAS_PREV(prev, new FailedNode(prev))
          GCAS_Complete(/*READ*/mainnode, ct)
        }
    }
  }
  
  @inline final def GCAS(old: MainNode[K, V], n: MainNode[K, V], ct: ConcurrentTrie[K, V]): Boolean = {
    n.WRITE_PREV(old)
    if (CAS(old, n)) {
      GCAS_Complete(n, ct)
      /*READ*/n.prev eq null
    } else false
  }
  
  @inline private def inode(cn: MainNode[K, V]) = {
    val nin = new INode[K, V](gen)
    nin.WRITE(cn)
    nin
  }
  
  @inline final def copyToGen(ngen: Gen, ct: ConcurrentTrie[K, V]) = {
    val nin = new INode[K, V](ngen)
    val main = GCAS_READ(ct)
    nin.WRITE(main)
    nin
  }
  
  /** Inserts a key value pair, overwriting the old pair if the keys match.
   *  
   *  @return        true if successful, false otherwise
   */
  @tailrec final def rec_insert(k: K, v: V, hc: Int, lev: Int, parent: INode[K, V], startgen: Gen, ct: ConcurrentTrie[K, V]): Boolean = {
    val m = GCAS_READ(ct) // use -Yinline!
    
    m match {
      case cn: CNode[K, V] => // 1) a multiway node
        val idx = (hc >>> lev) & 0x1f
        val flag = 1 << idx
        val bmp = cn.bitmap
        val mask = flag - 1
        val pos = Integer.bitCount(bmp & mask)
        if ((bmp & flag) != 0) {
          // 1a) insert below
          cn.array(pos) match {
            case in: INode[K, V] =>
              if (startgen eq in.gen) in.rec_insert(k, v, hc, lev + 5, this, startgen, ct)
              else {
                if (GCAS(cn, cn.renewed(startgen, ct), ct)) rec_insert(k, v, hc, lev, parent, startgen, ct)
                else false
              }
            case sn: SNode[K, V] =>
              if (sn.hc == hc && sn.k == k) GCAS(cn, cn.updatedAt(pos, new SNode(k, v, hc), gen), ct)
              else {
                val rn = if (cn.gen eq gen) cn else cn.renewed(gen, ct)
                val nn = rn.updatedAt(pos, inode(CNode.dual(sn, sn.hc, new SNode(k, v, hc), hc, lev + 5, gen)), gen)
                GCAS(cn, nn, ct)
              }
          }
        } else {
          val rn = if (cn.gen eq gen) cn else cn.renewed(gen, ct)
          val ncnode = rn.insertedAt(pos, flag, new SNode(k, v, hc), gen)
          GCAS(cn, ncnode, ct)
        }
      case tn: TNode[K, V] =>
        clean(parent, ct, lev - 5)
        false
      case ln: LNode[K, V] => // 3) an l-node
        val nn = ln.inserted(k, v)
        GCAS(ln, nn, ct)
    }
  }
  
  /** Inserts a new key value pair, given that a specific condition is met.
   *
   *  @param cond        null - don't care if the key was there; KEY_ABSENT - key wasn't there; KEY_PRESENT - key was there; other value `v` - key must be bound to `v`
   *  @return            null if unsuccessful, Option[V] otherwise (indicating previous value bound to the key)
   */
  @tailrec final def rec_insertif(k: K, v: V, hc: Int, cond: AnyRef, lev: Int, parent: INode[K, V], startgen: Gen, ct: ConcurrentTrie[K, V]): Option[V] = {
    val m = GCAS_READ(ct)  // use -Yinline!
    
    m match {
      case cn: CNode[K, V] => // 1) a multiway node
        val idx = (hc >>> lev) & 0x1f
        val flag = 1 << idx
        val bmp = cn.bitmap
        val mask = flag - 1
        val pos = Integer.bitCount(bmp & mask)
        if ((bmp & flag) != 0) {
          // 1a) insert below
          cn.array(pos) match {
            case in: INode[K, V] =>
              if (startgen eq in.gen) in.rec_insertif(k, v, hc, cond, lev + 5, this, startgen, ct)
              else {
                if (GCAS(cn, cn.renewed(startgen, ct), ct)) rec_insertif(k, v, hc, cond, lev, parent, startgen, ct)
                else null
              }
            case sn: SNode[K, V] => cond match {
              case null =>
                if (sn.hc == hc && sn.k == k) {
                  if (GCAS(cn, cn.updatedAt(pos, new SNode(k, v, hc), gen), ct)) Some(sn.v) else null
                } else {
                  val rn = if (cn.gen eq gen) cn else cn.renewed(gen, ct)
                  val nn = rn.updatedAt(pos, inode(CNode.dual(sn, sn.hc, new SNode(k, v, hc), hc, lev + 5, gen)), gen)
                  if (GCAS(cn, nn, ct)) None
                  else null
                }
              case INode.KEY_ABSENT =>
                if (sn.hc == hc && sn.k == k) Some(sn.v)
                else {
                  val rn = if (cn.gen eq gen) cn else cn.renewed(gen, ct)
                  val nn = rn.updatedAt(pos, inode(CNode.dual(sn, sn.hc, new SNode(k, v, hc), hc, lev + 5, gen)), gen)
                  if (GCAS(cn, nn, ct)) None
                  else null
                }
              case INode.KEY_PRESENT =>
                if (sn.hc == hc && sn.k == k) {
                  if (GCAS(cn, cn.updatedAt(pos, new SNode(k, v, hc), gen), ct)) Some(sn.v) else null
                } else None
              case otherv: V =>
                if (sn.hc == hc && sn.k == k && sn.v == otherv) {
                  if (GCAS(cn, cn.updatedAt(pos, new SNode(k, v, hc), gen), ct)) Some(sn.v) else null
                } else None
            }
          }
        } else cond match {
          case null | INode.KEY_ABSENT =>
            val rn = if (cn.gen eq gen) cn else cn.renewed(gen, ct)
            val ncnode = rn.insertedAt(pos, flag, new SNode(k, v, hc), gen)
            if (GCAS(cn, ncnode, ct)) None else null
          case INode.KEY_PRESENT => None
          case otherv: V => None
        }
      case sn: TNode[K, V] =>
        clean(parent, ct, lev - 5)
        null
      case ln: LNode[K, V] => // 3) an l-node
        @inline def insertln() = {
          val nn = ln.inserted(k, v)
          GCAS(ln, nn, ct)
        }
        cond match {
          case null =>
            val optv = ln.get(k)
            if (insertln()) optv else null
          case INode.KEY_ABSENT =>
            ln.get(k) match {
              case None => if (insertln()) None else null
              case optv => optv
            }
          case INode.KEY_PRESENT =>
            ln.get(k) match {
              case Some(v0) => if (insertln()) Some(v0) else null
              case None => None
            }
          case otherv: V =>
            ln.get(k) match {
              case Some(v0) if v0 == otherv => if (insertln()) Some(otherv) else null
              case _ => None
            }
        }
    }
  }
  
  /** Looks up the value associated with the key.
   *  
   *  @return          null if no value has been found, RESTART if the operation wasn't successful, or any other value otherwise
   */
  @tailrec final def rec_lookup(k: K, hc: Int, lev: Int, parent: INode[K, V], startgen: Gen, ct: ConcurrentTrie[K, V]): AnyRef = {
    val m = GCAS_READ(ct) // use -Yinline!
    
    m match {
      case cn: CNode[K, V] => // 1) a multinode
        val idx = (hc >>> lev) & 0x1f
        val flag = 1 << idx
        val bmp = cn.bitmap
        if ((bmp & flag) == 0) null // 1a) bitmap shows no binding
        else { // 1b) bitmap contains a value - descend
          val pos = if (bmp == 0xffffffff) idx else Integer.bitCount(bmp & (flag - 1))
          val sub = cn.array(pos)
          sub match {
            case in: INode[K, V] =>
              if (ct.isReadOnly || (startgen eq in.gen)) in.rec_lookup(k, hc, lev + 5, this, startgen, ct)
              else {
                if (GCAS(cn, cn.renewed(startgen, ct), ct)) rec_lookup(k, hc, lev, parent, startgen, ct)
                else return RESTART // used to be throw RestartException
              }
            case sn: SNode[K, V] => // 2) singleton node
              if (sn.hc == hc && sn.k == k) sn.v.asInstanceOf[AnyRef]
              else null
          }
        }
      case tn: TNode[K, V] => // 3) non-live node
        def cleanReadOnly(tn: TNode[K, V]) = if (ct.nonReadOnly) {
          clean(parent, ct, lev - 5)
          RESTART // used to be throw RestartException
        } else {
          if (tn.hc == hc && tn.k == k) tn.v.asInstanceOf[AnyRef]
          else null
        }
        cleanReadOnly(tn)
      case ln: LNode[K, V] => // 5) an l-node
        ln.get(k).asInstanceOf[Option[AnyRef]].orNull
    }
  }
  
  /** Removes the key associated with the given value.
   *  
   *  @param v         if null, will remove the key irregardless of the value; otherwise removes only if binding contains that exact key and value
   *  @return          null if not successful, an Option[V] indicating the previous value otherwise
   */
  final def rec_remove(k: K, v: V, hc: Int, lev: Int, parent: INode[K, V], startgen: Gen, ct: ConcurrentTrie[K, V]): Option[V] = {
    val m = GCAS_READ(ct) // use -Yinline!
    
    m match {
      case cn: CNode[K, V] =>
        val idx = (hc >>> lev) & 0x1f
        val bmp = cn.bitmap
        val flag = 1 << idx
        if ((bmp & flag) == 0) None
        else {
          val pos = Integer.bitCount(bmp & (flag - 1))
          val sub = cn.array(pos)
          val res = sub match {
            case in: INode[K, V] => 
              if (startgen eq in.gen) in.rec_remove(k, v, hc, lev + 5, this, startgen, ct)
              else {
                if (GCAS(cn, cn.renewed(startgen, ct), ct)) rec_remove(k, v, hc, lev, parent, startgen, ct)
                else null
              }
            case sn: SNode[K, V] =>
              if (sn.hc == hc && sn.k == k && (v == null || sn.v == v)) {
                val ncn = cn.removedAt(pos, flag, gen).toContracted(lev)
                if (GCAS(cn, ncn, ct)) Some(sn.v) else null
              } else None
          }
          
          if (res == None || (res eq null)) res
          else {
            @tailrec def cleanParent(nonlive: AnyRef) {
              val pm = parent.GCAS_READ(ct)
              pm match {
                case cn: CNode[K, V] =>
                  val idx = (hc >>> (lev - 5)) & 0x1f
                  val bmp = cn.bitmap
                  val flag = 1 << idx
                  if ((bmp & flag) == 0) {} // somebody already removed this i-node, we're done
                  else {
                    val pos = Integer.bitCount(bmp & (flag - 1))
                    val sub = cn.array(pos)
                    if (sub eq this) nonlive match {
                      case tn: TNode[K, V] =>
                        val ncn = cn.updatedAt(pos, tn.copyUntombed, gen).toContracted(lev - 5)
                        if (!parent.GCAS(cn, ncn, ct))
                          if (ct.RDCSS_READ_ROOT().gen == startgen) cleanParent(nonlive)
                    }
                  }
                case _ => // parent is no longer a cnode, we're done
              }
            }
            
            if (parent ne null) { // never tomb at root
              val n = GCAS_READ(ct)
              if (n.isInstanceOf[TNode[_, _]])
                cleanParent(n)
            }
            
            res
          }
        }
      case tn: TNode[K, V] =>
        clean(parent, ct, lev - 5)
        null
      case ln: LNode[K, V] =>
        if (v == null) {
          val optv = ln.get(k)
          val nn = ln.removed(k)
          if (GCAS(ln, nn, ct)) optv else null
        } else ln.get(k) match {
          case optv @ Some(v0) if v0 == v =>
            val nn = ln.removed(k)
            if (GCAS(ln, nn, ct)) optv else null
          case _ => None
        }
    }
  }
  
  private def clean(nd: INode[K, V], ct: ConcurrentTrie[K, V], lev: Int) {
    val m = nd.GCAS_READ(ct)
    m match {
      case cn: CNode[K, V] => nd.GCAS(cn, cn.toCompressed(ct, lev, gen), ct)
      case _ =>
    }
  }
  
  final def isNullInode(ct: ConcurrentTrie[K, V]) = GCAS_READ(ct) eq null
  
  /* this is a quiescent method! */
  def string(lev: Int) = "%sINode -> %s".format("  " * lev, mainnode match {
    case null => "<null>"
    case tn: TNode[_, _] => "TNode(%s, %s, %d, !)".format(tn.k, tn.v, tn.hc)
    case cn: CNode[_, _] => cn.string(lev)
    case ln: LNode[_, _] => ln.string(lev)
    case x => "<elem: %s>".format(x)
  })
  
}


object INode {
  val KEY_PRESENT = new AnyRef
  val KEY_ABSENT = new AnyRef
  
  def newRootNode[K, V] = {
    val gen = new Gen
    val cn = new CNode[K, V](0, new Array(0), gen)
    new INode[K, V](cn, gen)
  }
}


final class FailedNode[K, V](p: MainNode[K, V]) extends MainNode[K, V] {
  WRITE_PREV(p)
  
  def string(lev: Int) = throw new UnsupportedOperationException
  
  override def toString = "FailedNode(%s)".format(p)
}


trait KVNode[K, V] {
  def kvPair: (K, V)
}


final class SNode[K, V](final val k: K, final val v: V, final val hc: Int)
extends BasicNode with KVNode[K, V] {
  final def copy = new SNode(k, v, hc)
  final def copyTombed = new TNode(k, v, hc)
  final def copyUntombed = new SNode(k, v, hc)
  final def kvPair = (k, v)
  final def string(lev: Int) = ("  " * lev) + "SNode(%s, %s, %x)".format(k, v, hc)
}


final class TNode[K, V](final val k: K, final val v: V, final val hc: Int)
extends MainNode[K, V] with KVNode[K, V] {
  final def copy = new TNode(k, v, hc)
  final def copyTombed = new TNode(k, v, hc)
  final def copyUntombed = new SNode(k, v, hc)
  final def kvPair = (k, v)
  final def string(lev: Int) = ("  " * lev) + "TNode(%s, %s, %x, !)".format(k, v, hc)
}


final class LNode[K, V](final val listmap: ListMap[K, V])
extends MainNode[K, V] {
  def this(k: K, v: V) = this(ListMap(k -> v))
  def this(k1: K, v1: V, k2: K, v2: V) = this(ListMap(k1 -> v1, k2 -> v2))
  def inserted(k: K, v: V) = new LNode(listmap + ((k, v)))
  def removed(k: K) = {
    val updmap = listmap - k
    if (updmap.size > 1) new LNode(updmap)
    else {
      val (k, v) = updmap.iterator.next
      new TNode(k, v, ConcurrentTrie.computeHash(k)) // create it tombed so that it gets compressed on subsequent accesses
    }
  }
  def get(k: K) = listmap.get(k)
  def string(lev: Int) = (" " * lev) + "LNode(%s)".format(listmap.mkString(", "))
}


final class CNode[K, V](final val bitmap: Int, final val array: Array[BasicNode], final val gen: Gen)
extends MainNode[K, V] {
  
  final def updatedAt(pos: Int, nn: BasicNode, gen: Gen) = {
    val len = array.length
    val narr = new Array[BasicNode](len)
    Array.copy(array, 0, narr, 0, len)
    narr(pos) = nn
    new CNode[K, V](bitmap, narr, gen)
  }
  
  final def removedAt(pos: Int, flag: Int, gen: Gen) = {
    val arr = array
    val len = arr.length
    val narr = new Array[BasicNode](len - 1)
    Array.copy(arr, 0, narr, 0, pos)
    Array.copy(arr, pos + 1, narr, pos, len - pos - 1)
    new CNode[K, V](bitmap ^ flag, narr, gen)
  }
  
  final def insertedAt(pos: Int, flag: Int, nn: BasicNode, gen: Gen) = {
    val len = array.length
    val bmp = bitmap
    val narr = new Array[BasicNode](len + 1)
    Array.copy(array, 0, narr, 0, pos)
    narr(pos) = nn
    Array.copy(array, pos, narr, pos + 1, len - pos)
    new CNode[K, V](bmp | flag, narr, gen)
  }
  
  /** Returns a copy of this cnode such that all the i-nodes below it are copied
   *  to the specified generation `ngen`.
   */
  final def renewed(ngen: Gen, ct: ConcurrentTrie[K, V]) = {
    var i = 0
    val arr = array
    val len = arr.length
    val narr = new Array[BasicNode](len)
    while (i < len) {
      arr(i) match {
        case in: INode[K, V] => narr(i) = in.copyToGen(ngen, ct)
        case bn: BasicNode => narr(i) = bn
      }
      i += 1
    }
    new CNode[K, V](bitmap, narr, ngen)
  }
  
  private def resurrect(inode: INode[K, V], inodemain: AnyRef) = inodemain match {
    case tn: TNode[_, _] => tn.copyUntombed
    case _ => inode
  }
  
  final def toContracted(lev: Int) = if (array.length == 1 && lev > 0) array(0) match {
    case sn: SNode[K, V] => sn.copyTombed
    case _ => this
  } else this
  
  // - if the branching factor is 1 for this CNode, and the child
  //   is a tombed SNode, returns its tombed version
  // - otherwise, if there is at least one non-null node below,
  //   returns the version of this node with at least some null-inodes
  //   removed (those existing when the op began)
  // - if there are only null-i-nodes below, returns null
  final def toCompressed(ct: ConcurrentTrie[K, V], lev: Int, gen: Gen) = {
    var bmp = bitmap
    var i = 0
    val arr = array
    val tmparray = new Array[BasicNode](arr.length)
    while (i < arr.length) { // construct new bitmap
      val sub = arr(i)
      sub match {
        case in: INode[K, V] =>
          val inodemain = in.GCAS_READ(ct)
          assert(inodemain ne null)
          tmparray(i) = resurrect(in, inodemain)
        case sn: SNode[K, V] =>
          tmparray(i) = sn
      }
      i += 1
    }
    
    new CNode[K, V](bmp, tmparray, gen).toContracted(lev)
  }
  
  private[ctries2] def string(lev: Int): String = "CNode %x\n%s".format(bitmap, array.map(_.string(lev + 1)).mkString("\n"))
  
  /* quiescently consistent - don't call concurrently to anything involving a GCAS!! */
  protected def collectElems: Seq[(K, V)] = array flatMap {
    case sn: SNode[K, V] => Some(sn.kvPair)
    case in: INode[K, V] => in.mainnode match {
      case tn: TNode[K, V] => Some(tn.kvPair)
      case ln: LNode[K, V] => ln.listmap.toList
      case cn: CNode[K, V] => cn.collectElems
    }
  }
  
  protected def collectLocalElems: Seq[String] = array flatMap {
    case sn: SNode[K, V] => Some(sn.kvPair._2.toString)
    case in: INode[K, V] => Some(in.toString.drop(14) + "(" + in.gen + ")")
  }
  
  override def toString = {
    val elems = collectLocalElems
    "CNode(sz: %d; %s)".format(elems.size, elems.sorted.mkString(", "))
  }
}


object CNode {
  
  def dual[K, V](x: SNode[K, V], xhc: Int, y: SNode[K, V], yhc: Int, lev: Int, gen: Gen): MainNode[K, V] = if (lev < 35) {
    val xidx = (xhc >>> lev) & 0x1f
    val yidx = (yhc >>> lev) & 0x1f
    val bmp = (1 << xidx) | (1 << yidx)
    if (xidx == yidx) {
      val subinode = new INode[K, V](gen)//(ConcurrentTrie.inodeupdater)
      subinode.mainnode = dual(x, xhc, y, yhc, lev + 5, gen)
      new CNode(bmp, Array(subinode), gen)
    } else {
      if (xidx < yidx) new CNode(bmp, Array(x, y), gen)
      else new CNode(bmp, Array(y, x), gen)
    }
  } else {
    new LNode(x.k, x.v, y.k, y.v)
  }
  
}


case class RDCSS_Descriptor[K, V](old: INode[K, V], expectedmain: MainNode[K, V], nv: INode[K, V]) {
  @volatile var committed = false
}


class ConcurrentTrie[K, V] private (r: AnyRef, rtupd: AtomicReferenceFieldUpdater[ConcurrentTrie[K, V], AnyRef])
extends ConcurrentMap[K, V]
{
  import ConcurrentTrie.computeHash
  
  private val rootupdater = rtupd
  @volatile var root = r
  
  def this() = this(
    INode.newRootNode,
    AtomicReferenceFieldUpdater.newUpdater(classOf[ConcurrentTrie[K, V]], classOf[AnyRef], "root")
  )
  
  /* internal methods */
  
  @inline final def CAS_ROOT(ov: AnyRef, nv: AnyRef) = rootupdater.compareAndSet(this, ov, nv)
  
  @inline final def RDCSS_READ_ROOT(abort: Boolean = false): INode[K, V] = {
    val r = /*READ*/root
    r match {
      case in: INode[K, V] => in
      case desc: RDCSS_Descriptor[K, V] => RDCSS_Complete(abort)
    }
  }
  
  @tailrec private def RDCSS_Complete(abort: Boolean): INode[K, V] = {
    val v = /*READ*/root
    v match {
      case in: INode[K, V] => in
      case desc: RDCSS_Descriptor[K, V] =>
        val RDCSS_Descriptor(ov, exp, nv) = desc
        if (abort) {
          if (CAS_ROOT(desc, ov)) ov
          else RDCSS_Complete(abort)
        } else {
          val oldmain = ov.GCAS_READ(this)
          if (oldmain eq exp) {
            if (CAS_ROOT(desc, nv)) {
              desc.committed = true
              nv
            } else RDCSS_Complete(abort)
          } else {
            if (CAS_ROOT(desc, ov)) ov
            else RDCSS_Complete(abort)
          }
        }
    }
  }
  
  private def RDCSS_ROOT(ov: INode[K, V], expectedmain: MainNode[K, V], nv: INode[K, V]): Boolean = {
    val desc = RDCSS_Descriptor(ov, expectedmain, nv)
    if (CAS_ROOT(ov, desc)) {
      RDCSS_Complete(false)
      /*READ*/desc.committed
    } else false
  }
  
  @tailrec private def inserthc(k: K, hc: Int, v: V) {
    val r = RDCSS_READ_ROOT()
    if (!r.rec_insert(k, v, hc, 0, null, r.gen, this)) inserthc(k, hc, v)
  }
  
  @tailrec private def insertifhc(k: K, hc: Int, v: V, cond: AnyRef): Option[V] = {
    val r = RDCSS_READ_ROOT()
    
    val ret = r.rec_insertif(k, v, hc, cond, 0, null, r.gen, this)
    if (ret eq null) insertifhc(k, hc, v, cond)
    else ret
  }
  
  @tailrec private def lookuphc(k: K, hc: Int): AnyRef = {
    val r = RDCSS_READ_ROOT()
    val res = r.rec_lookup(k, hc, 0, null, r.gen, this)
    if (res eq INodeBase.RESTART) lookuphc(k, hc)
    else res
  }
  
  /*
  //@tailrec
  private def lookuphc(k: K, hc: Int): AnyRef = {
    val r = RDCSS_READ_ROOT()
    try {
      r.rec_lookup(k, hc, 0, null, r.gen, this)
    } catch {
      case RestartException =>
        lookuphc(k, hc)
    }
  }
  */
  
  @tailrec private def removehc(k: K, v: V, hc: Int): Option[V] = {
    val r = RDCSS_READ_ROOT()
    val res = r.rec_remove(k, v, hc, 0, null, r.gen, this)
    if (res ne null) res
    else removehc(k, v, hc)
  }
  
  def string = RDCSS_READ_ROOT().string(0)
  
  /* public methods */
  
  @inline final def isReadOnly = rootupdater eq null
  
  @inline final def nonReadOnly = rootupdater ne null
  
  @tailrec final def snapshot(): ConcurrentTrie[K, V] = {
    val r = RDCSS_READ_ROOT()
    val expmain = r.GCAS_READ(this)
    if (RDCSS_ROOT(r, expmain, r.copyToGen(new Gen, this))) new ConcurrentTrie(r.copyToGen(new Gen, this), rootupdater)
    else snapshot()
  }
  
  @tailrec final def readOnlySnapshot(): Map[K, V] = {
    val r = RDCSS_READ_ROOT()
    val expmain = r.GCAS_READ(this)
    if (RDCSS_ROOT(r, expmain, r.copyToGen(new Gen, this))) new ConcurrentTrie(r, null)
    else readOnlySnapshot()
  }
  
  @tailrec final override def clear() {
    val r = RDCSS_READ_ROOT()
    if (!RDCSS_ROOT(r, r.GCAS_READ(this), INode.newRootNode[K, V])) clear()
  }
  
  final def lookup(k: K): V = {
    val hc = computeHash(k)
    lookuphc(k, hc).asInstanceOf[V]
  }
  
  final override def apply(k: K): V = {
    val hc = computeHash(k)
    val res = lookuphc(k, hc)
    if (res eq null) throw new NoSuchElementException
    else res.asInstanceOf[V]
  }
  
  final def get(k: K): Option[V] = {
    val hc = computeHash(k)
    Option(lookuphc(k, hc)).asInstanceOf[Option[V]]
  }
  
  override def put(key: K, value: V): Option[V] = {
    val hc = computeHash(key)
    insertifhc(key, hc, value, null)
  }
  
  final override def update(k: K, v: V) {
    val hc = computeHash(k)
    inserthc(k, hc, v)
  }
  
  final def +=(kv: (K, V)) = {
    update(kv._1, kv._2)
    this
  }
  
  final override def remove(k: K): Option[V] = {
    val hc = computeHash(k)
    removehc(k, null.asInstanceOf[V], hc)
  }
  
  final def -=(k: K) = {
    remove(k)
    this
  }
  
  def putIfAbsent(k: K, v: V): Option[V] = {
    val hc = computeHash(k)
    insertifhc(k, hc, v, INode.KEY_ABSENT)
  }
  
  def remove(k: K, v: V): Boolean = {
    val hc = computeHash(k)
    removehc(k, v, hc).nonEmpty
  }
  
  def replace(k: K, oldvalue: V, newvalue: V): Boolean = {
    val hc = computeHash(k)
    insertifhc(k, hc, newvalue, oldvalue.asInstanceOf[AnyRef]).nonEmpty
  }
  
  def replace(k: K, v: V): Option[V] = {
    val hc = computeHash(k)
    insertifhc(k, hc, v, INode.KEY_PRESENT)
  }
  
  def iterator: Iterator[(K, V)] =
    if (nonReadOnly) readOnlySnapshot().iterator
    else new CtrieIterator(this)
  
}


object ConcurrentTrie {
  val inodeupdater = AtomicReferenceFieldUpdater.newUpdater(classOf[INodeBase[_, _]], classOf[MainNode[_, _]], "mainnode")
  
  @inline final def computeHash[K](k: K): Int = {
    k.hashCode
  }
}


class CtrieIterator[K, V](ct: ConcurrentTrie[K, V], mustInit: Boolean = true) extends Iterator[(K, V)] {
  var stack = new Array[Array[BasicNode]](7)
  var stackpos = new Array[Int](7)
  var depth = -1
  var subiter: Iterator[(K, V)] = null
  var current: KVNode[K, V] = null
  
  if (mustInit) initialize()
  
  def hasNext = (current ne null) || (subiter ne null)
  
  def next() = if (hasNext) {
    var r: (K, V) = null
    if (subiter ne null) {
      r = subiter.next()
      checkSubiter()
    } else {
      r = current.kvPair
      advance()
    }
    r
  } else Iterator.empty.next()
  
  private def readin(in: INode[K, V]) = in.GCAS_READ(ct) match {
    case cn: CNode[K, V] =>
      depth += 1
      stack(depth) = cn.array
      stackpos(depth) = -1
      advance()
    case tn: TNode[K, V] =>
      current = tn
    case ln: LNode[K, V] =>
      subiter = ln.listmap.iterator
      checkSubiter()
    case null =>
      current = null
  }
  
  @inline private def checkSubiter() = if (!subiter.hasNext) {
    subiter = null
    advance()
  }
  
  @inline private def initialize() {
    assert(ct.isReadOnly)
    
    val r = ct.RDCSS_READ_ROOT()
    readin(r)
  }
  
  def advance(): Unit = if (depth >= 0) {
    val npos = stackpos(depth) + 1
    if (npos < stack(depth).length) {
      stackpos(depth) = npos
      stack(depth)(npos) match {
        case sn: SNode[K, V] =>
          current = sn
        case in: INode[K, V] =>
          readin(in)
      }
    } else {
      depth -= 1
      advance()
    }
  } else current = null
  
  /** Returns a sequence of iterators over subsets of this iterator.
   *  It's used to ease the implementation of splitters for a parallel version of the Ctrie.
   */
  protected def subdivide: Seq[Iterator[(K, V)]] = if (subiter ne null) {
    // the case where an LNode is being iterated
    val it = subiter
    subiter = null
    advance()
    Seq(it, this)
  } else if (depth == -1) Seq(this) else {
    var d = 0
    while (d <= depth) {
      val rem = stack(d).length - 1 - stackpos(d)
      if (rem > 0) {
        val (arr1, arr2) = stack(d).drop(stackpos(d) + 1).splitAt(rem / 2)
        stack(d) = arr1
        stackpos(d) = -1
        val it = new CtrieIterator[K, V](ct, false)
        it.stack(0) = arr2
        it.stackpos(0) = -1
        it.depth = 0
        it.advance() // <-- fix it
        return Seq(this, it)
      }
      d += 1
    }
    Seq(this)
  }
  
  private def print {
    println("ctrie iterator")
    println(stackpos.mkString(","))
    println("depth: " + depth)
    println("curr.: " + current)
    println(stack.mkString("\n"))
  }
  
}


object RestartException extends util.control.ControlThrowable


final class Gen


private[ctries2] object Debug {
  import collection._
  
  lazy val logbuffer = new java.util.concurrent.ConcurrentLinkedQueue[AnyRef]
  
  def log(s: AnyRef) = logbuffer.add(s)
  
  def flush() {
    for (s <- JavaConversions.asScalaIterator(logbuffer.iterator())) Console.out.println(s.toString)
    logbuffer.clear()
  }
  
  def clear() {
    logbuffer.clear()
  }
  
}












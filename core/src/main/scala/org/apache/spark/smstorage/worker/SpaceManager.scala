/**
 *
 */
package org.apache.spark.smstorage.worker

import org.apache.spark.smstorage.SBlockEntry
import org.apache.spark.util.{Utils, TimeStampedHashSet}
import org.apache.spark.Logging
import org.apache.spark.smstorage.sharedmemory.SMemoryManager

/**
 * 管理本节点的存储空间的逻辑使用情况的组件
 * entry对于每一个block都是唯一的，所以可以作为唯一区分的标志
 * @param totalMemory
 * @param smManager
 * @author Wang Haihua
 */
private[spark] class SpaceManager(
    var totalMemory: Long,
    smManager: SMemoryManager) extends Logging {

  /**
   * 本节点的共享存储空间使用量
   */
  var usedMemory: Long = 0L
  
  /**
   * 节点当前所有Executor的JVM合计的最大内存
   */
  var totalExecutorMemory: Long = 0L

  /**
   * 正在写或者释放的内存
   */
  var pendingMemory: Long = 0L
  
  //private val peningEntries = new TimeStampedHashSet[String]
  
  def getAvailableMemory() = {
    totalMemory - usedMemory - pendingMemory
  }
  
  /**
   * 申请空间，如果成功，会锁定这块空间，并且返回共享存储空间的入口
   * 1) reqMemSize <= availableMemory => None
   * 2) availableMemory < reqMemSize <= totalMemory => (availableMemory-reqMemSize)
   * 3) reqMemSize > totalMemory => -1(Special identity)
   * TODO: 检查申请的kong
   */
  def checkSpace(reqMemSize: Long): Option[Long] = {
    logInfo(s"ensureFreeSpace(${Utils.bytesToString(reqMemSize)}) called with curMem=${Utils.bytesToString(usedMemory)}, maxMem=${Utils.bytesToString(totalMemory)}")

    if (reqMemSize > totalMemory) {
      logWarning(s"Will not store the block as it is larger than total space memory.")
      Some(-1L)
    } else if (getAvailableMemory() < reqMemSize) {
      logInfo(s"Will not store the block as it is larger than available memory, should evict some block")
      Some(reqMemSize - getAvailableMemory())
    } else {
      None
    }
  }

  def allocateSpace(reqMemSize: Long): Int = {
    usedMemory += reqMemSize
    smManager.applySpace(reqMemSize.toInt)
  }
  
  /**
   * 释放共享存储空间
   */
  def releaseSpace(entryId: Int, size: Long) {
    smManager.realseSpace(entryId);
    usedMemory -= size
  }
  
  def close() {
    
  }
  
}
package top.kmar.game.utils

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 线程安全的任务管理器
 * @author 空梦
 */
class TaskManager(size: Int) {

    private val array = Array(size) { ConcurrentLinkedQueue<Runnable>() }

    /** 添加一个任务 */
    fun add(index: Int, task: Runnable) {
        array[index].add(task)
    }

    /** 在当前线程执行并删除指定任务列表中的所有任务 */
    fun runTaskList(index: Int) {
        val itor = array[index].iterator()
        while (itor.hasNext()) {
            val it = itor.next()
            itor.remove()
            it.run()
        }
    }

    /** 在当前线程中执行指定任务列表中的所有任务 */
    fun runTaskListNoRemove(index: Int) {
        array[index].forEach { it.run() }
    }

    /** 移除指定任务 */
    fun removeTask(index: Int, task: Runnable) {
        array[index].remove(task)
    }

}
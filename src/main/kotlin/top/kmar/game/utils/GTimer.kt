package top.kmar.game.utils

import java.util.function.LongConsumer

/**
 * 带修复功能的计时器
 * @author 空梦
 */
class GTimer {

    private lateinit var thread: Thread
    val alive: Boolean
        get() = thread.isAlive

    /**
     * 启动任务
     * @param name 线程名称
     * @param interval 执行间隔
     * @param isDaemon 是否为守护线程
     * @param task 要执行的任务（接收的参数为两次任务执行之间的时间间隔）
     */
    fun start(name: String, interval: Long, isDaemon: Boolean, task: LongConsumer) {
        thread = Thread {
            val sleepBound = interval - 2
            var prev = System.currentTimeMillis()
            var offset = 0L
            while (!thread.isInterrupted) {
                var now = System.currentTimeMillis()
                var time = now - prev
                // 如果未达到临界条件则 sleep 等待
                if (time + offset < sleepBound) {
                    try {
                        Thread.sleep(sleepBound - time)
                    } catch (e: InterruptedException) {
                        break
                    }
                    now = System.currentTimeMillis()
                    time = now - prev
                }
                // 自旋等待
                while (time + offset < interval) {
                    now = System.currentTimeMillis()
                    time = now - prev
                }
                offset += time - interval
                prev = now
                task.accept(time)
            }
        }
        thread.name = name
        thread.isDaemon = isDaemon
        thread.start()
    }

    /**
     * 启动无时间修补的任务
     * @param name 线程名称
     * @param interval 间隔时间
     * @param isDaemon 是否为守护线程
     * @param task 要执行的任务
     */
    fun startNonFixed(name: String, interval: Long, isDaemon: Boolean, task: Runnable) {
        thread = Thread {
            while (!thread.isInterrupted) {
                try {
                    Thread.sleep(interval)
                } catch (e: InterruptedException) {
                    break
                }
                task.run()
            }
        }
        thread.name = name
        thread.isDaemon = isDaemon
        thread.start()
    }

    /** 取消任务 */
    fun cancel() {
        thread.interrupt()
    }

    /** 阻塞当前线程直到任务执行完毕 */
    fun join() {
        thread.join()
    }

}
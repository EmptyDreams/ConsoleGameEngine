package top.kmar.game.utils

import java.util.concurrent.atomic.AtomicInteger

/**
 * 在当前值的基础上或一个数值，并复制给 Atomic。
 *
 * 该函数是线程安全的。
 */
inline fun AtomicInteger.updateWith(updater: (Int) -> Int) {
    while (true) {
        val old = get()
        val update = updater(old)
        if (compareAndSet(old, update))
            break
    }
}
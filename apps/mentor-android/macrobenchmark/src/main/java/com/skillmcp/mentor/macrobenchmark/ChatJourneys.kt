package com.skillmcp.mentor.macrobenchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

const val TARGET_PACKAGE = "com.skillmcp.mentor"
private const val WAIT_MS = 5_000L

/** Scrolls the chat list (Compose testTag "chat_list", exposed as a resource id) up and down. */
fun MacrobenchmarkScope.scrollChatList() {
    val list = device.wait(Until.findObject(By.res("chat_list")), WAIT_MS) ?: return
    list.setGestureMargin(device.displayWidth / 5)
    list.fling(Direction.UP)
    device.waitForIdle()
    list.fling(Direction.DOWN)
    device.waitForIdle()
}

/** Opens and closes the chat history drawer (menu button content description "Chats"). */
fun MacrobenchmarkScope.openAndCloseDrawer() {
    val menu = device.wait(Until.findObject(By.desc("Chats")), WAIT_MS) ?: return
    menu.click()
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
}

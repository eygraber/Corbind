/*
 * Copyright 2019 Vladimir Raupov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ldralighieri.corbind.widget

import android.os.Build
import android.view.MenuItem
import android.widget.Toolbar
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.offerElement

/**
 * Perform an action on the clicked item in [Toolbar] menu.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Toolbar.itemClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit
) {

    val events = scope.actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(scope, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Perform an action on the clicked item in [Toolbar] menu inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun Toolbar.itemClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit
) = coroutineScope {

    val events = actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(this, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Create a channel which emits the clicked item in [Toolbar] menu.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@CheckResult
fun Toolbar.itemClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {
    setOnMenuItemClickListener(listener(scope, ::offerElement))
    invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Create a flow which emits the clicked item in [Toolbar] menu.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@CheckResult
fun Toolbar.itemClicks(): Flow<MenuItem> = channelFlow {
    setOnMenuItemClickListener(listener(this, ::offer))
    awaitClose { setOnMenuItemClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (MenuItem) -> Boolean
) = Toolbar.OnMenuItemClickListener {

    if (scope.isActive) {
        emitter(it)
        return@OnMenuItemClickListener true
    }
    return@OnMenuItemClickListener false
}

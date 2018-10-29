@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.appcompat

import android.view.MenuItem
import androidx.annotation.CheckResult
import androidx.appcompat.widget.ActionMenuView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

// -----------------------------------------------------------------------------------------------


fun ActionMenuView.itemClicks(
        scope: CoroutineScope,
        action: suspend (MenuItem) -> Unit
) {

    val events = scope.actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(scope, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

suspend fun ActionMenuView.itemClicks(
        action: suspend (MenuItem) -> Unit
) = coroutineScope {

    val events = actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(this, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun ActionMenuView.itemClicks(
        scope: CoroutineScope
): ReceiveChannel<MenuItem> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnMenuItemClickListener(listener(this, ::offer))
    invokeOnClose { setOnMenuItemClickListener(null) }
}

@CheckResult
suspend fun ActionMenuView.itemClicks(): ReceiveChannel<MenuItem> = coroutineScope {

    produce<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        setOnMenuItemClickListener(listener(this, ::offer))
        invokeOnClose { setOnMenuItemClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (MenuItem) -> Boolean
) = ActionMenuView.OnMenuItemClickListener {

    if (scope.isActive) { emitter(it) }
    return@OnMenuItemClickListener true
}
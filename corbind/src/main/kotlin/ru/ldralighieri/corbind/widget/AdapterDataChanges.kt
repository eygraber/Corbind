package ru.ldralighieri.corbind.widget

import android.database.DataSetObserver
import android.widget.Adapter
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun <T : Adapter> T.dataChanges(
        scope: CoroutineScope,
        action: suspend (T) -> Unit
) {
    val events = scope.actor<T>(Dispatchers.Main, Channel.CONFLATED) {
        for (adapter in channel) action(adapter)
    }

    events.offer(this)
    val dataSetObserver = observer(this, events::offer)
    registerDataSetObserver(dataSetObserver)
    events.invokeOnClose { unregisterDataSetObserver(dataSetObserver) }
}

suspend fun <T : Adapter> T.dataChanges(
        action: suspend (T) -> Unit
) = coroutineScope {
    val events = actor<T>(Dispatchers.Main, Channel.CONFLATED) {
        for (adapter in channel) action(adapter)
    }

    events.offer(this@dataChanges)
    val dataSetObserver = observer(this@dataChanges, events::offer)
    registerDataSetObserver(dataSetObserver)
    events.invokeOnClose { unregisterDataSetObserver(dataSetObserver) }
}


// -----------------------------------------------------------------------------------------------


fun <T : Adapter> T.dataChanges(
        scope: CoroutineScope
): ReceiveChannel<T> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(this@dataChanges)
    val dataSetObserver = observer(this@dataChanges, ::offer)
    registerDataSetObserver(dataSetObserver)
    invokeOnClose { unregisterDataSetObserver(dataSetObserver) }
}

suspend fun <T : Adapter> T.dataChanges(): ReceiveChannel<T> = coroutineScope {

    produce<T>(Dispatchers.Main, Channel.CONFLATED) {
        offer(this@dataChanges)
        val dataSetObserver = observer(this@dataChanges, ::offer)
        registerDataSetObserver(dataSetObserver)
        invokeOnClose { unregisterDataSetObserver(dataSetObserver) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun <T : Adapter> observer(
        adapter: T,
        emitter: (T) -> Boolean
) = object : DataSetObserver() {
    override fun onChanged() { emitter(adapter) }
}
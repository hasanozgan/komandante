package com.hasanozgan.komandante.eventbus

import com.hasanozgan.examples.bankaccount.MessageSent
import com.hasanozgan.examples.bankaccount.NotificationEvent
import com.hasanozgan.komandante.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.BeforeClass
import kotlin.test.Test

sealed class UserEvent(override val aggregateID: AggregateID) : Event()

data class AddUser(val userID: AggregateID) : UserEvent(userID)
data class RemoveUser(val userID: AggregateID) : UserEvent(userID)
data class ChangeUserAddress(val userID: AggregateID) : UserEvent(userID)
data class AnotherEvent(override val aggregateID: AggregateID) : Event()

class LocalBusTest {
    companion object {
        val receivedAllEvents = mutableListOf<Event>()
        val receivedUserEvents = mutableListOf<UserEvent>()
        val receivedAnotherEvents = mutableListOf<AnotherEvent>()
        val receivedEventHandlerEvents = mutableListOf<Event>()

        val localBus = localBusOf<Event>()
        val UserID = AggregateID.randomUUID()
        val events = listOf(
                AddUser(UserID),
                RemoveUser(UserID),
                ChangeUserAddress(UserID),
                AnotherEvent(AggregateID.randomUUID())
        )

        @JvmStatic
        @BeforeClass
        fun setup() {
            localBus.subscribe {
                receivedAllEvents.add(it)
            }

            localBus.subscribeOf<UserEvent> {
                receivedUserEvents.add(it)
            }

            localBus.subscribeOf<AnotherEvent> {
                receivedAnotherEvents.add(it)
            }

            val eventHandler = object : EventHandler<UserEvent> {
                override fun <T : Event> handle(event: T) {
                    receivedEventHandlerEvents.add(event)
                }

                override val handlerType: EventHandlerType
                    get() = "test-event-handler"
            }
            localBus.addHandler(eventHandler)

            events.forEach {
                localBus.publish(it)
            }
        }
    }

    @Test
    fun shouldReceivedAllEvents() {
        assertThat(receivedAllEvents.size, IsEqual(events.size))
        assertThat(receivedAllEvents, IsEqual(events))
    }

    @Test
    fun shouldReceivedOnlyUserEvents() {
        val userEvents = events.filter { it is UserEvent }
        assertThat(receivedUserEvents.size, IsEqual(userEvents.size))
        assertThat(receivedUserEvents, IsEqual(userEvents))
    }

    @Test
    fun shouldReceivedOnlyAnotherEvents() {
        val anotherEvent = events.filter { it is AnotherEvent }
        assertThat(receivedAnotherEvents.size, IsEqual(anotherEvent.size))
        assertThat(receivedAnotherEvents, IsEqual(anotherEvent))
    }

    @Test
    fun shouldAddEventHandler() {
        val userEvents = events.filter { it is UserEvent }
        assertThat(receivedEventHandlerEvents.size, IsEqual(userEvents.size))
        assertThat(receivedEventHandlerEvents, IsEqual(userEvents))
    }

    @Test
    fun shouldHandleErrorSubscribe() {
        val localBus = localBusOf<Event>()
        val dummyException = Exception("dummy exception")
        localBus.subscribe({
            throw dummyException
        }, { assertThat(dummyException, IsEqual(it)) })
        localBus.publish(MessageSent(newAggregateID(), "some message"))
    }

    @Test
    fun shouldHandleErrorSubscribeOf() {
        val localBus = localBusOf<Event>()
        val dummyException = Exception("dummy exception")
        localBus.subscribeOf<NotificationEvent>({
            throw dummyException
        }, { assertThat(dummyException, IsEqual(it)) })
        localBus.publish(MessageSent(newAggregateID(), "some message"))
    }

    @Test
    fun shouldHandleErrorSubscribeOfWithGeneric() {
        val localBus = localBusOf<Event>()
        val dummyException = Exception("dummy exception")
        localBus.subscribeOf<AnotherEvent>({
            throw dummyException
        }, { assertThat(dummyException, IsEqual(it)) })
        localBus.publish(AnotherEvent(newAggregateID()))
    }

    @Test
    fun shouldHandleErrorAddEventHandler() {
        val localBus = localBusOf<Event>()
        val dummyException = Exception("dummy exception")
        localBus.addHandler(object : EventHandler<NotificationEvent> {
            override val handlerType: EventHandlerType
                get() = "dummy-handler"

            override fun <T : Event> handle(event: T) {
                throw dummyException
            }
        }, { assertThat(dummyException, IsEqual(it)) })
        localBus.publish(MessageSent(newAggregateID(), "some message"))
    }
}

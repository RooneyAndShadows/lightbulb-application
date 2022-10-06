package com.github.rooneyandshadows.lightbulb.application.activity.service.notification.client.stomp.base

import android.net.ConnectivityManager
import com.github.rooneyandshadows.lightbulb.application.activity.service.notification.client.BaseNotificationClient

abstract class StompNotificationClient(
    configuration: StompNotificationClientConfiguration
) : BaseNotificationClient(configuration) {

    open class StompNotificationClientConfiguration(
        connectivityManager: ConnectivityManager,
        notificationBuilder: NotificationBuilder
    ) : NotificationClientConfiguration(
        connectivityManager,
        notificationBuilder
    ) {
        var subscribePayload: StompPayload? = null
        var unsubscribePayload: StompPayload? = null
        var clientListener: ClientListener? = null
        var listenUntilCondition: (() -> Boolean) = { true }

        open fun withSubscribePayload(startPayload: StompPayload): StompNotificationClientConfiguration {
            return apply {
                this.subscribePayload = startPayload
            }
        }

        open fun withUnsubscribePayload(stopPayload: StompPayload): StompNotificationClientConfiguration {
            return apply {
                this.unsubscribePayload = stopPayload
            }
        }

        open fun withListener(clientListener: ClientListener): StompNotificationClientConfiguration {
            return apply {
                this.clientListener = clientListener
            }
        }

        open fun withListenUntilCondition(condition: (() -> Boolean)): StompNotificationClientConfiguration {
            return apply {
                this.listenUntilCondition = condition
            }
        }

        abstract class StompPayload {
            open fun configureHeaders(stompHeaders: Map<String, String>) {
            }

            open fun configureMessage(): String {
                return ""
            }
        }

        abstract class ClientListener {
            open fun onConnected() {
            }

            open fun onDisconnected() {
            }

            open fun onError(exception: Throwable) {

            }
        }
    }
}
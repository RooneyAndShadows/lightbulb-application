package com.github.rooneyandshadows.lightbulb.application.activity.service

import android.app.Notification
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.github.rooneyandshadows.java.commons.stomp.StompClient
import com.github.rooneyandshadows.java.commons.stomp.StompSubscription
import com.github.rooneyandshadows.java.commons.stomp.frame.StompFrame
import com.github.rooneyandshadows.java.commons.stomp.listener.StompConnectionListener
import org.java_websocket.drafts.Draft_6455
import java.net.URI
import java.util.function.Consumer

class StompWebSocketNotificationClient(val configuration: StompNotificationJobServiceConfiguration) :
    NotificationClient() {
    private val maxExecutionTime = 180000//3 minutes
    private var jobStartTime: Long = 0
    private lateinit var jobStompClient: StompClient
    private lateinit var jobStompThread: StompThread
    private var jobStompSubscription: StompSubscription? = null
    private var isConnected: Boolean = false

    override fun initialize() {
        jobStompClient = object :
            StompClient(URI.create(configuration.stompApiUrl), Draft_6455(), null, 3000) {
            override fun onError(ex: Exception) {
                Log.d(
                    this.javaClass.simpleName,
                    "Failed to connect notification service. Retrying..."
                )
            }
        }.apply {
            isReuseAddr = true
        }.apply {
            setStompConnectionListener(object : StompConnectionListener() {
                override fun onConnecting() {
                    super.onConnecting()
                    isConnected = false
                }

                override fun onConnected() {
                    super.onConnected()
                    Log.d(
                        this.javaClass.simpleName,
                        "Notification service connected"
                    )
                    isConnected = true
                    if (configuration.listenUntilCondition.invoke()) return
                    if (jobStompSubscription != null)
                        jobStompClient.removeSubscription(jobStompSubscription)
                    val headers: MutableMap<String, String> = HashMap()
                    configuration.stompStartPayload?.configureHeaders(headers)
                    jobStompSubscription = subscribe(
                        configuration.stompSubscribeUrl,
                        headers
                    ) { stompFrame: StompFrame ->
                        val notification =
                            configuration.generateNotificationFromMessage(stompFrame.body)
                        onNotificationReceived.forEach(Consumer { listener ->
                            listener.invoke(notification)
                        })
                    }
                }

                override fun onDisconnected() {
                    super.onDisconnected()
                    isConnected = false
                    Log.d(this.javaClass.simpleName, "Notification service disconnected")
                }
            })
        }
    }

    override fun start() {
        this.jobStartTime = System.currentTimeMillis()
        this.jobStompThread = StompThread()
        this.jobStompThread.start()
    }

    override fun stop() {
        jobStompThread.stopThread()
    }

    private fun stopListenForNotifications(clearShownNotifications: Boolean) {
        val headers: MutableMap<String, String> = HashMap()
        configuration.stompStopPayload?.configureHeaders(headers)
        if (jobStompSubscription != null)
            jobStompClient.removeSubscription(jobStompSubscription, headers)
        jobStompClient.closeConnection(0, "")
        if (clearShownNotifications)
            onNotificationsInvalidated.forEach(Consumer { listener ->
                listener.invoke()
            })
    }

    inner class StompThread : Thread() {
        private var initialConnection = true
        private var interrupt = false

        fun stopThread() {
            interrupt = true
        }

        override fun run() {
            try {
                while (!interrupt) {
                    if (!isInternetAvailable(configuration.connectivityManager))
                        jobStompClient.closeConnection(0, "")
                    if (!isConnected && configuration.listenUntilCondition()) {
                        if (initialConnection) {
                            jobStompClient.connectBlocking()
                            initialConnection = false
                        } else {
                            jobStompClient.reconnectBlocking()
                        }
                        if (!isConnected) {
                            sleep(5000)
                            continue
                        }
                    }
                    if ((System.currentTimeMillis() - jobStartTime) > maxExecutionTime) {
                        if (isConnected)
                            stopListenForNotifications(false)
                        break
                    }
                    if (isConnected && !configuration.listenUntilCondition())
                        stopListenForNotifications(true)
                    sleep(5000)
                }
                if (interrupt && isConnected)
                    stopListenForNotifications(false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onClose.forEach(Consumer { listener ->
                    listener.invoke()
                })
            }
        }
    }

    class StompNotificationJobServiceConfiguration constructor(
        val stompApiUrl: String,
        val stompSubscribeUrl: String,
        val connectivityManager: ConnectivityManager,
        val generateNotificationFromMessage: ((receivedMessage: String) -> Notification)
    ) {
        var stompStartPayload: StompPayload? = null
        var stompStopPayload: StompPayload? = null
        var listenUntilCondition: (() -> Boolean) = { true }

        fun withSubscribePayload(startPayload: StompPayload): StompNotificationJobServiceConfiguration {
            return apply {
                this.stompStartPayload = startPayload
            }
        }

        fun withUnsubscribePayload(stopPayload: StompPayload): StompNotificationJobServiceConfiguration {
            return apply {
                this.stompStopPayload = stopPayload
            }
        }

        fun withListenUntilCondition(condition: (() -> Boolean)): StompNotificationJobServiceConfiguration {
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
    }
}
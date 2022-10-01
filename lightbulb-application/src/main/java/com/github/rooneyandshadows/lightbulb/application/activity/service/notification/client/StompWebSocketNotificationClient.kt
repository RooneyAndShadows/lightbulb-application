package com.github.rooneyandshadows.lightbulb.application.activity.service.notification.client

import android.app.Notification
import android.net.ConnectivityManager
import com.github.rooneyandshadows.java.commons.stomp.StompClient
import com.github.rooneyandshadows.java.commons.stomp.StompSubscription
import com.github.rooneyandshadows.java.commons.stomp.frame.StompFrame
import com.github.rooneyandshadows.java.commons.stomp.listener.StompConnectionListener
import com.github.rooneyandshadows.lightbulb.application.activity.service.notification.client.base.NotificationClient
import org.java_websocket.drafts.Draft_6455
import java.net.URI

class StompWebSocketNotificationClient(val configuration: Configuration) :
    NotificationClient() {
    private val maxExecutionTime = 180000//3 minutes
    private var jobStartTime: Long = 0
    private lateinit var jobStompClient: StompClient
    private lateinit var jobStompThread: StompThread
    private var jobStompSubscription: StompSubscription? = null
    private var isConnected: Boolean = false

    @Override
    override fun initialize() {
        jobStompThread = StompThread()
    }

    @Override
    override fun start() {
        this.jobStartTime = System.currentTimeMillis()
        this.jobStompThread.start()
    }

    @Override
    override fun stop() {
        jobStompThread.stopThread()
    }

    inner class StompThread : Thread() {
        private var initialConnection = true
        private var interrupt = false

        override fun run() {
            try {
                initializeClient();
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
                    if ((System.currentTimeMillis() - jobStartTime) > maxExecutionTime) break
                    if (isConnected && !configuration.listenUntilCondition()) {
                        if (jobStompSubscription != null) {
                            val headers: MutableMap<String, String> = HashMap()
                            configuration.stompStopPayload?.configureHeaders(headers)
                            jobStompClient.removeSubscription(jobStompSubscription, headers)
                        }
                        onNotificationsInvalidated.forEach { it.invoke() }
                        break
                    }
                    sleep(5000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                jobStompClient.closeConnection(0, "")
                jobStompClient.close()
                onClose.forEach { it.invoke() }
            }
        }

        private fun initializeClient() {
            jobStompClient = object :
                StompClient(URI.create(configuration.stompApiUrl), Draft_6455(), null, 3000) {
                override fun onError(ex: Exception) {
                    isConnected = false
                    configuration.clientListener?.onError(ex)
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
                        isConnected = true
                        configuration.clientListener?.onConnected()
                        if (!configuration.listenUntilCondition.invoke()) return
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
                            onNotificationReceived.forEach { it.invoke(notification) }
                        }
                    }

                    override fun onDisconnected() {
                        super.onDisconnected()
                        isConnected = false
                        configuration.clientListener?.onDisconnected()
                    }
                })
            }
        }

        fun stopThread() {
            interrupt = true
        }
    }

    class Configuration constructor(
        val stompApiUrl: String,
        val stompSubscribeUrl: String,
        val connectivityManager: ConnectivityManager,
        val generateNotificationFromMessage: ((receivedMessage: String) -> Notification)
    ) {
        var stompStartPayload: StompPayload? = null
        var stompStopPayload: StompPayload? = null
        var clientListener: ClientListener? = null
        var listenUntilCondition: (() -> Boolean) = { true }

        fun withSubscribePayload(startPayload: StompPayload): Configuration {
            return apply {
                this.stompStartPayload = startPayload
            }
        }

        fun withUnsubscribePayload(stopPayload: StompPayload): Configuration {
            return apply {
                this.stompStopPayload = stopPayload
            }
        }

        fun withListener(clientListener: ClientListener): Configuration {
            return apply {
                this.clientListener = clientListener
            }
        }

        fun withListenUntilCondition(condition: (() -> Boolean)): Configuration {
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

            open fun onError(exception: Exception) {

            }
        }
    }
}
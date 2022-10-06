package com.github.rooneyandshadows.lightbulb.application.activity.service.notification.client.stomp

import android.net.ConnectivityManager
import com.github.rooneyandshadows.java.commons.stomp.StompClient
import com.github.rooneyandshadows.java.commons.stomp.StompSubscription
import com.github.rooneyandshadows.java.commons.stomp.frame.StompFrame
import com.github.rooneyandshadows.java.commons.stomp.listener.StompConnectionListener
import com.github.rooneyandshadows.lightbulb.application.activity.service.notification.client.stomp.base.StompNotificationClient
import org.java_websocket.drafts.Draft_6455
import java.net.URI

class StompWebSocketNotificationClient(val configuration: Configuration) :
    StompNotificationClient(configuration) {
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
                    if (!isInternetAvailable())
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
                            configuration.unsubscribePayload?.configureHeaders(headers)
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
                        configuration.subscribePayload?.configureHeaders(headers)
                        jobStompSubscription = subscribe(
                            configuration.stompSubscribeUrl,
                            headers
                        ) { stompFrame: StompFrame ->
                            val notification =
                                configuration.notificationBuilder.build(stompFrame.body)
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

    class Configuration(
        val stompApiUrl: String,
        val stompSubscribeUrl: String,
        connectivityManager: ConnectivityManager,
        notificationBuilder: NotificationBuilder,
    ) : StompNotificationClientConfiguration(
        connectivityManager,
        notificationBuilder
    ) {
        override fun withSubscribePayload(startPayload: StompPayload): Configuration {
            return super.withSubscribePayload(startPayload) as Configuration
        }

        override fun withUnsubscribePayload(stopPayload: StompPayload): Configuration {
            return super.withUnsubscribePayload(stopPayload) as Configuration
        }

        override fun withListener(clientListener: ClientListener): Configuration {
            return super.withListener(clientListener) as Configuration
        }

        override fun withListenUntilCondition(condition: () -> Boolean): Configuration {
            return super.withListenUntilCondition(condition) as Configuration
        }
    }
}
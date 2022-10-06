package com.github.rooneyandshadows.lightbulb.application.activity.service.notification.client.stomp

import android.net.ConnectivityManager
import com.github.rooneyandshadows.lightbulb.application.activity.service.notification.client.stomp.base.StompNotificationClient
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.JdkLoggerFactory
import io.vertx.core.Vertx
import io.vertx.ext.stomp.StompClient
import io.vertx.ext.stomp.StompClientConnection
import io.vertx.ext.stomp.StompClientOptions
import java.util.*
import kotlin.collections.HashMap

class StompSocketNotificationClient(val configuration: Configuration) :
    StompNotificationClient(configuration) {
    private val maxExecutionTime = 180000//3 minutes
    private var jobStartTime: Long = 0
    private lateinit var jobStompClient: StompClient
    private var jobStompClientConnection: StompClientConnection? = null
    private lateinit var jobStompThread: StompThread
    private var isConnected: Boolean = false
    private var closing: Boolean = false
    private lateinit var vertx: Vertx

    @Override
    override fun initialize() {
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE)
        vertx = Vertx.vertx()
        jobStompClient = StompClient.create(
            vertx,
            StompClientOptions()
                .setHost(configuration.stompHost)
                .setPort(configuration.stompPort)
        )
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

    private fun connectClient() {
        jobStompClient.connect()
            .onSuccess { connection: StompClientConnection ->
                connection.connectionDroppedHandler {
                    isConnected = false
                    configuration.clientListener?.onDisconnected()
                    reconnectClient()
                }
                isConnected = true
                jobStompClientConnection = connection
                configuration.clientListener?.onConnected()
                if (!configuration.listenUntilCondition.invoke()) {
                    connection.close()
                    return@onSuccess
                }
                val headers: MutableMap<String, String> = HashMap()
                configuration.subscribePayload?.configureHeaders(headers)
                connection.subscribe(configuration.stompSubscribeDestination, headers) { frame ->
                    val notification =
                        configuration.notificationBuilder.build(frame.bodyAsString)
                    onNotificationReceived.forEach { it.invoke(notification) }
                }
            }
            .onFailure { err: Throwable ->
                isConnected = false
                configuration.clientListener?.onError(err)
                reconnectClient()
            }
    }


    private fun reconnectClient() {
        if (closing)
            return
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                jobStompClient.close()
                if (closing)
                    return
                jobStompClient = StompClient.create(
                    vertx,
                    StompClientOptions()
                        .setHost(configuration.stompHost)
                        .setPort(configuration.stompPort)
                )
                connectClient();
            }
        }, 5000)
    }

    inner class StompThread : Thread() {
        private var interrupt = false

        fun stopThread() {
            interrupt = true
        }

        override fun run() {
            try {
                connectClient()
                while (!interrupt) {
                    if ((System.currentTimeMillis() - jobStartTime) > maxExecutionTime) {
                        break
                    }
                    if (isConnected && !configuration.listenUntilCondition()) {
                        onNotificationsInvalidated.forEach { it.invoke() }
                        break
                    }
                    sleep(5000)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (isConnected) {
                    val headers: MutableMap<String, String> = HashMap()
                    configuration.unsubscribePayload?.configureHeaders(headers)
                    jobStompClientConnection?.unsubscribe(
                        configuration.stompSubscribeDestination,
                        headers
                    )
                    jobStompClient.close()
                }
                closing = true
                jobStompClient.close()
                onClose.forEach { it.invoke() }
            }
        }
    }

    class Configuration(
        val stompHost: String,
        val stompPort: Int,
        val stompSubscribeDestination: String,
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
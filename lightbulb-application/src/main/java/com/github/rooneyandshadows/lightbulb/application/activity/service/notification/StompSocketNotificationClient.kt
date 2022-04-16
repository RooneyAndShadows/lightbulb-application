package com.github.rooneyandshadows.lightbulb.application.activity.service.notification

import android.app.Notification
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.JdkLoggerFactory
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.ext.stomp.StompClient
import io.vertx.ext.stomp.StompClientConnection
import io.vertx.ext.stomp.StompClientOptions

class StompSocketNotificationClient(val configuration: Configuration) :
    NotificationClient() {
    private val maxExecutionTime = 180000//3 minutes
    private var jobStartTime: Long = 0
    private lateinit var jobStompClient: StompClient
    private var jobStompClientConnection: StompClientConnection? = null
    private lateinit var jobStompThread: StompThread
    private var isConnected: Boolean = false
    private lateinit var vertx: Vertx

    @Override
    override fun initialize() {
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE)
        vertx = Vertx.vertx()
        val options = VertxOptions();
        options.blockedThreadCheckInterval = 1000 * 60 * 60
        Vertx.vertx(options)
        jobStompClient = StompClient.create(
            vertx,
            StompClientOptions()
                .setHost(configuration.stompHost)
                .setPort(configuration.stompPort)
        )
    }

    @Override
    override fun start() {
        this.jobStartTime = System.currentTimeMillis()
        this.jobStompThread = StompThread()
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
                    Thread.sleep(5000)
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
                configuration.stompSubscribePayload?.configureHeaders(headers)
                connection.subscribe(configuration.stompSubscribeDestination, headers) { frame ->
                    val notification =
                        configuration.generateNotificationFromMessage(frame.bodyAsString)
                    onNotificationReceived.forEach { it.invoke(notification) }
                }
            }
            .onFailure { err: Throwable ->
                isConnected = false
                configuration.clientListener?.onError(err)
                Thread.sleep(5000)
                reconnectClient()
            }
    }

    private fun reconnectClient() {
        jobStompClient.close()
        jobStompClient = StompClient.create(
            vertx,
            StompClientOptions()
                .setHost(configuration.stompHost)
                .setPort(configuration.stompPort)
        )
        connectClient();
    }

    private fun stopListenForNotifications(clearShownNotifications: Boolean) {
        val headers: MutableMap<String, String> = HashMap()
        configuration.stompUnsubscribePayload?.configureHeaders(headers)
        jobStompClientConnection?.unsubscribe(configuration.stompSubscribeDestination, headers)
        jobStompClient.close()
        if (clearShownNotifications)
            onNotificationsInvalidated.forEach { it.invoke() }
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
                jobStompClient.close()
                onClose.forEach { it.invoke() }
            }
        }
    }

    class Configuration constructor(
        val stompHost: String,
        val stompPort: Int,
        val stompSubscribeDestination: String,
        val generateNotificationFromMessage: ((receivedMessage: String) -> Notification)
    ) {
        var stompSubscribePayload: StompPayload? = null
        var stompUnsubscribePayload: StompPayload? = null
        var clientListener: ClientListener? = null
        var listenUntilCondition: (() -> Boolean) = { true }

        fun withSubscribePayload(startPayload: StompPayload): Configuration {
            return apply {
                this.stompSubscribePayload = startPayload
            }
        }

        fun withUnsubscribePayload(stopPayload: StompPayload): Configuration {
            return apply {
                this.stompUnsubscribePayload = stopPayload
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

            open fun onError(exception: Throwable) {

            }
        }
    }
}
package io.sakurasou.renkei

import io.ktor.server.application.Application
import io.sakurasou.renkei.config.configureAuth
import io.sakurasou.renkei.config.configureDatabase
import io.sakurasou.renkei.di.configureDependencies
import io.sakurasou.renkei.plugins.configureHttp
import io.sakurasou.renkei.plugins.configureMonitoring
import io.sakurasou.renkei.plugins.configureRouting
import io.sakurasou.renkei.plugins.configureSecurity
import io.sakurasou.renkei.plugins.configureSerialization

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain
        .main(args)
}

fun Application.mainModule() {
    configureDatabase()
    configureAuth()
    configureDependencies()
    configureHttp()
    configureMonitoring()
    configureSerialization()
    configureSecurity()
    configureRouting()
}

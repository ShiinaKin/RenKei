package io.sakurasou.renkei.common

import java.time.Clock
import java.time.Instant

/**
 * @author Shiina Kin
 * 2026/8/16 00:54
 */

fun getUTCTimestamp() = Instant.now(Clock.systemUTC()).toEpochMilli()

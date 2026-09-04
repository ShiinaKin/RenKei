package io.sakurasou.renkei.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.sakurasou.renkei.di.DIManager
import io.sakurasou.renkei.di.diOperation
import io.sakurasou.renkei.di.get
import io.sakurasou.renkei.di.regist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/**
 * @author ShiinaKin
 * 2024/9/5 14:33
 */
object DatabaseSingleton {
    fun init(
        jdbcURL: String,
        driverClassName: String,
        username: String,
        password: String,
    ) {
        prepareSQLiteDatabasePath(jdbcURL)

        val hikariConfig =
            HikariConfig().apply {
                jdbcUrl = jdbcURL
                this.driverClassName = driverClassName
                this.username = username
                this.password = password
                maximumPoolSize = 3
                isReadOnly = false
                transactionIsolation = "TRANSACTION_SERIALIZABLE"
            }
        val dataSource = HikariDataSource(hikariConfig)

        val database = Database.connect(dataSource)
        diOperation {
            regist { database }
        }

        DatabaseInit.init(database)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        suspendTransaction(DIManager.getDIInstance().get()) {
            withContext(Dispatchers.IO) {
                block()
            }
        }
}

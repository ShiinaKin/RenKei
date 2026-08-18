package io.sakurasou.renkei.module

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.sakurasou.renkei.module.dao.IncomingCallEventDAO
import io.sakurasou.renkei.module.dao.SMSEventDAO
import io.sakurasou.renkei.module.entity.IncomingCallEvent
import io.sakurasou.renkei.module.entity.SMSEvent
import javax.inject.Singleton

/**
 * @author Shiina Kin
 * 2026/8/18 22:59
 */

@Database(entities = [SMSEvent::class, IncomingCallEvent::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsEventDAO(): SMSEventDAO
    abstract fun incomingCallEventDAO(): IncomingCallEventDAO
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room.databaseBuilder<AppDatabase>(context, DATABASE_NAME)
            .setDriver(AndroidSQLiteDriver())
            .build()

    @Provides
    fun provideSmsEventDao(database: AppDatabase): SMSEventDAO = database.smsEventDAO()

    @Provides
    fun provideIncomingCallEventDao(database: AppDatabase): IncomingCallEventDAO =
        database.incomingCallEventDAO()

    private const val DATABASE_NAME = "renkei-db"
}

package io.sakurasou.renkei.module.dao

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.sakurasou.renkei.module.AppDatabase
import io.sakurasou.renkei.module.entity.BaseEvent
import io.sakurasou.renkei.module.entity.IncomingCallEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomingCallEventDAOTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: IncomingCallEventDAO

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder<AppDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()
        dao = database.incomingCallEventDAO()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun pendingAndLatestQueriesReflectStatus() =
        runBlocking {
            val id = dao.save(IncomingCallEvent(number = "10086", createdTime = 100))

            assertEquals(id, dao.observeLatest().first()?.id)
            assertEquals(listOf(id), dao.findPending().map(IncomingCallEvent::id))

            dao.updateStatus(id, BaseEvent.Status.SENT, updatedTime = 200)

            assertTrue(dao.findPending().isEmpty())
            assertEquals(200, dao.observeLatest().first()?.updatedTime)
        }
}

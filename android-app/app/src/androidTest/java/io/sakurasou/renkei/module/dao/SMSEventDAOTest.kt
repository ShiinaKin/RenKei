package io.sakurasou.renkei.module.dao

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.sakurasou.renkei.module.AppDatabase
import io.sakurasou.renkei.module.entity.BaseEvent
import io.sakurasou.renkei.module.entity.SMSEvent
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SMSEventDAOTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: SMSEventDAO

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder<AppDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()
        dao = database.smsEventDAO()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertUpdateAndDeleteById() =
        runBlocking {
            val id =
                dao.saveSMSEvent(
                    SMSEvent(
                        number = "10086",
                        message = "hello",
                        receivedAt = 90,
                        createdTime = 100,
                    ),
                )

            assertTrue(id > 0)
            assertEquals(BaseEvent.Status.WAITING, dao.findSMSByID(id)?.status)
            assertEquals(listOf(id), dao.findPending().map(SMSEvent::id))

            val updatedRows =
                dao.updateSMSEventStatus(
                    id = id,
                    status = BaseEvent.Status.SENT,
                    updatedTime = 200,
                )

            assertEquals(1, updatedRows)
            assertEquals(BaseEvent.Status.SENT, dao.findSMSByID(id)?.status)
            assertTrue(dao.findPending().isEmpty())
            assertEquals(200, dao.findSMSByID(id)?.updatedTime)
            assertEquals(1, dao.deleteSMSEventByID(id))
            assertNull(dao.findSMSByID(id))
        }

    @Test
    fun latestQueriesUseIdAsTimestampTieBreaker() =
        runBlocking {
            dao.saveSMSEvent(
                SMSEvent(
                    number = "10010",
                    message = "first",
                    receivedAt = 90,
                    createdTime = 100,
                ),
            )
            dao.saveSMSEvent(
                SMSEvent(
                    number = "10010",
                    message = "second",
                    receivedAt = 90,
                    createdTime = 100,
                ),
            )

            assertEquals("second", dao.findLatest()?.message)
            assertEquals("second", dao.findSMSByPhoneNumber("10010")?.message)
        }
}

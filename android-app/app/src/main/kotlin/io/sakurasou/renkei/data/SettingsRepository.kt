package io.sakurasou.renkei.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sakurasou.renkei.settings.AppSettings
import io.sakurasou.renkei.settings.AppSettingsSerializer
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

private const val SETTINGS_FILE_NAME = "app_settings.json"

private val Context.appSettingsDataStore: DataStore<AppSettings> by dataStore(
    fileName = SETTINGS_FILE_NAME,
    serializer = AppSettingsSerializer,
)

@Singleton
class SettingsRepository
@Inject
constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.appSettingsDataStore

    val settings: Flow<AppSettings> =
        dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(AppSettings())
            } else {
                throw exception
            }
        }

    suspend fun update(settings: AppSettings) {
        dataStore.updateData { settings }
    }
}

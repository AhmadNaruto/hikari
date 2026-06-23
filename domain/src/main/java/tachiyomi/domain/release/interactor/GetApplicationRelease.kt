package tachiyomi.domain.release.interactor

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService
import java.time.Instant
import java.time.temporal.ChronoUnit

class GetApplicationRelease(
    private val service: ReleaseService,
    private val preferenceStore: PreferenceStore,
) {

    private val lastChecked: Preference<Long> by lazy {
        preferenceStore.getLong(Preference.appStateKey("last_app_check"), 0)
    }

    suspend fun await(arguments: Arguments): Result {
        val now = Instant.now()

        if (!arguments.forceCheck && now.isBefore(
                Instant.ofEpochMilli(lastChecked.get()).plus(3, ChronoUnit.DAYS),
            )
        ) {
            return Result.NoNewUpdate
        }

        val release = service.latest(arguments) ?: return Result.NoNewUpdate

        lastChecked.set(now.toEpochMilli())
        val isNewVersion = isNewVersion(
            arguments.commitCount,
            arguments.versionName,
            release.version,
        )
        return when {
            isNewVersion -> Result.NewUpdate(release)
            else -> Result.NoNewUpdate
        }
    }

    private fun isNewVersion(
        commitCount: Int,
        versionName: String,
        versionTag: String,
    ): Boolean {
        val newVersion = versionTag.replace("[^\\d.]".toRegex(), "")
        val oldVersion = versionName.replace("[^\\d.]".toRegex(), "")

        val newSemVer = newVersion.split(".").mapNotNull { it.toIntOrNull() }
        val oldSemVer = oldVersion.split(".").mapNotNull { it.toIntOrNull() }

        val maxSize = maxOf(newSemVer.size, oldSemVer.size)
        for (i in 0 until maxSize) {
            val newPart = newSemVer.getOrElse(i) { 0 }
            val oldPart = oldSemVer.getOrElse(i) { 0 }
            if (newPart > oldPart) return true
            if (newPart < oldPart) return false
        }

        return false
    }

    data class Arguments(
        val commitCount: Int,
        val versionName: String,
        val repository: String,
        val forceCheck: Boolean = false,
    )

    sealed interface Result {
        data class NewUpdate(val release: Release) : Result
        data object NoNewUpdate : Result
        data object OsTooOld : Result
    }
}

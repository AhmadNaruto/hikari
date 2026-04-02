package hikari.core.migration.migrations

import eu.kanade.domain.base.BasePreferences
import hikari.core.common.FeatureFlags
import hikari.core.migration.Migration
import hikari.core.migration.MigrationContext
import kotlin.uuid.ExperimentalUuidApi

class InstallationIdMigration : Migration {
    override val version: Float = Migration.ALWAYS

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val installationId = migrationContext.get<BasePreferences>()?.installationId ?: return false
        if (!installationId.isSet()) installationId.set(FeatureFlags.newInstallationId())
        return true
    }
}

package hikari.domain.extensionrepo.interactor

import hikari.domain.extensionrepo.model.ExtensionRepo
import hikari.domain.extensionrepo.repository.ExtensionRepoRepository

class ReplaceExtensionRepo(
    private val repository: ExtensionRepoRepository,
) {
    suspend fun await(repo: ExtensionRepo) {
        repository.replaceRepo(repo)
    }
}

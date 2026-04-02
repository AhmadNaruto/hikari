package hikari.domain.extensionrepo.interactor

import hikari.domain.extensionrepo.repository.ExtensionRepoRepository

class GetExtensionRepoCount(
    private val repository: ExtensionRepoRepository,
) {
    fun subscribe() = repository.getCount()
}

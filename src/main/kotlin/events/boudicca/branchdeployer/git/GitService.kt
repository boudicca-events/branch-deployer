package events.boudicca.branchdeployer.git

import events.boudicca.branchdeployer.BranchDeployerProperties
import org.eclipse.jgit.api.Git
import org.springframework.stereotype.Service

@Service
class GitService(
    private val branchDeployerProperties: BranchDeployerProperties
) {

    fun getAllRemoteBranches(): List<String> {
        return Git.lsRemoteRepository()
            .setRemote(branchDeployerProperties.gitRepository)
            .setHeads(true)
            .call()
            .map {
                it.name.removePrefix("refs/heads/")
            }
    }

}
package events.boudicca.branchdeployer.git

import org.eclipse.jgit.api.Git
import org.springframework.stereotype.Service

@Service
class GitService {

    fun getAllRemoteBranchesForRemote(remote: String): List<String> {
        return Git.lsRemoteRepository()
            .setRemote(remote)
            .setHeads(true)
            .call()
            .map {
                it.name.removePrefix("refs/heads/")
            }
    }

}
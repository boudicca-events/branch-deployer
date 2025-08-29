package events.boudicca.branchdeployer.docker

import events.boudicca.branchdeployer.BRANCH_LABEL
import events.boudicca.branchdeployer.DEPLOYMENTTYPE_LABEL
import events.boudicca.branchdeployer.GITREPOSITORY_LABEL
import events.boudicca.branchdeployer.URL_LABEL

data class DockerContainer(
    val id: String,
    val image: String,
    val name: String,
    val labels: Map<String, String>
) {
    fun getBranch(): String {
        return labels[BRANCH_LABEL]!!
    }

    fun getUrl(): String {
        return labels[URL_LABEL]!!
    }

    fun getGitRepository(): String {
        return labels[GITREPOSITORY_LABEL]!!
    }

    fun getDeploymentType(): String {
        return labels[DEPLOYMENTTYPE_LABEL]!!
    }
}

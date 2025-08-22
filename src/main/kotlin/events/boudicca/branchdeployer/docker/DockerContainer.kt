package events.boudicca.branchdeployer.docker

import events.boudicca.branchdeployer.BRANCH_LABEL
import events.boudicca.branchdeployer.URL_LABEL

data class DockerContainer(
    val id: String,
    val name: String,
    val labels: Map<String, String>
) {
    fun getBranch(): String {
        return labels[BRANCH_LABEL]!!
    }

    fun getUrl(): String {
        return labels[URL_LABEL]!!
    }
}

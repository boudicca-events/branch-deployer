package events.boudicca.branchdeployer

import events.boudicca.branchdeployer.docker.DockerContainerCreate
import events.boudicca.branchdeployer.docker.DockerService
import org.springframework.stereotype.Service

private const val LABEL_PREFIX = "events.boudicca.branchdeployer"
const val MANAGED_LABEL = "$LABEL_PREFIX.managed"
const val BRANCH_LABEL = "$LABEL_PREFIX.branch"
const val CONTAINER_NAME_PREFIX = "branchdeployer_"

@Service
class DeploymentService(
    private val dockerService: DockerService,
    private val branchDeployerProperties: BranchDeployerProperties
) {

    fun getCurrentState(): DeploymentState {
        val allContainers = dockerService.getAllContainers()

        val relevantContainers = allContainers.filter {
            it.labels.any { it.key == MANAGED_LABEL && it.value == "true" }
        }

        return DeploymentState(
            relevantContainers.map {
                it.labels.entries.single { it.key == BRANCH_LABEL }.value
            }.sorted()
        )
    }

    fun deploy(request: DeploymentRequest) {
        val cleanedBranchName = cleanBranchName(request.branchName)

        dockerService.deploy(
            DockerContainerCreate(
                CONTAINER_NAME_PREFIX + cleanedBranchName,
                branchDeployerProperties.dockerImageName + ":" + cleanedBranchName,
                mapOf(
                    MANAGED_LABEL to "true",
                    BRANCH_LABEL to request.branchName
                )
            )
        )
    }

    private fun cleanBranchName(string: String): String {
        return string.filter { it.isLetter() }.lowercase()
    }
}

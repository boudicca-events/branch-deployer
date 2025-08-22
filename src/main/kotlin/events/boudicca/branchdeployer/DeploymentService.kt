package events.boudicca.branchdeployer

import events.boudicca.branchdeployer.docker.DockerContainer
import events.boudicca.branchdeployer.docker.DockerContainerCreate
import events.boudicca.branchdeployer.docker.DockerService
import io.github.oshai.kotlinlogging.KotlinLogging
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

    private val logger = KotlinLogging.logger {}

    fun getCurrentState(): DeploymentState {
        val relevantContainers = findAllManagedContainers()

        return DeploymentState(
            relevantContainers.map {
                it.labels.entries.single { it.key == BRANCH_LABEL }.value
            }.sorted()
        )
    }

    fun deploy(request: DeploymentRequest) {
        val cleanedBranchName = cleanBranchName(request.branchName)

        val containerId = findContainerIdForBranchName(request.branchName)
        if (containerId != null) {
            logger.info { "container for branch already exists, deleting old container" }
            dockerService.delete(containerId)
        }

        logger.info { "deploying new branch: $request" }
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

    private fun findContainerIdForBranchName(branchName: String): String? {
        return findAllManagedContainers()
            .firstOrNull { it.labels[BRANCH_LABEL] == branchName }
            ?.id
    }

    private fun cleanBranchName(string: String): String {
        return string.filter { it.isLetter() }.lowercase()
    }

    private fun findAllManagedContainers(): List<DockerContainer> {
        val allContainers = dockerService.getAllContainers()
        val relevantContainers = allContainers.filter {
            it.labels.any { it.key == MANAGED_LABEL && it.value == "true" }
        }
        return relevantContainers
    }
}

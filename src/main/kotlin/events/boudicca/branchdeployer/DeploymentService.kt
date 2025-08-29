package events.boudicca.branchdeployer

import events.boudicca.branchdeployer.docker.DockerContainer
import events.boudicca.branchdeployer.docker.DockerContainerCreate
import events.boudicca.branchdeployer.docker.DockerService
import events.boudicca.branchdeployer.git.GitService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit


private const val LABEL_PREFIX = "events.boudicca.branchdeployer"
const val MANAGED_LABEL = "$LABEL_PREFIX.managed"
const val BRANCH_LABEL = "$LABEL_PREFIX.branch"
const val URL_LABEL = "$LABEL_PREFIX.url"
const val DEPLOYMENTTYPE_LABEL = "$LABEL_PREFIX.deploymentType"
const val GITREPOSITORY_LABEL = "$LABEL_PREFIX.gitRepository"
const val CONTAINER_NAME_PREFIX = "branchdeployer_"

@Service
class DeploymentService(
    private val dockerService: DockerService,
    private val gitService: GitService,
    private val branchDeployerProperties: BranchDeployerProperties
) {

    private val logger = KotlinLogging.logger {}

    fun getCurrentState(): DeploymentState {
        val relevantContainers = findAllManagedContainers()

        return DeploymentState(
            relevantContainers.map {
                DeployedBranch(
                    it.getBranch(),
                    it.getGitRepository(),
                    it.getUrl(),
                    it.image,
                    it.getDeploymentType(),
                )
            }.sortedBy { it.branch }
        )
    }

    fun deploy(request: DeploymentRequest): DeploymentResult {
        val deployment = findDeploymentType(request.deploymentName)
        val cleanedBranchName = cleanBranchName(request.branchName)
        val imageName = request.dockerImageName
        val domain = request.deploymentName + "." + cleanedBranchName + "." + branchDeployerProperties.baseUrl
        val url = "https://$domain"
        val props = ReplacementProperties(
            domain,
            url,
            request.branchName,
            request.gitRepository,
            cleanedBranchName,
            imageName
        )

        val containerId = findContainerIdForBranchNameAndDeploymentType(request.branchName, request.deploymentName)
        if (containerId != null) {
            logger.info { "container for branch already exists, deleting old container" }
            dockerService.delete(containerId)
        }

        logger.info { "deploying new branch: $request" }
        dockerService.deploy(
            DockerContainerCreate(
                CONTAINER_NAME_PREFIX + cleanedBranchName + "_" + request.deploymentName,
                imageName,
                mapOf(
                    MANAGED_LABEL to "true",
                    BRANCH_LABEL to request.branchName,
                    URL_LABEL to url,
                    DEPLOYMENTTYPE_LABEL to request.deploymentName,
                    GITREPOSITORY_LABEL to request.gitRepository,
                ) + collectAdditionalLabels(deployment, props),
                generateBuildTarFile(deployment, props),
                if (branchDeployerProperties.dockerNetwork.isNullOrBlank()) null else branchDeployerProperties.dockerNetwork
            )
        )

        return DeploymentResult(url)
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
    fun cleanup() {
        logger.info { "starting cleanup" }
        val containers = findAllManagedContainers()
        logger.debug { "active containers: $containers" }
        val containersByGitRepo = containers.groupBy { it.getGitRepository() }

        containersByGitRepo.forEach {
            val activeBranches = gitService.getAllRemoteBranchesForRemote(it.key).toSet()
            logger.debug { "active branches for git repo ${it.key}: $activeBranches" }

            for (container in it.value) {
                if (!activeBranches.contains(container.getBranch())) {
                    logger.info { "cleaning up container ${container.name} for branch ${container.getBranch()} for git repo ${it.key}" }
                    dockerService.delete(container.id)
                }
            }
        }
    }

    private fun generateBuildTarFile(deployment: Deployment, props: ReplacementProperties): ByteArray {
        val arrayOutputStream = ByteArrayOutputStream()
        ArchiveStreamFactory()
            .createArchiveOutputStream<TarArchiveOutputStream>(ArchiveStreamFactory.TAR, arrayOutputStream)
            .use { tarOutputStream ->
                deployment.filesToCopy
                    .forEach {
                        val file = File(it.from)
                        val fileContent = props
                            .replaceAll(file.readText())
                            .toByteArray()
                        val entry = TarArchiveEntry(file)
                        entry.size = fileContent.size.toLong()
                        tarOutputStream.putArchiveEntry(entry)
                        tarOutputStream.write(fileContent)
                        tarOutputStream.closeArchiveEntry()
                    }

                val dockerfileBytes = generateDockerFile(deployment, props)
                val entry = TarArchiveEntry("Dockerfile")
                entry.size = dockerfileBytes.size.toLong()
                tarOutputStream.putArchiveEntry(entry)
                tarOutputStream.write(dockerfileBytes)
                tarOutputStream.closeArchiveEntry()
            }

        return arrayOutputStream.toByteArray()
    }

    private fun generateDockerFile(deployment: Deployment, props: ReplacementProperties): ByteArray {
        val sb = StringBuilder()

        sb.append("FROM ${props.imageName}").appendLine()
        deployment.filesToCopy
            .forEach {
                sb.append("ADD ${it.from} ${it.to}").appendLine()
            }

        return sb.toString().toByteArray()
    }

    private fun findDeploymentType(deploymentType: String): Deployment {
        return branchDeployerProperties.deployments.single { it.name == deploymentType }
    }

    private fun collectAdditionalLabels(
        deployment: Deployment,
        props: ReplacementProperties
    ): Map<String, String> {
        return deployment.labelsToAdd
            .associate {
                props.replaceAll(it.key) to props.replaceAll(it.value)
            }
    }

    private fun findContainerIdForBranchNameAndDeploymentType(branchName: String, deploymentType: String): String? {
        return findAllManagedContainers()
            .firstOrNull { it.getBranch() == branchName && it.getDeploymentType() == deploymentType }
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

    private data class ReplacementProperties(
        val domain: String,
        val url: String,
        val branch: String,
        val gitRepository: String,
        val cleanedBranch: String,
        val imageName: String,
    ) {
        fun replaceAll(string: String): String {
            return string.replace("%DOMAIN%", domain)
                .replace("%URL%", url)
                .replace("%BRANCH%", branch)
                .replace("%CLEAN_BRANCH%", cleanedBranch)
                .replace("%IMAGE%", imageName)
                .replace("%GIT_REPOSITORY%", gitRepository)
        }
    }
}

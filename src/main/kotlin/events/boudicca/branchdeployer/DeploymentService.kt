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
                    it.getUrl(),
                )
            }.sortedBy { it.branch }
        )
    }

    fun deploy(request: DeploymentRequest): DeploymentResult {
        val cleanedBranchName = cleanBranchName(request.branchName)
        val imageName = branchDeployerProperties.dockerImageName + ":branchdeployer-" + cleanedBranchName
        val domain = cleanedBranchName + "." + branchDeployerProperties.baseUrl
        val url = "https://" + domain
        val props = ReplacementProperties(
            domain,
            url,
            request.branchName,
            cleanedBranchName,
            imageName
        )

        val containerId = findContainerIdForBranchName(request.branchName)
        if (containerId != null) {
            logger.info { "container for branch already exists, deleting old container" }
            dockerService.delete(containerId)
        }

        logger.info { "deploying new branch: $request" }
        dockerService.deploy(
            DockerContainerCreate(
                CONTAINER_NAME_PREFIX + cleanedBranchName,
                imageName,
                mapOf(
                    MANAGED_LABEL to "true",
                    BRANCH_LABEL to request.branchName,
                    URL_LABEL to url,
                ) + collectAdditionalLabels(props),
                generateBuildTarFile(props)
            )
        )

        return DeploymentResult(url)
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
    fun cleanup() {
        logger.info { "starting cleanup" }
        val containers = findAllManagedContainers()
        logger.debug { "active containers: $containers" }
        val activeBranches = gitService.getAllRemoteBranches().toSet()
        logger.debug { "active branches: $activeBranches" }

        for (container in containers) {
            if (!activeBranches.contains(container.getBranch())) {
                logger.info { "cleaning up branch ${container.getBranch()}" }
                dockerService.delete(container.id)
            }
        }
    }

    private fun generateBuildTarFile(props: ReplacementProperties): ByteArray {
        val arrayOutputStream = ByteArrayOutputStream()
        ArchiveStreamFactory()
            .createArchiveOutputStream<TarArchiveOutputStream>(ArchiveStreamFactory.TAR, arrayOutputStream)
            .use { tarOutputStream ->
                branchDeployerProperties.filesToCopy
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

                val dockerfileBytes = generateDockerFile(props)
                val entry = TarArchiveEntry("Dockerfile")
                entry.size = dockerfileBytes.size.toLong()
                tarOutputStream.putArchiveEntry(entry)
                tarOutputStream.write(dockerfileBytes)
                tarOutputStream.closeArchiveEntry()
            }

        return arrayOutputStream.toByteArray()
    }

    private fun generateDockerFile(props: ReplacementProperties): ByteArray {
        val sb = StringBuilder()

        sb.append("FROM ${props.imageName}").appendLine()
        branchDeployerProperties.filesToCopy
            .forEach {
                sb.append("ADD ${it.from} ${it.to}").appendLine()
            }

        return sb.toString().toByteArray()
    }

    private fun collectAdditionalLabels(
        props: ReplacementProperties
    ): Map<String, String> {
        return branchDeployerProperties.labelsToAdd
            .associate {
                props.replaceAll(it.key) to props.replaceAll(it.value)
            }
    }

    private fun findContainerIdForBranchName(branchName: String): String? {
        return findAllManagedContainers()
            .firstOrNull { it.getBranch() == branchName }
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
        val cleanedBranch: String,
        val imageName: String,
    ) {
        fun replaceAll(string: String): String {
            return string.replace("%DOMAIN%", domain)
                .replace("%URL%", url)
                .replace("%BRANCH%", branch)
                .replace("%CLEAN_BRANCH%", cleanedBranch)
                .replace("%IMAGE%", imageName)
        }
    }
}

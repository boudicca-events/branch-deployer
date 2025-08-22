package events.boudicca.branchdeployer.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.DockerException
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream


@Service
@ConditionalOnBooleanProperty("deploy.useRealDocker", havingValue = true)
class RealDockerService : DockerService {

    private val logger = KotlinLogging.logger {}

    val dockerConfig: DockerClientConfig = DefaultDockerClientConfig
        .createDefaultConfigBuilder()
        .build()!!

    val dockerHttpClient = ZerodepDockerHttpClient.Builder()
        .dockerHost(dockerConfig.dockerHost)
        .sslConfig(dockerConfig.sslConfig)
        .build()!!

    var dockerClient: DockerClient = DockerClientImpl.getInstance(dockerConfig, dockerHttpClient)

    override fun getAllContainers(): List<DockerContainer> {
        val containers = try {
            dockerClient.listContainersCmd().exec()
        } catch (e: DockerException) {
            throw RuntimeException("error listing containers", e)
        }

        return containers.map {
            DockerContainer(
                it.id,
                it.names[0],
                it.labels
            )
        }
    }

    override fun deploy(create: DockerContainerCreate) {
        logger.info { "pulling image ${create.image}" }
        try {
            dockerClient.pullImageCmd(create.image)
                .start()
                .awaitCompletion()
        } catch (e: DockerException) {
            throw RuntimeException("error pulling image", e)
        }

        val newImageId = try {
            dockerClient.buildImageCmd()
                .withTarInputStream(ByteArrayInputStream(create.tar))
                .start()
                .awaitImageId()
        } catch (e: DockerException) {
            throw RuntimeException("error building image", e)
        }

        val containerId = try {
            var createContainerCmd = dockerClient.createContainerCmd(newImageId)
                .withName(create.name)
                .withLabels(create.labels)
            if (create.dockerNetwork != null) {
                createContainerCmd = createContainerCmd
                    .withHostConfig(
                        HostConfig.newHostConfig()
                            .withNetworkMode(create.dockerNetwork)
                    )
            }
            createContainerCmd
                .exec()
                .id
        } catch (e: DockerException) {
            throw RuntimeException("error creating container", e)
        }

        try {
            dockerClient.startContainerCmd(containerId)
                .exec()
        } catch (e: DockerException) {
            throw RuntimeException("error starting container", e)
        }
    }

    override fun delete(containerId: String) {
        try {
            dockerClient.stopContainerCmd(containerId)
                .exec()
        } catch (e: DockerException) {
            throw RuntimeException("error stopping container", e)
        }
        try {
            dockerClient.removeContainerCmd(containerId)
                .exec()
        } catch (e: DockerException) {
            throw RuntimeException("error removing container", e)
        }
    }
}
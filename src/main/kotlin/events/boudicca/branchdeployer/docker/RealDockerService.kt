package events.boudicca.branchdeployer.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.stereotype.Service


@Service
@ConditionalOnBooleanProperty("deploy.useRealDocker", havingValue = true)
class RealDockerService : DockerService {

    val dockerConfig: DockerClientConfig = DefaultDockerClientConfig
        .createDefaultConfigBuilder()
        .build()!!

    val dockerHttpClient = ZerodepDockerHttpClient.Builder()
        .dockerHost(dockerConfig.dockerHost)
        .sslConfig(dockerConfig.sslConfig)
        .build()!!

    var dockerClient: DockerClient = DockerClientImpl.getInstance(dockerConfig, dockerHttpClient)

    override fun getAllContainers(): List<DockerContainer> {
        val containers = dockerClient.listContainersCmd().exec()

        return containers.map {
            DockerContainer(
                it.id,
                it.names[0],
                it.labels
            )
        }
    }

    override fun deploy(create: DockerContainerCreate) {
        dockerClient.pullImageCmd(create.image)
            .start()
            .awaitCompletion()

        val containerId = dockerClient.createContainerCmd(create.image)
            .withName(create.name)
            .withLabels(create.labels)
            .exec()
            .id

        dockerClient.startContainerCmd(containerId)
            .exec()
    }

    override fun delete(containerId: String) {
        dockerClient.stopContainerCmd(containerId)
            .exec()
        dockerClient.removeContainerCmd(containerId)
            .exec()
    }
}
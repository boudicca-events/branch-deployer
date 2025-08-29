package events.boudicca.branchdeployer.docker

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.stereotype.Service
import kotlin.random.Random


@Service
@ConditionalOnBooleanProperty("deploy.useRealDocker", havingValue = false)
class MockDockerService : DockerService {

    val containers = mutableListOf<DockerContainer>()

    override fun getAllContainers(): List<DockerContainer> {
        return containers
    }

    override fun deploy(create: DockerContainerCreate) {
        containers.add(
            DockerContainer(
                Random.nextInt().toString(10),
                create.image,
                create.name,
                create.labels
            )
        )
    }

    override fun delete(containerId: String) {
        containers.removeAll { it.id == containerId }
    }
}
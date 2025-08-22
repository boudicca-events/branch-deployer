package events.boudicca.branchdeployer.docker

import events.boudicca.branchdeployer.BRANCH_LABEL
import events.boudicca.branchdeployer.MANAGED_LABEL
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.stereotype.Service


@Service
@ConditionalOnBooleanProperty("deploy.useRealDocker", havingValue = false)
class MockDockerService : DockerService {
    override fun getAllContainers(): List<DockerContainer> {
        return listOf(
            DockerContainer(
                "someRandomOtherContainer",
                mapOf(
                    "label" to "labelvalue",
                )
            ),
            DockerContainer(
                "branch1",
                mapOf(
                    MANAGED_LABEL to "true",
                    BRANCH_LABEL to "branch1",
                )
            ),
            DockerContainer(
                "branch2",
                mapOf(
                    MANAGED_LABEL to "true",
                    BRANCH_LABEL to "branch2",
                )
            ),
        )
    }

    override fun deploy(create: DockerContainerCreate) {
        println("request to deploy container $create")
    }
}
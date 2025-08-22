package events.boudicca.branchdeployer.docker

interface DockerService {
    fun getAllContainers(): List<DockerContainer>
    fun deploy(create: DockerContainerCreate)
}

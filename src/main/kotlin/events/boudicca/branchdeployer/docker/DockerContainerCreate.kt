package events.boudicca.branchdeployer.docker

data class DockerContainerCreate(
    val name: String,
    val image: String,
    val labels: Map<String, String>
)

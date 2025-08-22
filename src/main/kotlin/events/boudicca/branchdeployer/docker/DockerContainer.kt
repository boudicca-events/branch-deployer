package events.boudicca.branchdeployer.docker

data class DockerContainer(
    val id: String,
    val name: String,
    val labels: Map<String, String>
)

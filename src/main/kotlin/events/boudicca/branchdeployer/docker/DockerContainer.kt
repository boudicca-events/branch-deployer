package events.boudicca.branchdeployer.docker

data class DockerContainer(
    val name: String,
    val labels: Map<String, String>
)

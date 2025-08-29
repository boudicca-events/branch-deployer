package events.boudicca.branchdeployer

data class DeploymentRequest(
    val deploymentName: String,
    val branchName: String,
    val gitRepository: String,
    val dockerImageName: String,
)

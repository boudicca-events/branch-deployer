package events.boudicca.branchdeployer

data class DeploymentState(val deployedBranches: List<DeployedBranch>)

data class DeployedBranch(
    val branch: String,
    val url: String,
)

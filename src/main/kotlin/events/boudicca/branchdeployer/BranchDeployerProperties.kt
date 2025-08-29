package events.boudicca.branchdeployer

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "deploy")
data class BranchDeployerProperties(
    val password: String,
    val baseUrl: String,
    val dockerNetwork: String?,
    val deployments: List<Deployment>,
)

data class Deployment(
    val name: String,
    val filesToCopy: List<FileCopy>,
    val labelsToAdd: List<Label>,
)

data class FileCopy(
    val from: String,
    val to: String,
)

data class Label(
    val key: String,
    val value: String,
)

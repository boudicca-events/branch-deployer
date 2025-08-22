package events.boudicca.branchdeployer

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "deploy")
data class BranchDeployerProperties(
    val password: String,
    val gitRepository: String,
    val dockerImageName: String,
    val configTemplatePath: String,
    val configPlacementPath: String,
    val gitRepositoryCacheFolder: String,
)

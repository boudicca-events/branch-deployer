package events.boudicca.branchdeployer

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "deploy")
data class BranchDeployerProperties(val tmp: String?)

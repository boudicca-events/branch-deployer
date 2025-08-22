package events.boudicca.branchdeployer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController("/")
class Controller(private val deploymentService: DeploymentService) {

    private val logger = KotlinLogging.logger {}

    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun helloWorld(): DeploymentState {
        try {
            return deploymentService.getCurrentState()
        } catch (e: Exception) {
            logger.error(e) { "error while getting current state" }
            throw e
        }
    }

    @PostMapping(
        "/deploy",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun deploy(@RequestBody deploymentRequest: DeploymentRequest): DeploymentResult {
        try {
            return deploymentService.deploy(deploymentRequest)
        } catch (e: Exception) {
            logger.error(e) { "error while deploying" }
            throw e
        }
    }

    @PostMapping(
        "/deploy/cleanup",
    )
    fun cleanup() {
        try {
            deploymentService.cleanup()
        } catch (e: Exception) {
            logger.error(e) { "error while cleaning up" }
            throw e
        }
    }
}
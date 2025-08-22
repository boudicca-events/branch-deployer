package events.boudicca.branchdeployer

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController("/")
class Controller(private val deploymentService: DeploymentService) {

    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun helloWorld(): DeploymentState {
        return deploymentService.getCurrentState()
    }

    @PostMapping(
        "/deploy",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun deploy(@RequestBody deploymentRequest: DeploymentRequest) {
        deploymentService.deploy(deploymentRequest)
    }
}
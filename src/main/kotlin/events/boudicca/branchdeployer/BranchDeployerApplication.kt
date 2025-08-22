package events.boudicca.branchdeployer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BranchDeployerProperties::class)
class BranchDeployerApplication : WebMvcConfigurer {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests {
            it.requestMatchers("/deploy").hasRole("DEPLOY")
            it.requestMatchers("/deploy/**").hasRole("DEPLOY")
            it.requestMatchers("/**").permitAll()
        }
        http.httpBasic(withDefaults())
        http.csrf { it.disable() }
        return http.build()
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
    }

    @Bean
    fun users(branchDeployerProperties: BranchDeployerProperties): UserDetailsService {
        val ingestUser = User.builder()
            .username("deploy")
            .password("{noop}" + branchDeployerProperties.password)
            .roles("DEPLOY")
            .build()
        return InMemoryUserDetailsManager(ingestUser)
    }
}

fun main(args: Array<String>) {
    runApplication<BranchDeployerApplication>(*args)
}

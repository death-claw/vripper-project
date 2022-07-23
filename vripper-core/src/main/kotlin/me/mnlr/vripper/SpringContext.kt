package me.mnlr.vripper

import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import me.mnlr.vripper.delegate.LoggerDelegate

@Component
@EnableScheduling
class SpringContext : ApplicationContextAware {
    @Throws(BeansException::class)
    override fun setApplicationContext(context: ApplicationContext) {

        // store ApplicationContext reference to access required beans later on
        Companion.context = context as ConfigurableApplicationContext
    }

    companion object {
        private val log by LoggerDelegate()
        private lateinit var context: ConfigurableApplicationContext
        fun <T> getBean(beanClass: Class<T>): T {
            return context.getBean(beanClass)
        }

        fun <T> getBeansOfType(beanClass: Class<T>): Map<String, T> {
            return context.getBeansOfType(beanClass)
        }

        fun close() {
            log.info("Application terminating...")
            context.close()
        }
    }
}
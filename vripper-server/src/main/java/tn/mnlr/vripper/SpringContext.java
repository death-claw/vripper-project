package tn.mnlr.vripper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SpringContext implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SpringContext.class);

    private static ConfigurableApplicationContext context;

    /**
     * Returns the Spring managed bean instance of the given class type (if it exists).
     * Returns null otherwise.
     *
     * @param beanClass
     * @return
     */
    public static <T extends Object> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    public static <T extends Object> Map<String, T> getBeansOfType(Class<T> beanClass) {
        return context.getBeansOfType(beanClass);
    }

    public static void close() {
        logger.info("Application terminating...");
        context.close();
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {

        // store ApplicationContext reference to access required beans later on
        SpringContext.context = ((ConfigurableApplicationContext) context);
    }
}
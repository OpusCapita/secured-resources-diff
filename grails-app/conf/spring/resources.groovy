import org.springframework.beans.factory.config.PropertiesFactoryBean

// Place your Spring DSL code here
beans = {
    configurationProperties(PropertiesFactoryBean) {
        location = "/WEB-INF/conf/configuration.properties"
    }
}

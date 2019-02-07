import org.springframework.beans.factory.config.PropertiesFactoryBean

beans = {
    configurationProperties(PropertiesFactoryBean) {
        locations = ["classpath:default-configuration.properties", "/WEB-INF/conf/configuration.properties"]
    }
}

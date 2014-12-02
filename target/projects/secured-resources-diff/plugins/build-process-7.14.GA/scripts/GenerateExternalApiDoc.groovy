import com.jcatalog.tool.ExternalApiDocGenerator
import org.apache.commons.logging.impl.SimpleLog

final String apiAnnotation = "com.jcatalog.util.annotation.Api"
final String outputPath = "externalapidocs"

final String organizationName = "jCatalog Software AG."
final String organizationUrl = "http://www.jcatalog.com/"

final List externalLinks = [
        "http://download.oracle.com/javase/6/docs/api/",
        "http://static.springsource.org/spring/docs/3.0.x/javadoc-api/",
        "http://docs.jboss.org/hibernate/core/3.3/api/",
        "http://docs.jboss.org/hibernate/annotations/3.4/api/",
        "http://commons.apache.org/lang/api-release/",
        "http://commons.apache.org/collections/api-release/",
        "http://commons.apache.org/beanutils/api/",
        "http://commons.apache.org/logging/apidocs/",
        "http://commons.apache.org/io/api-release/",
        "http://commons.apache.org/dbcp/api-1.2.2/",
        "http://commons.apache.org/pool/apidocs/",
        "http://commons.apache.org/vfs/apidocs/",
        "http://commons.apache.org/codec/api-release/",
        "http://commons.apache.org/el/apidocs/",
        "http://commons.apache.org/fileupload/apidocs/",
        "http://commons.apache.org/net/api-3.0.1/",
        "http://hc.apache.org/httpclient-legacy/apidocs/",
        "http://commons.apache.org/digester/commons-digester-1.8/docs/api/",
        "http://commons.apache.org/validator/api-1.3.1/",
        "http://kentbeck.github.com/junit/javadoc/latest/",
        "http://logging.apache.org/log4j/1.2/apidocs/",
        "http://www.dbunit.org/apidocs/",
        "http://www.slf4j.org/apidocs/",
        "http://static.springsource.org/spring-ldap/docs/1.3.x/apidocs/",
        "http://aopalliance.sourceforge.net/doc/",
        "http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/html/",
        "http://groovy.codehaus.org/api/",
        "http://mx4j.sourceforge.net/docs/api/",
        "http://easymock.org/api/easymock/2.5.2/",
        "http://powermock.googlecode.com/svn/docs/powermock-1.3.1/apidocs/",
        "http://velocity.apache.org/engine/releases/velocity-1.4/api/",
        "http://jexcelapi.sourceforge.net/resources/javadocs/2_6/docs/",
        "http://opencsv.sourceforge.net/apidocs/",
        "http://xstream.codehaus.org/javadoc/",
        "http://api.dpml.net/org/apache/ant/1.7.0/",
        "http://download.oracle.com/javaee/5/api/",
        "http://docs.oracle.com/javaee/5/jstl/1.1/docs/api/",
        "http://myfaces.apache.org/core11/myfaces-api/apidocs/",
        "http://myfaces.apache.org/core11/myfaces-impl/apidocs/",
        "http://myfaces.apache.org/tomahawk-project/tomahawk/apidocs/",
        "http://dom4j.sourceforge.net/dom4j-1.6.1/apidocs/",
        "http://cglib.sourceforge.net/apidocs/",
        "http://docs.jboss.org/envers/api/"
]

includeTargets << grailsScript("_GrailsPackage")

target('default': "Generate external API docs.") {
    depends(compile)
    depends(createConfig)
    generateExternalApiDoc()
}

/**
 * Generate external api docs after main docs were generated.
 * @author Eugeny Pozharsky
 */
target(generateExternalApiDoc: "Generate external API docs.") {
    // configure a simple logger because there are no loggers configured at this build step
    ExternalApiDocGenerator generator = new ExternalApiDocGenerator()
    generator.setAdditionalRoots(config.grails?.doc?.externalApi?.additionalRoots ?: null)
    generator.setApiAnnotation(apiAnnotation)
    generator.setBaseDir(grailsSettings.baseDir.path)
    generator.setDependencies(grailsSettings.compileDependencies.collect{return it.canonicalPath})
    generator.setInceptionYear(config.grails?.doc?.externalApi?.inceptionYear ?: "")
    generator.setLog(new SimpleLog("externalApiGenerator"))
    generator.setOrganizationName(config.grails?.doc?.externalApi?.organizationName ?: organizationName)
    generator.setOrganizationUrl(config.grails?.doc?.externalApi?.organizationUrl ?: organizationUrl)
    generator.setOutputPath(new File(grailsSettings.docsOutputDir.path - (grailsSettings.baseDir.path + File.separator), config.grails?.doc?.externalApi?.outputPath ?: outputPath).getCanonicalPath())
    generator.setPredefinedLinks(externalLinks)
    generator.setProjectName(config.grails?.doc?.externalApi?.projectName ?: grailsAppName)
    generator.setProjectVersion(config.grails?.doc?.externalApi?.projectVersion ?: grailsAppVersion)
    generator.execute()
}

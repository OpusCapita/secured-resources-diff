import grails.util.GrailsUtil
import groovy.xml.DOMBuilder
import groovy.xml.XmlUtil
import groovy.xml.dom.DOMCategory
import grails.util.Environment
import grails.util.Metadata
import grails.doc.DocPublisher
import org.radeox.macro.BaseMacro
import org.radeox.macro.MailToMacro
import org.radeox.macro.parameter.MacroParameter

//includeTargets << new File("${buildProcessPluginDir}/scripts/GenerateExternalApiDoc.groovy")

//eventDocEnd = { kind->
//    if (kind == "groovydoc") {
//        generateExternalApiDoc()
//    }
//}

extractWebResources = {
    long now = System.currentTimeMillis()
    println 'Extracting web resources ...'
    ant.mkdir(dir: 'web-app')
    // touch possible changeable files
    ant.touch() {
        fileset(dir: "web-app") {
            include(name: "**/*")
        }
    }
    // extract web resources from jars
    def dependencyResources = buildConfig.grails.project.dependencyResources.collect{new File(it.split(":").collect{it.replace(".", File.separator)}.join(File.separator)).path}

    Set filteredDependencies = grailsSettings.buildDependencies.findAll {dep ->
        dep.absolutePath.replace(File.separator, '.') =~ /com\.jcatalog/ &&
                dependencyResources.any {d -> dep.absolutePath.indexOf(d) > 0}
    } as Set
    
    filteredDependencies.each {f ->
        println "unzip resource - $f"
        ant.unzip(src: f.absolutePath, dest: 'web-app', overwrite: true) {
            patternset() {
                exclude(name: 'META-INF/**')
                exclude(name: 'WEB-INF/**')
            }
        }

        ant.unzip(src: f.absolutePath, dest: grailsSettings.classesDir, overwrite: true) {
            patternset() {
                include(name: 'WEB-INF/classes/**')
            }
            globmapper(from: 'WEB-INF/classes/*', to: '*')
        }
    }
    println 'Web resources extracted in ' + (System.currentTimeMillis() - now) + 'ms.'
}

generateAppVersionAndBuildInfo = {
    Date buildDate = new Date()

    println "Creating war version and build infos  (${buildDate}) ..."

    Metadata metadata = Metadata.current
    def version = metadata.getApplicationVersion()
    def appName = metadata.getApplicationName() ?: baseDir.name
    ant.echo(file: "${grailsSettings.classesDir.path}/${appName}.version",
            message: "Module: ${appName}, Build version: ${version}, Build time: ${String.format('%tY.%<tm.%<td at %<tH:%<tM %<tZ', buildDate)}")

    def message = """
build.time.year = ${String.format('%tY', buildDate)}
build.time.month = ${String.format('%tm', buildDate)}
build.time.day = ${String.format('%td', buildDate)}
build.time.zone = ${String.format('%tZ', buildDate)}
build.application.name = ${appName}
build.application.version = ${version}"""

    ant.echo(file: "${grailsSettings.classesDir.path}/buildinfo.properties", message: message)
}

eventCompileEnd = {
    println "Preparing project for Grails before end compilation ..."
    generateAppVersionAndBuildInfo()
    extractWebResources()
    println "Preparing project for Grails before end compilation finished."
}

eventCreateWarEnd = {warName, stagingDir ->
    ant.exec(outputproperty: "sourceCodeRevision", failonerror: "true", executable: 'hg')
            {
                arg(line: "id -i")
            }
    println "Current source code revision: ${sourceCodeRevision}"

    ant.jar(destfile: warName, update: true) {
        manifest {
            attribute(name: "RepositoryUrl", value: buildConfig.grails.project.sourceUrl)
            attribute(name: "Revision", value: "${sourceCodeRevision}")
        }
    }
}


/**
 * Re-write the web.xml to order the servlet filters the way we need
 *
 * @author Dmitry Divin
 */
eventWebXmlEnd = { String filename ->
    try {
        fixWebXml()
    }
    catch (e) {
        GrailsUtil.deepSanitize e
        e.printStackTrace()
    }
}

private void fixWebXml() {
    int DEFAULT_POSITION = 500

    def webXmlFilterOrder = getConfig()?.webXmlFilterOrder
    if (!webXmlFilterOrder) {
        return;
    }
    def wxml = DOMBuilder.parse(new StringReader(webXmlFile.text)).documentElement
    def sorted = new TreeMap()
    def defaultPositionNames = []

    sorted[DEFAULT_POSITION] = defaultPositionNames

    def orderedNames = []
    webXmlFilterOrder.each { k, v ->
        // invert the map; new map key is int (order) and value is list of names for that order
        def list = sorted[v]
        if (!list) {
            list = []
        }
        list << k
        orderedNames << k
        sorted[v] = list
    }

    for (String name in findFilterMappingNames(wxml)) {
        if (!orderedNames.contains(name)) {
            defaultPositionNames << name
        }
    }
    def orderedFilterNames = (sorted.values() as List).flatten()

    sortFilterMappingNodes(wxml, orderedFilterNames)

    webXmlFile.withWriter { it << XmlUtil.serialize(wxml) }
}

private Set<String> findFilterMappingNames(dom) {
    Set names = []

    use(DOMCategory) {
        def mappingNodes = dom.'filter-mapping'
        mappingNodes.each { n ->
            names << n.'filter-name'.text()
        }
    }

    names
}

private void sortFilterMappingNodes(dom, orderedFilterNames) {
    def sortedMappingNodes = []
    def followingNode

    use(DOMCategory) {
        def mappingNodes = dom.'filter-mapping'
        if (mappingNodes.size()) {
            followingNode = mappingNodes[-1].nextSibling

            Set doneFilters = []
            orderedFilterNames.each { f ->
                mappingNodes.each { n ->
                    def filterName = n.'filter-name'.text()
                    if (!(filterName in doneFilters)) {
                        if (filterName == f || (f == '*' && !orderedFilterNames.contains(filterName))) {
                            sortedMappingNodes << n
                            doneFilters << n
                        }
                    }
                }
            }

            mappingNodes.each { dom.removeChild(it) }
        }
    }

    sortedMappingNodes.each { dom.insertBefore(it, followingNode) }
}

private getConfig() {
    GroovyClassLoader loader = new GroovyClassLoader(getClass().classLoader)

    def config
    try {
        def defaultConfigFile = loader.loadClass("Config")
        config = new ConfigSlurper(Environment.current.name).parse(defaultConfigFile)
    }
    catch (ClassNotFoundException e) {
    }
    return config?.webxml
}

def pluginLinkMacro = new BaseMacro() {

    @Override
    String getName() {
        return "pluginLink"
    }

    @Override
    void execute(Writer writer, MacroParameter params) throws IllegalArgumentException, IOException {
        if(params.length < 1){
            writer << '<div style="color:red;">Invalid Parameter number: pluginLink macro needs at least one parameter: {pluginLink:pluginName}</div>'
            return
        }
        String pluginName = params.get(0)
        String linkContent = params.get(1)

        File pluginDescriptor = searchDescriptorInPluginXmlMetadata(pluginName) ?: searchDescriptorInGrailsSettings(pluginName)

        if(!pluginDescriptor){
            String  message = "Plugin with name ${pluginName} wasn't found."
            writer << '<div style="color:red;">' + message + '</div>'
            return
        }
        def descriptorInstance = new GroovyClassLoader().parseClass(pluginDescriptor).newInstance()

        if (descriptorInstance.hasProperty("documentation") && descriptorInstance.documentation) {
            writer << "<a href='${descriptorInstance.documentation}'>${linkContent ?: pluginName}</a>"
        }
    }
}

private File searchDescriptorInPluginXmlMetadata(String pluginToSearch){
    for (p in readAllPluginXmlMetadata()) {
        String pluginName = p.@name.text()
        if(pluginToSearch == pluginName){
            return new File(grailsSettings.projectPluginsDir, "${pluginName}-${p.@version.text()}").listFiles().find {  File it -> it.name.endsWith("GrailsPlugin.groovy")}

        }
    }
    return null
}

private File searchDescriptorInGrailsSettings(String pluginToSearch){
    Map.Entry pluginDescriptorEntry = grailsSettings.config.grails?.plugin?.location?.find { String name, def location ->
        def applicationProps = new Properties()
        applicationProps.load(new File(location, "application.properties").newInputStream())
        applicationProps["app.name"] == pluginToSearch
    }
    return pluginDescriptorEntry ? new File(pluginDescriptorEntry.value).listFiles().find { File it -> it.name.endsWith("GrailsPlugin.groovy") } : null
}


/**
 * Rebuild documentation guide with {pluginLinks} processing macros
 * during generate documentation.
 *
 * @author Dmitry Divin
 */
eventDocEnd = {docType ->
    if (docType == 'refdocs') {

        def srcDocs = new File("${basedir}/src/docs")

        File refDocsDir = grailsSettings.docsOutputDir
        def publisher = new DocPublisher(srcDocs, refDocsDir, grailsConsole)
        publisher.ant = ant
        publisher.title = grailsAppName
        publisher.subtitle = grailsAppName
        publisher.version = grailsAppVersion
        publisher.authors = ""
        publisher.license = ""
        publisher.copyright = ""
        publisher.footer = ""
        publisher.engineProperties = config?.grails?.doc?.flatten()
        // if this is a plugin obtain additional metadata from the plugin
        readPluginMetadataForDocs(publisher)
        readDocProperties(publisher)
        configureAliases()

        ['pluginsLinks', 'pluginLinks'].each {
            publisher.registerMacro(new BaseMacro() {
                String getName() {it}
                void execute(Writer writer, MacroParameter params) {
                    processingPluginLinks(writer, params)
                }
            })
        }

        publisher.registerMacro(pluginLinkMacro)
        publisher.publish()
    }
}



private processingPluginLinks(Writer writer, MacroParameter params) {
    List plugins = []

    for (p in readAllPluginXmlMetadata()) {
        String pluginName = p.@name.text()
        String pluginVersion = p.@version.text()
        File pluginDescriptor = new File(grailsSettings.projectPluginsDir, "${pluginName}-${pluginVersion}").listFiles().find {  File it -> it.name.endsWith("GrailsPlugin.groovy")}
        plugins << [name: pluginName,  version: pluginVersion, descriptor: pluginDescriptor]
    }
    grailsSettings.config.grails?.plugin?.location?.each {String name, def location ->
        def applicationProps = new Properties()
        applicationProps.load(new File(location, "application.properties").newInputStream())
        String pluginName = applicationProps["app.name"]
        String pluginVersion = applicationProps["app.version"]
        File pluginDescriptor = new File(location).listFiles().find {  File it -> it.name.endsWith("GrailsPlugin.groovy")}
        plugins << [name: pluginName, version: pluginVersion, descriptor: pluginDescriptor]
    }
    if (!plugins) {
        return
    }

    GroovyClassLoader groovyClassLoader = new GroovyClassLoader()

    writer << "<dl>"
    for (plugin in plugins.sort {a,b -> a.name <=> b.name}) {
        Class descriptorClass = groovyClassLoader.parseClass(plugin.descriptor)
        def descriptorInstance = descriptorClass.newInstance()
        String pluginLabel = plugin.name
        if (descriptorInstance.hasProperty("title") && descriptorInstance.title) {
            pluginLabel = "${descriptorInstance.title} (${plugin.name})"
        }

        if (descriptorInstance.hasProperty("documentation") && descriptorInstance.documentation) {
            writer << "<dt><a href='${descriptorInstance.documentation}'>${pluginLabel}</a></dt>"
        } else {
            writer << "<dt>${pluginLabel}</dt>"
        }

        if (descriptorInstance.hasProperty("description") && descriptorInstance.description) {
            writer << "<dd>${descriptorInstance.description}</dd>"
        } else {
            writer << "<dd>&nbsp;</dd>"
        }
    }
    writer << "</dl>"
}


private loadBasePlugin() {
    pluginManager?.allPlugins?.find { it.basePlugin }
}

def configureAliases() {
    // See http://jira.codehaus.org/browse/GRAILS-6484 for why this is soft loaded
    def docEngineClassName = "grails.doc.DocEngine"
    def docEngineClass = classLoader.loadClass(docEngineClassName)
    if (!docEngineClass) {
        throw new IllegalStateException("Failed to load $docEngineClassName to configure documentation aliases")
    }
    docEngineClass.ALIAS.putAll(config.grails.doc.alias)
}

def readPluginMetadataForDocs(DocPublisher publisher) {
    def basePlugin = loadBasePlugin()?.instance
    if (basePlugin) {
        if (basePlugin.hasProperty("title")) {
            publisher.title = basePlugin.title
        }
        if (basePlugin.hasProperty("description")) {
            publisher.subtitle = basePlugin.description
        }
        if (basePlugin.hasProperty("version")) {
            publisher.version = basePlugin.version
        }
        if (basePlugin.hasProperty("license")) {
            publisher.license = basePlugin.license
        }
        if (basePlugin.hasProperty("author")) {
            publisher.authors = basePlugin.author
        }
    }
}

def readDocProperties(DocPublisher publisher) {
    ['copyright', 'license', 'authors', 'footer', 'images',
            'css', 'style', 'encoding', 'logo', 'sponsorLogo'].each { readIfSet publisher, it }
}

private readIfSet(DocPublisher publisher,String prop) {
    if (config.grails.doc."$prop") {
        publisher[prop] = config.grails.doc."$prop"
    }
}
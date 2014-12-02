import org.apache.commons.io.FilenameUtils

import java.nio.file.Path
import java.util.regex.Pattern

includeTargets << grailsScript("Compile")

/**
 * The goal copy all resources from plugins to customizationarea
 * Usage: grails add-plugin-resource-to-customization-area
 *
 * @author Dmitry Divin
 */
String customizationAreaPluginsPath = "web-app/WEB-INF/standardcustomizationarea/webapp/plugins"
target(addPluginResourcesToCustomizationArea: "Add plugin resource to customization area") {
    depends(compile, resolveDependencies)

    File destDir = new File(customizationAreaPluginsPath, new File("."))

    for (item in pluginsFiles) {

        File pluginDir = new File(item.key, destDir)

        File i18nDir = new File("i18n", pluginDir)
        File viewsDir = new File("views", pluginDir)

        for (gspItem in item.value.gsp) {
            ant.copy(file: gspItem.file, tofile: new File(gspItem.relativePath, viewsDir), overwrite: false)
        }
        for (i18nItem in item.value.i18n) {
            ant.copy(file: i18nItem.file, tofile: new File(i18nItem.relativePath, i18nDir), overwrite: false)
        }
        for (staticItem in item.value.static) {
            ant.copy(file: staticItem.file, tofile: new File(staticItem.relativePath, pluginDir), overwrite: false)
        }
    }
}

setDefaultTarget(addPluginResourcesToCustomizationArea)

substractBasePathFromFullPath = {basePath, fullPath ->
    def fullBasePath = new File(basePath).canonicalPath + File.separator
    def fullFullPath = new File(fullPath).canonicalPath
    return FilenameUtils.normalize(fullFullPath - fullBasePath)
}

/**
 * Gets all i18n and gsp files for all plugins (inline and external).
 * Result is a map contains plugin name as a key and a map of properties as a value.
 * Properties map has 2 property - i18n and gsp, each contains a list of files (instances of File)
 *
 * @return map
 */
private Map getPluginsFiles() {
    return getInlinePluginsFiles() + getExternalPluginsFiles()
}

/**
 * Gets all i18n and gsp files for external plugins (included via BuildConfig).
 * Result is a map contains plugin name as a key and a map of properties as a value.
 * Properties map has 2 property - i18n and gsp, each contains a list of files (instances of File)
 *
 * @return map
 */
private Map getExternalPluginsFiles() {
    Map allPluginNamesWithRevision = [:]
    for (p in readAllPluginXmlMetadata()) {
        String name = p.@name.text()
        String version = p.@version.text()
        allPluginNamesWithRevision.put("${name}-${version}" as String, name)
    }

    Map externalPluginsFolders = [:]
    new File(grailsSettings.projectPluginsDir as String).listFiles(new FileFilter() {
        @Override
        boolean accept(File file) {
            return file.isDirectory() && allPluginNamesWithRevision.containsKey(file.name)
        }
    }).each {
        externalPluginsFolders.put(allPluginNamesWithRevision.get(it.name), it.canonicalPath)
    }

    Map res = [:]
    externalPluginsFolders.each {name, String file ->
        Map files = getGrailsResourcesFiles(file)
        if (files.i18n || files.gsp || files.static) {
            res.put(name, files)
        }
    }
    return res
}

/**
 * Gets all i18n and gsp files for inline plugins.
 * Result is a map contains plugin name as a key and a map of properties as a value.
 * Properties map has 2 property - i18n and gsp, each contains a list of files (instances of File)
 *
 * @return map
 */
private Map getInlinePluginsFiles() {
    Map res = [:]
    grailsSettings.config.grails?.plugin?.location?.each {String name, def location ->
        if (location instanceof String) {
            Map files = getGrailsResourcesFiles(location as String)
            if (files.i18n || files.gsp || files.static) {
                res.put(name, files)
            }
        }
    }
    return res
}

/**
 * Returns all grails i18n and template files from specified grails project's root
 */
getGrailsResourcesFiles = {String root ->
    ['i18n': getGrailsI18nFiles(root), 'gsp': getGrailsGspFiles(root), 'static': getStaticFiles(root)]
}

/**
 * Returns all grails i18n files from specified grails project's root
 */
getGrailsI18nFiles = {String root ->
    File base = new File(root, "grails-app/i18n")
    Pattern pattern = ~/.*properties$/
    return getFilesFiltered(base, pattern).collect {
        [file: it, relativePath: substractBasePathFromFullPath(base.canonicalPath, it.canonicalPath)]
    }
}

/**
 * Returns all grails template files from specified grails project's root
 */
getGrailsGspFiles = {String root ->
    File base = new File(root, "grails-app/views")
    Pattern pattern = ~/.*gsp$/
    return getFilesFiltered(base, pattern).collect {
        [file: it, relativePath: substractBasePathFromFullPath(base.canonicalPath, it.canonicalPath)]
    }
}

/**
 * Get all static resource files except WEB-INF & META-INF
 */
getStaticFiles = {String root ->
    File base = new File(root, "web-app")
    List<Path> excludes = []
    excludes << base.toPath().resolve("WEB-INF")
    excludes << base.toPath().resolve("META-INF")

    List res = []
    if (base.exists()) {
        base.eachFileRecurse {File file->
            if (!file.isDirectory() && !excludes.any {file.toPath().startsWith(it)}) {
                res << [file: file, relativePath: substractBasePathFromFullPath(base.canonicalPath, file.canonicalPath)]
            }
        }
    }

    return res
}

/**
 * Returns a list of files (recursively) filtered by extension
 *
 * @param base root path
 * @param pattern pattern to filter file names
 * @return list of files.
 */
private List getFilesFiltered(File base, Pattern pattern) {
    List res = []
    if (base.exists()) {
        base.eachFileRecurse {
            if (!it.isDirectory() && it.name.matches(pattern)) {
                res << it
            }
        }
    }
    return res
}

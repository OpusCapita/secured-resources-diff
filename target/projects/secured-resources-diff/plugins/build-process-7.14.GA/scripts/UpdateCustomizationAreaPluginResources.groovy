import java.nio.file.Path
import java.util.regex.Pattern
import org.apache.commons.io.FilenameUtils

includeTargets << grailsScript("Compile")

/**
 * WARNING!: be careful, mercurial required extension [purge]
 *
 * Usage: grails updateCustomizationAreaResources <path-to-customization-area>
 * The path may be both absolute or relative.
 * Current dir may be root grails application directory.
 *
 * This script looks at customization area and merge all files with '-plugins' branch (or create new branch if it does not exists)
 * and store all information about existing files (in customization area) at the same time. After that the script find all plugins,
 * used in application and gather all *{{i18n}}* and *{{gsp}}* files (that have appropriate extension and located in the standard
 * grails folders). After that Script does the same in customization area, using path *{{webapp/plugins/<plugin name>/(i18n|views)}}*.
 * After that it it compares files and build list of added, modified and deleted files in plugins relative to customization area.
 * After that the plugin make update customization area to branch named as *{{<current branch name>-plugins}}*.
 * Pay your attention that customization area should be stay on the branch you want to merge and should be updated to the revision
 * you want to process. All changes found by this script will be committed into plugins branch, exclude all changes in files already
 * present in customization area) and after that customization area will be updated to original branch. If any error occurs the script
 * thrown exception in console.
 *
 * @author Eugeny Pozharsky
 */

def branchSuffix = "-plugins"
def newBranchMessage = "Created new branch for plugins resources."
def newCommitMessage = "Added new plugin resources."
def basePathForFilesInsideCA = "webapp/plugins"
File caRoot

target(main: "Collects customizable resources from application and plugins and copy it into customization area") {
    depends(compile, resolveDependencies)

    if (args.size() == 0) {
        println "Abort: Please, specify path to customization area."
        return -1
    }

    print "Checking Mercurial present..."
    try {
        isMercurialPresent()
    } catch (Exception ex) {
        throw new RuntimeException("Mercurial must be present in the system.", ex)
    }
    println "ok"

    print "Checking installed [purge] extenstion for mercurial present..."
    try {
       executeCommand("hg purge --print")
    } catch(e) {
       ant.fail(message: "Need to install extension [purge] for mercurial. Please try to install this extenstion and run the script again.")
    }
    println "ok"

    caRoot = new File(".")
    executeCommand("hg rev -C application.properties")

    caRoot = new File(args as String)

    print "Checking customization area root folder exists..."
    if (!caRoot.exists()) {
        throw new RuntimeException("Path to customization area invalid.")
    }
    println "ok"

    def tag = getCurrentTag()
    if (!tag) {
        ant.input(addproperty: "tagName", message: "Enter plugin's branch tag name: ")
        tag = ant.antProject.properties."tagName"
    } else {
        tag = tag + branchSuffix
    }
    if (!tag) {
        println "Tag name must be defined."

    }
    if (!validateTagName(tag)) {
        println "Tag name validation error for '${tag}'."
        return
    }
    println "Use tag name for customization area plugins branch: ${tag}"

    def process

    print "Gathering information about files in customization area..."
    def caFiles = collectCAFileDatas(caRoot)
    println "ok"

    print "Getting original and current branch names..."
    String originalBranchName = executeCommand("hg branch").trim()
    String originalBranchRev = executeCommand("hg id").trim().split(" ").first()
    List originalBranches = executeCommand("hg branches").trim().split("\n").collect {it.indexOf(" ") != -1 ? it.substring(0, it.indexOf(" ")) : ""}
    String branchName = originalBranchName + branchSuffix
    println "ok"

    if (!originalBranches.contains(branchName)) {
        // need to create plugins branch
        print "No plugin branch found. Creating a new one..."
        executeCommand("hg branch ${branchName}")
        try {
            executeCommand("hg rem ..${File.separator}.hgsubstate -f")
            executeCommand("hg rem ..${File.separator}.hgsub -f")
        } catch (Exception ex) {
            // hide exception if no files found
        }
        executeCommand(["hg", "ci", "-m '${newBranchMessage}'"])
        println "ok"
    } else {
        print "Merging current branch with plugin branch..."
        executeCommand("hg up ${branchName}")
        executeCommand("hg purge --all")
        try {
            executeCommand("hg merge -r ${originalBranchRev}")
        } catch (Exception ex) {
            // avoid script break if no changes to be merged
        }
        if (executeCommand("hg st")) {
            try {
                executeCommand("hg rem ..${File.separator}.hgsubstate -f")
                executeCommand("hg rem ..${File.separator}.hgsub -f")
            } catch (Exception ex) {
                // hide exception if no files found
            }
            println "\nPress any key to commit merge of ${originalBranchName} branch to plugins branch..."
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            executeCommand(["hg", "ci", "-m 'Merged ${originalBranchName} branch on revision ${originalBranchRev} to plugin branch.'"])
        }
        println "ok"
    }

    println "Updating customization area..."
    updateCA(basePathForFilesInsideCA, caRoot, caFiles)
    println "ok"

    // check that any changes present
    if (executeCommand("hg st")) {
        print "Committing result into plugin branch..."
        executeCommand(["hg", "ci", "-m '${newCommitMessage}'"])
        println "ok"
    } else {
        System.out.println "No changes found for branch [${branchName}]."
    }

    executeCommand("hg tag ${tag}")

    print "Updating to original branch..."
    executeCommand("hg up ${originalBranchRev} -C")
    executeCommand("hg purge --all")
    println "ok"

    ant.input(addproperty: "result", message: "Do you want to push changes? (y,N): ")
    result = ant.antProject.properties."result"
    if (result == 'y' || result == 'Y') {
        executeCommand("hg push --new-branch")
    } else {
        println("Do not forget to push changes.")
    }
    println("Bye!")
}

getCurrentTag = {->
    def result = null
    def rev = executeCommand("hg id -i").trim()
    def tags = executeCommand("hg tags").split("\n") as List
    if (tags.size() > 0) {
        if (tags[0].contains("tip")) {
            tags.remove(0) // remove tip tag
        }
        def tagString = tags.find {it.contains(rev)}
        if (tagString) {
            result = tagString.split(" ").first()
        }
    }
    return result
}

validateTagName = {String tag ->
    def tags = executeCommand("hg tags").split("\n") as List
    if (tags.size() > 0) {
        if (tags.find {it.contains(tag)}) {
            println "Tag '${tag}' already exists."
            return false
        }
    }
    return true
}

collectCAFileDatas = { File root ->
    def files = [:]
    root.eachFileRecurse {
        if (!it.isDirectory()) {
            def path = it.canonicalPath - root.canonicalPath
            if (path =~ "^${File.separator}webapp${File.separator}plugins${File.separator}.*".replace("\\", "\\\\")) {
                def pluginName = path - "${File.separator}webapp${File.separator}plugins${File.separator}"
                pluginName = pluginName.substring(0, pluginName.indexOf(File.separator))
                def relativePath = path - "${File.separator}webapp${File.separator}plugins${File.separator}${pluginName}${File.separator}"
                if (files.containsKey(pluginName)) {
                    files[pluginName] << relativePath
                } else {
                    files[pluginName] = [relativePath]
                }
            }
        }
    }
    return files
}

setDefaultTarget(main)

/**
 * Execute external command. Return stdout from external process. If any error occurs, throws exception.
 * @param command a string or a list of strings to execute
 * @return string
 */
executeCommand = {command ->
    Process process = command.execute(null, caRoot)
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
    StringBuilder builder = new StringBuilder()
    char[] charBuffer = new char[8192];
    int nbCharRead = 0
    while (!isProcessExit(process)) {
        nbCharRead = reader.read(charBuffer)
        if (nbCharRead != -1) {
            builder.append(charBuffer, 0, nbCharRead)
        }
    }
    builder.append(process.text)
    if (process.exitValue()) {
        throw new RuntimeException(process.err.text)
    }
    return builder.toString()
}

/**
 * Checks that given process already exit
 *
 * @param process
 * @return boolean
 */
private boolean isProcessExit(Process process) {
    boolean result = true
    try {
        process.exitValue()
    } catch (IllegalThreadStateException ex) {
        result = false
    }
    return result
}

/**
 * Update customization area. Copy existing files, add new files and remove removed files.
 *
 * @param caRoot
 * @param basePathForFilesInsideCA - (webapp/plugins)
 * @param caFiles
 * @return
 */
private updateCA(String basePathForFilesInsideCA, File caRoot, def caFiles) {
    Map files = getPluginsFiles()
    File basePath = new File(caRoot, basePathForFilesInsideCA)
    files.each {pluginName, pluginFiles ->
        pluginFiles.i18n.each {file ->
            def caFile = "i18n${File.separator}${caFiles[pluginName]}"
            if (!caFiles[pluginName] || !caFile.contains("i18n${File.separator}${file.relativePath}")) {
                File destFile = new File(basePath, "${pluginName}${File.separator}i18n${File.separator}${file.relativePath}")
                copy(file.file, destFile)
            } else {
                println "Skip file i18n${File.separator}${file.relativePath} for plugin ${pluginName} because it is customized in CA"
            }
        }
        pluginFiles.gsp.each {file ->
            def caFile = "views${File.separator}${caFiles[pluginName]}"
            if (!caFiles[pluginName] || !caFile.contains("views${File.separator}${file.relativePath}")) {
                File destFile = new File(basePath, "${pluginName}${File.separator}views${File.separator}${file.relativePath}")
                copy(file.file, destFile)
            } else {
                println "Skip file views${File.separator}${file.relativePath} for plugin ${pluginName} because it is customized in CA"
            }
        }
        pluginFiles.static.each {file ->
            def caFile = "web-app${File.separator}${caFiles[pluginName]}"
            if (!caFiles[pluginName] || !caFile.contains("${file.relativePath}")) {
                File destFile = new File(basePath, "${pluginName}${File.separator}${file.relativePath}")
                copy(file.file, destFile)
            } else {
                println "Skip file ${file.relativePath} for plugin ${pluginName} because it is customized in CA"
            }
        }
    }
    executeCommand(["hg", "add", basePathForFilesInsideCA])
    removeOldFiles()
}

/**
 * Compare old and new files, find files that was removed in new version of CA and remove in with Mercurial
 */
removeOldFiles = {
    List existingFiles = getExistingFileList()
    List newlyFiles = getNewFileList(getPluginsFiles())
    List removedFiles = getRemovedFiles(newlyFiles, existingFiles)
    if (removedFiles) {
        List command = ["hg", "rem"]
        removedFiles.each {
            command << substractBasePathFromFullPath(caRoot.canonicalPath, it)
        }
        executeCommand(command)
    }
}

substractBasePathFromFullPath = {basePath, fullPath ->
    def fullBasePath = new File(basePath).canonicalPath + File.separator
    def fullFullPath = new File(fullPath).canonicalPath
    return FilenameUtils.normalize(fullFullPath - fullBasePath)
}

getRemovedFiles = {newlyFiles, existingFiles ->
    List res = []
    existingFiles.each {existingFile ->
        if (!newlyFiles.contains(existingFile)) {
            res << existingFile
        }
    }
    return res
}

getNewFileList = { Map pluginFiles ->
    List res = []
    pluginFiles.each {pluginName, map ->
        File i18nRoot = new File(caRoot, "webapp/plugins/${pluginName}/i18n")
        File gspRoot = new File(caRoot, "webapp/plugins/${pluginName}/views")
        File webappRoot = new File(caRoot, "webapp/plugins/${pluginName}")
        map.i18n.each {
            res << new File(i18nRoot, it.relativePath).canonicalPath
        }
        map.gsp.each {
            res << new File(gspRoot, it.relativePath).canonicalPath
        }
        map.static.each {
            res << new File(webappRoot, it.relativePath).canonicalPath
        }
    }
    return res
}

getExistingFileList = {
    List res = []
    File base = new File(caRoot, basePathForFilesInsideCA)
    if (base.exists()) {
        base.eachFileRecurse {
            if (!it.isDirectory()) {
                res << it.canonicalPath
            }
        }
    }
    return res
}

/**
 * Copy _src_ file into _dest_ file. Create _dest_ file with path if needed
 *
 * @param src source file
 * @param dest dest file
 */
private copy(File src, File dest) {
    new File(dest.parent).mkdirs()
    def input = src.newDataInputStream()
    def output = dest.newDataOutputStream()

    output << input

    input.close()
    output.close()
}

/**
 * Thrown RuntimeException when no mercurial found
 * @return
 */
private isMercurialPresent() {
    executeCommand("hg help")
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

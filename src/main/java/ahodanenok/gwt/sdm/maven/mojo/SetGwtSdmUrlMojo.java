package ahodanenok.gwt.sdm.maven.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;

@Mojo(name = "setGwtSdmUrl", defaultPhase = PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class SetGwtSdmUrlMojo extends AbstractMojo {

    // todo: if module is not specified, then try to search for all modules in the project sources

    private static final Pattern HOST_NAME_PATTERN = Pattern.compile("var hostName\\s+=\\s+['\"](.+)['\"]");
    private static final Pattern SERVER_URL_PATTERN = Pattern.compile("var serverUrl\\s+=\\s+['\"](.+)['\"]");

    @Parameter(property = "setGwtSdmUrl.module", required = true)
    private String module;
    @Parameter(property = "setGwtSdmUrl.host", defaultValue = "localhost")
    private String host;
    @Parameter(property = "setGwtSdmUrl.port", defaultValue = "9876")
    private String port;
    @Parameter(property = "setGwtSdmUrl.webappDirectory", defaultValue = "${project.build.directory}/${project.build.finalName}")
    private String webappDirectory;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        writeStubJs(module, setSdmUrl(module, host, port));
    }

    private String setSdmUrl(String moduleName, String host, String port) throws MojoExecutionException {
        String stubJs = readStubJs()
                .replace("__MODULE_NAME__", moduleName)
                .replace("$wnd.location.hostname", String.format("'%s'", host))
                .replace("__SUPERDEV_PORT__", port);

        getLog().info("GWT SDM URL:");
        Matcher hostNameMatcher = HOST_NAME_PATTERN.matcher(stubJs);
        if (!hostNameMatcher.find()) {
            throw new MojoExecutionException("Unexpected 'stub.nocache.js' format: couldn't find 'hostName' variable");
        }
        getLog().info(String.format("  hostName='%s'", hostNameMatcher.group(1)));

        Matcher serverUrlMatcher = SERVER_URL_PATTERN.matcher(stubJs);
        if (!serverUrlMatcher.find()) {
            throw new MojoExecutionException("Unexpected 'stub.nocache.js' format: couldn't find 'serverUrl' variable");
        }
        getLog().info(String.format("  serverUrl='%s'", serverUrlMatcher.group(1)));

        return stubJs;
    }

    private String readStubJs() throws MojoExecutionException {
        getLog().debug("Resolving 'gwt-dev' artifact...");
        Artifact gwtArtifact = project.getArtifactMap().get(ArtifactUtils.versionlessKey("com.google.gwt", "gwt-dev"));
        URL gwtUrl;
        try {
            gwtUrl = gwtArtifact.getFile().toURI().toURL();
            getLog().debug("'gwt-dev' has been resolved: " + gwtUrl);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Can't convert GWT artifact path to URL", e);
        }

        Class codeServerClass;
        try {
            codeServerClass = new URLClassLoader(new URL[] { gwtUrl }).loadClass("com.google.gwt.dev.codeserver.CodeServer");
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Can't load 'stub.nocache.js': 'gwt-dev' is not found in the project classpath");
        }

        try {
            return IOUtil.toString(codeServerClass.getResource("stub.nocache.js").openStream());
        } catch (IOException e) {
            throw new MojoExecutionException("Can't load 'stub.nocache.js'", e);
        }
    }

    private void writeStubJs(String moduleName, String stub) throws MojoExecutionException {
        String filePath = new File(new File(webappDirectory, moduleName), moduleName + ".nocache.js").getAbsolutePath();
        getLog().debug(String.format("Writing modified 'stub.nocache.js' to '%s'", filePath));

        try {
            FileUtils.fileWrite(filePath, "UTF-8", stub);
        } catch (IOException e) {
            throw new MojoExecutionException("Can't write to file: " + filePath, e);
        }
    }
}

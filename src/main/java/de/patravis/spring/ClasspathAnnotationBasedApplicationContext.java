package de.patravis.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;
import org.xeustechnologies.jcl.context.DefaultContextLoader;
import org.xeustechnologies.jcl.context.JclContext;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.*;

public class ClasspathAnnotationBasedApplicationContext extends AnnotationConfigApplicationContext {

    private boolean scanDefaultTypes;
    private ClassPathScanningCandidateComponentProvider scanningCandidateComponentProvider;

    public ClasspathAnnotationBasedApplicationContext() {
        this(true);
    }

    public ClasspathAnnotationBasedApplicationContext(boolean scanDefaultTypes) {
        super();
        this.scanDefaultTypes = scanDefaultTypes;

        //Init JCL
        initJCL();
        initSpringScannerFilter();
    }

    /**
     * Spring Extensions
     */
    private void initSpringScannerFilter() {
        ClassUtils.overrideThreadContextClassLoader(JclContext.get());
        scanningCandidateComponentProvider = new ClassPathScanningCandidateComponentProvider(scanDefaultTypes);
        PathMatchingResourcePatternResolver resourceLoader = new PathMatchingResourcePatternResolver(JclContext.get());

        scanningCandidateComponentProvider.setResourceLoader(resourceLoader);
    }

    public void addAssignableScan(AssignableTypeFilter filter) {
        scanningCandidateComponentProvider.addIncludeFilter(filter);
    }

    public void addAnnotationScan(AnnotationTypeFilter filter) {
        scanningCandidateComponentProvider.addIncludeFilter(filter);
    }

    /**
     * JCL Extensions
     */
    private void initJCL() {
        JarClassLoader jcl = new JarClassLoader();
        DefaultContextLoader context = new DefaultContextLoader(jcl);
        context.loadContext();

        setClassLoader(jcl);
    }

    public void addToClasspath(String path) {
        JclContext.get().add(path);
    }

    public void addToClasspath(URL url) {
        JclContext.get().add(url);
    }

    public void addToClasspath(Object obj) {
        JclContext.get().add(obj);
    }

    public void addToClasspath(InputStream input) {
        JclContext.get().add(input);
    }

    @Override
    public void scan(String... basePackages) {

        JclObjectFactory factory = JclObjectFactory.getInstance();
        Map<String, byte[]> loadedResources = JclContext.get().getLoadedResources();
        Object[] configurations = new Class[0];
        for (String key : loadedResources.keySet()) {
            if (key.contains("plugin.xml")) {
                byte[] fileContent = loadedResources.get(key);
                try {
                    configurations = retrieveConfigurationClasses(fileContent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        for (Object configuration : configurations) {
            register((Class) configuration);
        }

        //Scan default packages
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanningCandidateComponentProvider.findCandidateComponents(basePackage);
            for (BeanDefinition beanDefinition : candidateComponents) {
                Object o = factory.create(JclContext.get(), beanDefinition.getBeanClassName());
                register(o.getClass());
            }
        }

    }

    private Object[] retrieveConfigurationClasses(byte[] fileContent) throws Exception {

        List<String> classes = new LinkedList<String>();

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder =  builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(new ByteArrayInputStream(fileContent));

        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/plugin/configurations/configuration";
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

        if(nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node item = nodeList.item(i);
                String textContent = item.getFirstChild().getTextContent();
                classes.add(textContent);
            }
        }

        return retrieveClasses(classes);
    }

    private Object[] retrieveClasses(List<String> classes) {

        JclObjectFactory factory = JclObjectFactory.getInstance();

        List<Class> classList = new ArrayList<Class>();
        for (String aClass : classes) {
            Object o = factory.create(JclContext.get(), aClass);
            if (o != null) {
                classList.add(o.getClass());
            }
        }
        return classList.toArray();
    }
}

package de.patravis.spring;

import de.patravis.spring.config.ApplicationConfig;
import de.patravis.spring.config.SampleClass;
import org.junit.Assert;
import org.junit.Test;

public class SpringClasspathTest {

    @Test
    public void runTest() {
        ClasspathAnnotationBasedApplicationContext ctx = new ClasspathAnnotationBasedApplicationContext(false);
        ctx.addToClasspath("C:\\Libs\\");
        ctx.register(ApplicationConfig.class);
        ctx.scan("de.patravis.spring");
        ctx.refresh();

        SampleClass bean = ctx.getBean(SampleClass.class);
        Assert.assertNotNull(bean);
    }

}

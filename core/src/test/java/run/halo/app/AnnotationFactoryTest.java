package run.halo.app;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.metrics.StartupStep;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

/**
 * @author guqing
 * @since 2.0.0
 */
public class AnnotationFactoryTest {

    @Test
    public void test() {
        // core application context
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(D.class);
        applicationContext.refresh();

        System.out.println(applicationContext.getBean(D.class).name());

        StopWatch stopWatch = new StopWatch();

        stopWatch.start("创建factory");
        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.setParent(applicationContext);
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)ctx.getBeanFactory();

        AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor =
            new AutowiredAnnotationBeanPostProcessor();
        autowiredAnnotationBeanPostProcessor.setBeanFactory(beanFactory);

        beanFactory.addBeanPostProcessor(autowiredAnnotationBeanPostProcessor);

        List<Class<?>> classes = List.of(A.class, B.class, C.class, E.class);
        for (Class<?> extensionClass : classes) {
//            BeanDefinitionBuilder beanDefinitionBuilder =
//                BeanDefinitionBuilder.genericBeanDefinition(extensionClass);
//            GenericBeanDefinition beanDefinition =
//                (GenericBeanDefinition) beanDefinitionBuilder.getRawBeanDefinition();
//            beanDefinition.setScope(SCOPE_SINGLETON);
//            beanDefinition.setLazyInit(true);
//            beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
//            beanFactory.registerBeanDefinition(extensionClass.getName(), beanDefinition);
            ctx.registerBean(extensionClass.getName(), extensionClass);
        }
        ctx.refresh();

        stopWatch.stop();
        System.out.println(stopWatch.prettyPrint());
        System.out.println(stopWatch.getTotalTimeSeconds());
        System.out.println(ctx.getBean(A.class).test());
        ctx.getBean(C.class).test();
        System.out.println(ctx.getBean(D.class).name());
        ctx.getBean(E.class).test();
        ctx.close();
    }



    @Component
    static class A {
        private final B b;

        public A(B b) {
            this.b = b;
        }

        public String test() {
            System.out.println("b:" + b);
            return b.name();
        }
    }

    @Component
    static class B {
        public String name() {
            return "zhangsan";
        }
    }

    @Component
    static class C {
        @Autowired
        private B b;

        public void test() {
            System.out.println("c: " + b.name());
        }
    }

    @Component
    static class D {
        public String name() {
            return "D";
        }
    }

    @Component
    static class E {
        @Autowired
        D d;

        public void test() {
            System.out.println("E:" + d.name());
        }
    }
}

class SimpleBeanRegistry implements AnnotationConfigRegistry, BeanDefinitionRegistry {
    private final DefaultListableBeanFactory beanFactory;
    private final AnnotatedBeanDefinitionReader reader;
    private final ClassPathBeanDefinitionScanner scanner;

    public SimpleBeanRegistry(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.reader = new AnnotatedBeanDefinitionReader(this);
        this.scanner = new ClassPathBeanDefinitionScanner(this);
    }
    @Override
    public void register(Class<?>... componentClasses) {
        Assert.notEmpty(componentClasses, "At least one component class must be specified");
        this.reader.register(componentClasses);
    }

    @Override
    public void scan(String... basePackages) {
        Assert.notEmpty(basePackages, "At least one base package must be specified");
        this.scanner.scan(basePackages);
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
        throws BeanDefinitionStoreException {

        this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
    }

    @Override
    public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        this.beanFactory.removeBeanDefinition(beanName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        return this.beanFactory.getBeanDefinition(beanName);
    }

    public DefaultListableBeanFactory getBeanFactory() {
        return beanFactory;
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return getBeanFactory().containsBeanDefinition(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return getBeanFactory().getBeanDefinitionCount();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return getBeanFactory().getBeanDefinitionNames();
    }


    @Override
    public boolean isBeanNameInUse(String beanName) {
        return this.beanFactory.isBeanNameInUse(beanName);
    }

    @Override
    public void registerAlias(String beanName, String alias) {
        this.beanFactory.registerAlias(beanName, alias);
    }

    @Override
    public void removeAlias(String alias) {
        this.beanFactory.removeAlias(alias);
    }

    @Override
    public boolean isAlias(String beanName) {
        return this.beanFactory.isAlias(beanName);
    }

    @Override
    public String[] getAliases(String name) {
        return getBeanFactory().getAliases(name);
    }

}

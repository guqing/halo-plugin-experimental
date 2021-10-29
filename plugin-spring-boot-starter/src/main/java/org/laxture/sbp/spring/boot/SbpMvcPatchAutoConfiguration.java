package org.laxture.sbp.spring.boot;

import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import xyz.guqing.plugin.core.SpringBootPluginManager;
import xyz.guqing.plugin.core.boot.SbpPluginStateChangedEvent;
import xyz.guqing.plugin.core.internal.PluginRequestMappingManager;

/**
 * Sbp main app auto configuration for Spring Boot.
 *
 * @author guqing
 * @see SbpProperties
 */
@Configuration
@ConditionalOnClass({PluginManager.class, SpringBootPluginManager.class})
@ConditionalOnProperty(prefix = SbpProperties.PREFIX, value = "enabled", havingValue = "true")
public class SbpMvcPatchAutoConfiguration {
//	@Bean
//	@ConditionalOnMissingBean(WebMvcRegistrations.class)
//	public WebMvcRegistrations mvcRegistrations() {
//		return new WebMvcRegistrations() {
//			@Override
//			public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
//				return new PluginRequestMappingHandlerMapping();
//			}
//
//			@Override
//			public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
//				return null;
//			}
//
//			@Override
//			public ExceptionHandlerExceptionResolver getExceptionHandlerExceptionResolver() {
//				return null;
//			}
//		};
//	}

    @EventListener(SbpPluginStateChangedEvent.class)
    public void onPluginStarted() {

    }
}
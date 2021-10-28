/*
 * Copyright (C) 2019-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.guqing.plugin.core.boot;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * This event will be published to <b>plugin application context</b> once plugin is restarted.
 *
 * Note that this event will not be fired duaring <b>main app application context</b> starting phase.
 *
 * @author <a href="https://github.com/hank-cp">Hank CP</a>
 */
public class SbpPluginRestartedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1651490578605729784L;

    public SbpPluginRestartedEvent(ApplicationContext pluginApplicationContext) {
        super(pluginApplicationContext);
    }
}

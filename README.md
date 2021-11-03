# Halo plugin experimental
[Halo](https://github.com/halo-dev/halo) 博客插件化的功能性探索。

## 简介
插件管理能力位于 [extensions](halo/src/main/java/run/halo/app/extensions) 目录下
[plugins](./plugins) 目录下为插件示例

在插件和 Halo 主应用共用 ApplicationContext 还是 插件使用独立的 ApplicationContext的问题上：

经过实验最终还是选择插件和 Halo 使用同一个 ApplicationContext，原因如下：
1. 插件作为独立应用使用时（即有自己独立的ApplicationContext）加载一个空插件在我的机器上需要 `10s`以上，
而这段时间基本都是花费在插件加载时创建插件的ApplicationContext上，如果插件功能类文件过多则会导致时间更久，
在配置低的服务器上则无法想像。
2. 如果以插件使用独立ApplicationContext为依据开发插件功能那么当一个Halo应用存
在 10 个插件时，加载插件所耗费的时间以及插件的ApplicationContext造成的无用资源浪费也是不可容忍的。

## 实现

- 自定义`ScanningExtensionFinder`插件类扫描器以提供对`spring`注解的支持
- 插件的所有类由自定义的插件类加载器`SpringPluginClassLoader`加载防止名称空间伪造
- 启用插件后将带有`spring`注解的插件类加载到 `Halo` 的 `Spring` 容器中。该功能由`SingletonSpringExtensionFactory`完成
- 加载时带有`ExtController`和`ExtRestController`注解的类由`PluginRequestMappingManager`注册到`MVC`

## 目标

- [x] 插件可以注入使用`Halo`提供的`Bean`

- [x] 插件可以像写普通的`spring boot`应用一样

- [x] 插件在开发时可以引入`Halo`中不存在的外部依赖
- [ ] 插件的静态资源可以被主应用加载
- [ ] 插件发生异常不会影响主应用及其他插件的使用

- [x] 插件类使用独立的类加载器
- [ ] 插件和主应用共用`ApplicationContext`却又能做到`Bean`之间的隔离


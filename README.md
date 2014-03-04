新浪微博Android REST Client
=====================
## 说明
简洁，流畅的微博app，并且fantasy每天都会带你发现地球上的精彩瞬间:-)

程序架构启发自Google IO 2010 - [developing-RESTful-android-apps][]，界面设计启发自[Twitter for Android][]（需翻墙）

个人学习时作品，目前有空或者有新的想法就写写，持续构建中:-)，欢迎感兴趣的朋友交流，fork，clone，bug issue啥的!

> 项目主页请见[这里][project-host]

> ![about][]

## 环境需求
1. Android SDK >= 4.1.x（API Level >= 16），*建议设置ANDROID_HOME环境变量*
2. JDK >= 1.6（现在只有4.4才支持JDK7的语法），**必须设置好JAVA_HOME环境变量**
3. IDE（选择一个自己熟悉的就可以了）
 1. Intellij IDEA（推荐使用免费开源的社区版[Intellij IDEA Community][]）
 2. Eclipse（包含ADT插件，建议直接下载打包好的[ADT Bundle][]）
 3. Android Studio（好是挺好，就是龟速，现阶段不太推荐）
4. 以下为**可选**
 1. Gradle == 1.8 （本项目自带了Gradle Wrapper，所以本地没有也没有关系，不用Android Studio的话更不需要这个）

## 依赖（在``build.gradle``里``dependencies {}``申明）
1. support-v4，注意版本号，为什么我们的项目要求那么高的API却还需要兼容库呢？因为兼容库不仅仅提供兼容类，还有其它的功能。并且没有用到的类可以在编译期间被删除掉[?][ProGuard]
2. support-v13，for native fragment api
3. android-volley，异步http请求框架，需要自行[下载][volley]或者通过Android Studio引引入
4. ActionBar-PullToRefresh，刷新控件，需自行[下载][pull2refresh]或者通过Android Studio引入
5. Google Analytics，匿名统计使用信息，需自行[下载][Google Analytics]并引入
6. Picasso，图片缓存框架，需自行[下载][Picasso]或者通过Android Studio引入
7. ViewPagerIndicator, 页面切换指示器，需自行[下载][ViewPagerIndicator]或者通过Android Studio引入

## 如何构建
1. 通过IDE
 1. Intellij IDEA，直接导入（需自行引入``build.gradle``里``dependencies {}``的依赖），后面你懂的
 2. Eclipse，直接导入（需自行引入``build.gradle``里``dependencies {}``的依赖），后面你懂的
 3. Android Studio，直接导入（最好选中build.gradle文件），后面你懂的
2. 通过命令行（gradle构建，**目前版本必须是1.8**，并且要**设置好ANDROID_HOME环境变量**）
 1. 如果本地有配置有gradle，那么shell或者cmd进入到项目根目录，敲``gradle clean build``，一切正常的话在``build/apk/``下会生成相应的apk文件（**注意安装文件名有debug的那个apk文件**）
 2. 如果本地没有安装gradle，那么shell或者cmd进入项目根目录，mac或者linux敲``./gradlew clean build``，windows敲``gradlew clean build``，接下来同1

## License
code license
> ```
> The MIT License (MIT)
> Copyright (c) 2014 longkai
> The software shall be used for good, not evil.
> ```

document license
> 本作品采用[知识共享署名-非商业性使用 4.0 国际许可协议][creative commons license]进行许可。

>![][creative commons icon]

## 联系作者
1. 邮箱：im.longkai@gmail.com
2. 微信：longkai_1991
3. Twitter: [@longkai_1991][]
4. 新浪微博：[@龙凯Orz][]

---
last update: 2014-03-04

[developing-RESTful-android-apps]: http://www.google.com/events/io/2010/sessions/developing-RESTful-android-apps.html "developing-RESTful-android-apps"
[Twitter for Android]: https://about.twitter.com/zh-hans/products/android "twitter for android"
[Intellij IDEA Community]: http://www.jetbrains.com/idea/ "Intellij IDEA"
[ADT Bundle]: http://developer.android.com/sdk/index.html "ADT Bundle"
[ProGuard]: http://proguard.sourceforge.net/index.html "ProGuard"
[volley]: https://android.googlesource.com/platform/frameworks/volley "android-volley"
[pull2refresh]: https://github.com/chrisbanes/ActionBar-PullToRefresh "ActionBar-PullToRefresh"
[Google Analytics]: https://developers.google.com/analytics/devguides/collection/android/v3/ "Google Analytics v3"
[Picasso]: http://square.github.io/picasso/ "Picasso"
[ViewPagerIndicator]: http://viewpagerindicator.com/ "ViewPagerIndicator"
[@龙凯Orz]: http://weibo.com/coding4fun "sina weibo"
[@longkai_1991]: https://twitter.com/longkai_1991 "twitter"
[creative commons icon]: http://i.creativecommons.org/l/by-nc/4.0/88x31.png "creative commons icon"
[creative commons license]: http://creativecommons.org/licenses/by-nc/4.0/deed.zh "creative commons license"

[about]: https://farm8.staticflickr.com/7417/12923931494_b40944bdcd_o.png "about"
[project-host]: http://longkai.github.io/catnut/ "project-host"

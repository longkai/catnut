 新浪微博Android REST Client
=====================
![logo](https://farm3.staticflickr.com/2915/14145326087_9fb76b1ed5_m.jpg)

[<img width=240 alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png' />][Google Play]

---
## 说明
简洁，流畅，快速的微博app，并且附带500px和知乎每日精选的照片和问答插件 :-)

程序架构启发自Google IO 2010 - [developing-RESTful-android-apps][]，界面设计启发自[Twitter for Android][]（需翻墙）

个人学习时作品，目前有空或者有新的想法就写写，持续构建中:-)，欢迎感兴趣的朋友交流，fork，clone，bug issue啥的!

项目主页请见[这里][project-host], 豌豆荚地址[下载][download]，目前需要Android 4.1+

> ![about][]

> ![timeline][]

## 环境需求(可直接导入到Android Studio)
1. Android SDK >= 4.1.x（API Level >= 16），*建议设置ANDROID_HOME环境变量*
2. JDK >= 1.6（现在只有4.4才支持JDK7的语法），**必须设置好JAVA_HOME环境变量**
3. IDE（选择一个自己熟悉的就可以了）
 1. Android Studio（推荐）
 2. Eclipse（包含ADT插件，建议直接下载打包好的[ADT Bundle][]）

## 依赖（在``build.gradle``里``dependencies {}``申明，使用Eclipse请自行导入，推荐直接导入到Android Studio中，啥事都没有直接run）
1. support-v4，注意版本号，为什么我们的项目要求那么高的API却还需要兼容库呢？因为兼容库不仅仅提供兼容类，还有其它的功能。并且没有用到的类可以在编译期间被删除掉[?][ProGuard]
2. support-v13，for native fragment api
3. android-volley，异步http请求框架，需要自行[下载][volley]或者通过Android Studio引入
4. Google Analytics，匿名统计使用信息，需自行[下载][Google Analytics]并引入
5. Picasso，(注意版本暂为2.2.0)图片缓存框架，需自行[下载][Picasso]或者通过Android Studio引入
6. ViewPagerIndicator, 页面切换指示器，需自行[下载][ViewPagerIndicator]或者通过Android Studio引入
7. AndroidStaggeredGrid, grid view，需自行[下载][AndroidStaggeredGrid]**并自行将目录结构设置为Eclipse项目结构**或者通过Android Studio引入
8. OkHttp, spdy http client, 需自行[下载][OkHttp]或者通过Android Studio引入

## 如何构建(推荐直接导入Android Studio或者在terminal构建)
1. 通过IDE
 1. Eclipse，直接导入（需自行引入``build.gradle``里``dependencies {}``的依赖），后面你懂的，一定要注意所有的support lib保持版本一致！
 2. Android Studio，直接导入（最好选中build.gradle文件），后面你懂的
2. 通过命令行（gradle构建，**目前当前适用的版本**，并且要**设置好ANDROID_HOME环境变量**）
 1. 如果本地没有安装gradle，那么shell或者cmd进入项目根目录，mac或者linux敲``./gradlew clean build``，windows敲``gradlew clean build``，接下来同1

## License
### code license
The MIT License (MIT)

Copyright (c) 2014 longkai

The software shall be used for good, not evil.


### document license
本作品采用[知识共享署名-非商业性使用 4.0 国际许可协议][creative commons license]进行许可。

![][creative commons icon]

## 联系作者
1. 邮箱：im.longkai@gmail.com
2. 微信：longkai_1991
3. Twitter: [@longkai_1991][]
4. 新浪微博：[@米粉撸油条][]

### Legal attribution
* Android, Google Play and the Google Play logo are trademarks of Google Inc.

===
last updated: 2015-06-06

[developing-RESTful-android-apps]: http://www.google.com/events/io/2010/sessions/developing-RESTful-android-apps.html "developing-RESTful-android-apps"
[Twitter for Android]: https://about.twitter.com/zh-hans/products/android "twitter for android"
[Intellij IDEA Community]: http://www.jetbrains.com/idea/ "Intellij IDEA"
[ADT Bundle]: http://developer.android.com/sdk/index.html "ADT Bundle"
[ProGuard]: http://proguard.sourceforge.net/index.html "ProGuard"
[volley]: https://android.googlesource.com/platform/frameworks/volley "android-volley"
[Google Analytics]: https://developers.google.com/analytics/devguides/collection/android/v3/ "Google Analytics v3"
[Picasso]: http://square.github.io/picasso/ "Picasso"
[ViewPagerIndicator]: http://viewpagerindicator.com/ "ViewPagerIndicator"
[AndroidStaggeredGrid]: https://github.com/etsy/AndroidStaggeredGrid "AndroidStaggeredGrid"
[OkHttp]: http://square.github.io/okhttp/ "OkHttp"
[@米粉撸油条]: http://weibo.com/coding4fun "sina weibo"
[@longkai_1991]: https://twitter.com/longkai_1991 "twitter"
[creative commons icon]: http://i.creativecommons.org/l/by-nc/4.0/88x31.png "creative commons icon"
[creative commons license]: http://creativecommons.org/licenses/by-nc/4.0/deed.zh "creative commons license"

[about]: https://farm4.staticflickr.com/3925/14308701166_9bc0348a0c_o.png "about"
[timeline]: https://farm4.staticflickr.com/3865/14331112414_929a71514c_o.png "timeline"
[project-host]: http://longkai.github.io/catnut/ "project-host"
[download]: http://www.wandoujia.com/apps/org.catnut "豌豆荚下载"

[Google Play]: https://play.google.com/store/apps/details?id=org.catnut&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1 'Get it on Google Play'

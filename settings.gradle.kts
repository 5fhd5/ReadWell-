pluginManagement {
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        mavenCentral()
    }
}

rootProject.name = "readwell"


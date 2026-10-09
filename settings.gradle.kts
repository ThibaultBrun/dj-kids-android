pluginManagement {
    repositories {
        maven {
            url = uri("https://redirector.gvt1.com/edgedl/android/maven2")
            isAllowInsecureProtocol = true
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://redirector.gvt1.com/edgedl/android/maven2")
            isAllowInsecureProtocol = true
        }
        mavenCentral()
    }
}
rootProject.name = "DJ Kids"
include(":app")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven {
            setUrl("https://jitpack.io")
        }
        maven {
            setUrl("https://github.com/500px/greedo-layout-for-android/raw/master/releases/")
        }
        maven {
            setUrl("https://a8c-libs.s3.amazonaws.com/android")
        }
    }
}

rootProject.name = "Encrypted Notes"
include(":app")
 
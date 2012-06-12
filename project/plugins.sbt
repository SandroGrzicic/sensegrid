resolvers += Resolver.url("scala-sbt repo", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.hanhuy.sbt" % "android-sdk-plugin" % "0.3.0")

resolvers += "spray repo" at "http://repo.spray.cc"

addSbtPlugin("cc.spray" % "sbt-revolver" % "0.6.1")
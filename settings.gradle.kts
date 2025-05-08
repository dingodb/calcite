rootProject.name = "calcite"
include("core")
include("linq4j")
include("testkit")
include("bom")

project(":core").name = "calcite-core"
project(":linq4j").name = "calcite-linq4j"

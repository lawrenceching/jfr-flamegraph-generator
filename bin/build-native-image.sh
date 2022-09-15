# This script is used for building a native executable binary using Graalm Native-Image

native-image --no-fallback -cp ./jfr-flamegraph-generator-1.0.0.jar -H:Name=jfr-flamegraph-generator -H:Class=me.imlc.Main -H:+ReportUnsupportedElementsAtRuntime -H:IncludeResourceBundles

package me.imlc;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.StreamSupport;

public class Main {

    private static final String HELP_MSG = """
java -jar jfr-flamegraph-generator.jar --from [source jfr] --to [output html]
    --from: Path to your JFR file
    --to  : Path to flame graph that will be generated as HTML file""";

    public static void main(String[] args) throws InterruptedException, CouldNotLoadRecordingException, IOException {

        if(args.length != 4) {
            System.err.println("Invalid arguments");
            System.out.println(HELP_MSG);
        }

        var source = args[1];
        var output = args[3];

        IItemCollection collection = JfrLoaderToolkit.loadEvents(new File(source));

        collection = collection.apply(JdkFilters.EXECUTION_SAMPLE);
        StacktraceTreeModel stacktraceTreeModel = new StacktraceTreeModel(collection,
                new FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false));

        var node = stacktraceTreeModel.getRoot();
        var root = buildJsonObject(node);

        JSONArray array = root.getJSONArray("children");
        int value = StreamSupport.stream(array.spliterator(), false).mapToInt(i -> ((JSONObject) i).getInt("value")).sum();
        root.put("value", value);

        Files.writeString(Paths.get(output), """
                <head>
                    <link rel="stylesheet" type="text/css" href="https://cdn.jsdelivr.net/npm/d3-flame-graph@4.1.3/dist/d3-flamegraph.css">
                </head>
                <body>
                <div id="chart" style="width: 90vw"></div>
                <script type="text/javascript" src="https://d3js.org/d3.v7.js"></script>
                <script type="text/javascript" src="https://cdn.jsdelivr.net/npm/d3-flame-graph@4.1.3/dist/d3-flamegraph.min.js"></script>
                <script type="text/javascript">
                    var chart = flamegraph()
                        .width(window.screen.width * 0.9);
                        
                    d3.select("#chart")
                        .datum(JSON.parse(`
                %s
                        `))
                        .call(chart);
                </script>
                </body>
                """.formatted(root.toString(4)));
    }

    private static JSONObject buildJsonObject(Node node) {
        var obj = new JSONObject();
        var frame = node.getFrame();
        var method = frame.getHumanReadableShortString();
        var pkg = FormatToolkit.getPackage(frame.getMethod().getType().getPackage());
        var count = node.getCumulativeWeight();

        obj.put("name", pkg + "." + method);
        obj.put("value", count);

        var children = new JSONArray();
        for (var child : node.getChildren()) {
            children.put(buildJsonObject(child));
        }
        obj.put("children", children);

        return obj;
    }

}

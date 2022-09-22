package me.imlc;

import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
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

    private static final String ANY_STRING = "ARG_ANY_STRING";
    public static void main(String[] args) throws InterruptedException, CouldNotLoadRecordingException, IOException {

        var source = args[1];

        IItemCollection collection = JfrLoaderToolkit.loadEvents(new File(source));
        if(stringsEqual(args, "--from", ANY_STRING, "threaddump")) {
            printThreads(args, collection);
        } else if (stringsEqual(args, "--from", ANY_STRING, "--to", ANY_STRING)) {
            generateFlameGraph(args, collection);
        } else {
            System.out.println(HELP_MSG);
        }
    }

    private static boolean stringsEqual(String[] args, String ... expectedArgs) {
        if(args.length != expectedArgs.length) {
            return false;
        }

        for (int i = 0; i < args.length; i++) {

            var expectedArg = expectedArgs[i];
            if(expectedArg == ANY_STRING) {
                continue;
            }

            if (!Objects.equals(args[i], expectedArg)) {
                return false;
            }
        }

        return true;
    }

    private static void generateFlameGraph(String[] args, IItemCollection collection)
        throws IOException {
        var source = args[1];
        var output = args[3];

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

    private static void printThreads(String[] _args, IItemCollection collection) {
        var threadCollection = collection.apply(JdkFilters.THREAD_DUMP);
        for (IItemIterable itemIterable : threadCollection) {
            IMemberAccessor<String, IItem> accessor = JdkAttributes.THREAD_DUMP_RESULT.getAccessor(
                itemIterable.getType());
            IMemberAccessor<IQuantity, IItem> stAccessor = JfrAttributes.END_TIME.getAccessor(itemIterable.getType());
            for (IItem item : itemIterable) {
                println("");
                println("=== " + stAccessor.getMember(item).displayUsing(IDisplayable.AUTO) + " ===");
                println("");
                println(accessor.getMember(item));
            }
        }
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

    private static void println(String msg) {
        System.out.println(msg);
    }
}

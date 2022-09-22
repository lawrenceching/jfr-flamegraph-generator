package me.imlc;

import org.junit.jupiter.api.Test;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void jfr() throws CouldNotLoadRecordingException, IOException, InterruptedException {
        Main.main(
                new String[] {
                        "--from",
                        "native-image/jetty.jfr",
                        "--to",
                        "target/jetty.html"
                }
        );
    }


    @Test
    void threaddump() throws CouldNotLoadRecordingException, IOException, InterruptedException {
        Main.main(
                new String[] {
                        "--from",
                        "native-image/jetty.jfr",
                        "threaddump"
                }
        );
    }
}
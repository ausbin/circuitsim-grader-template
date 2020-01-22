module tester {
    requires javafx.controls;
    requires com.google.gson;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires org.junit.jupiter.engine;
    requires org.junit.platform.launcher;
    requires circuitsim;

    exports edu.gatech.cs2110.circuitsim.extension;
    exports edu.gatech.cs2110.circuitsim.launcher;
    exports edu.gatech.cs2110.circuitsim.tests;
}

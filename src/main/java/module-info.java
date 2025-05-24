module com.depavlo.gitlaberfx {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.depavlo.gitlaberfx to javafx.fxml;
    exports com.depavlo.gitlaberfx;
    opens com.depavlo.gitlaberfx.controller to javafx.fxml;
    exports com.depavlo.gitlaberfx.controller;
}
module com.st.cloud.gametool {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.jfr;
    requires cn.hutool;
    requires java.sql;
    requires java.net.http;
    requires io.netty.transport;
    requires io.netty.handler;
    requires io.netty.codec.http;
    requires io.netty.common;
    requires com.google.protobuf;
    requires io.netty.buffer;
    requires static lombok;
    opens com.st.cloud.gametool to javafx.fxml;
    exports com.st.cloud.gametool;
    exports com.st.cloud.gametool.controller;
    opens com.st.cloud.gametool.controller to javafx.fxml;
    exports com.st.cloud.gametool.config;
    opens com.st.cloud.gametool.config to javafx.fxml;
}
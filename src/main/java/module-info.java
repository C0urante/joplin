module io.github.c0urante.joplin {
    requires java.desktop;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires org.bouncycastle.tls;

    exports io.github.c0urante.joplin;
    exports io.github.c0urante.joplin.errors;
}
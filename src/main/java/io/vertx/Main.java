package io.vertx;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class Main {
    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        // Enable multipart form data parsing
        router.route().handler(BodyHandler.create());
        router.route("/").handler(routingContext -> {
            routingContext.response().putHeader("content-type", "text/html").end(
                    "<fieldset>" +
                            "<legend> <h1>Parameters For POS Simulation </h1></legend>" +
                            "<form action=\"/ecash\" method=\"post\">\n" +
                            "    <div>\n" +
                            "        <label for=\"pos\">No Of POS terminal:</label>\n" +
                            "        <input type=\"text\" name=\"pos\" id=\"pos\" />\n " +
                            "        <label for=\"issue\">No Of Issuing Kiosk:</label>\n" +
                            "        <input type=\"text\" id=\"issue\" name=\"issue\" />\n " + " <br>"+ " <br>"+
                            "        <label for=\"not\">No Of Transaction per Refresh:</label>\n" +
                            "        <input type=\"text\" name=\"not\" id=\"not\" />\n " +
                            "        <label for=\"user\">No Of Users:</label>\n" +
                            "        <input type=\"text\" id=\"user\" name=\"user\" />\n <br><br>" +
                            "    </div>\n" +
                            "    <div class=\"button\">\n" +
                            "        <button type=\"submit\">Start Simulation</button>\n" +
                            "    </div>" +
                            "</form>" +
                            "</fieldset>"
            );
        });

        // handle the form
        router.post("/ecash").handler(ctx -> {
            ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/plain");

            // note the form attribute matches the html form element name.
            String pos,issue,user,not;
            pos = ctx.request().getParam("pos");
            issue = ctx.request().getParam("issue");
            user = ctx.request().getParam("user");
            not = ctx.request().getParam("not");

            vertx.deployVerticle(new MyFirstVerticle(pos,issue,user ,not));

            // Redirecting

            ctx.response().putHeader("content-type", "text/html").end(
                    "<!DOCTYPE html>\n" +
                            "<html>    \n" +
                            "<head><title>Simulation Started</title></head>\n" +
                            "<body>\n" +
                            "\n" +
                            "    <br/>\n" +
                            "\n" +
                            " <h1> Simulation started at port no :7778 <br> Keep Refreshing the page to get updated results. </h1> <br>"+
                            "    <form id='simulation' action='http://localhost:7778/' method='post'>\n" +
                            "    <div class=\"button\">\n" +
                            "        <button type=\"submit\">Click Here To See the Transactions </button>\n" +
                            "    </div>" +
                            "    </form>         \n" +
                            "</body>    \n" +
                            "</html>"
            );
        });
        vertx.createHttpServer().requestHandler(router::accept).listen(7777);

    }
}

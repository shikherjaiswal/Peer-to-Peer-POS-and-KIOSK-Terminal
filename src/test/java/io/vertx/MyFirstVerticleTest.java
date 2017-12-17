package io.vertx;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class MyFirstVerticleTest {

    private Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(new MyFirstVerticle("10","20","50","10"),
                context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testMyApplication(TestContext context) {
        final Async async = context.async();

        vertx.createHttpClient().getNow(7778, "localhost", "/?qty=65",
                response -> {
            //response.ha\\\\
                    response.handler(body -> {
                        if(body.toString().contains("65"))
                            System.out.println("Test complete");
                        else if (body.toString().contains("Input"))
                        {
                            System.out.println("No parameters passed");
                        }
                        context.assertTrue(body.toString().contains("70"));
                        async.complete();
                    });
                });
    }
}
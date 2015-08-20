package ru.home.edu.mongodb;

import com.mongodb.*;
import freemarker.template.Configuration;
import freemarker.template.Template;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import java.io.StringWriter;

public class MSFstyle {
    public static void main(String[] args) throws Exception{
        final Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(MSFstyle.class, "/");

        MongoClient client = new MongoClient();

        DB db = client.getDB("course");
        final DBCollection collection = db.getCollection("hello");

        collection.drop();

        collection.insert(new BasicDBObject("name", "MongoDB"));

        Spark.get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {
                StringWriter writer = new StringWriter();
                try {
                    Template helloTemplate =
                            configuration.getTemplate("hello.ftl");

                    DBObject document = collection.find().one();

                    helloTemplate.process(document, writer);
                } catch (Exception e) {
                    halt(500);
                    e.printStackTrace();
                }
                return writer;
            }
        });
    }
}

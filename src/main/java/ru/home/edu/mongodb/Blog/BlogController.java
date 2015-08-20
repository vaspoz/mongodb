package ru.home.edu.mongodb.Blog;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.bson.Document;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.StringWriter;

import static spark.Spark.get;

public class BlogController {

    public static void main(String[] args) {
        final Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(BlogController.class, "/");

        get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {
                StringWriter writer = new StringWriter();

                try {
                    Template template = configuration
                            .getTemplate("welcome.ftl");

                    Document document = getDocumentWithName();
                    System.out.println(document);

                    template.process(document, writer);

                } catch (Exception e) {
                    halt(500);
                    e.printStackTrace();
                }
                return writer;
            }
        });

        get(new Route("/logout") {
            @Override
            public Object handle(Request request, Response response) {
                return "(not yet implemented)";
            }
        });
    }

    private static Document getDocumentWithName() {
        try {
            MongoClient client = new MongoClient();

            MongoDatabase db = client.getDatabase("course");
            MongoCollection<Document> coll = db.getCollection("users");

            return coll.find().first();

        } catch (Exception e) {
            e.printStackTrace();
            return new Document("name", "null");
        }
    }
}

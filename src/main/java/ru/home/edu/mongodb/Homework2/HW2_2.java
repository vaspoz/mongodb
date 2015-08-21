package ru.home.edu.mongodb.Homework2;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Iterator;

public class HW2_2 {
    public static void main(String[] args) {
        MongoClient client = new MongoClient();

        MongoDatabase db = client.getDatabase("students");
        MongoCollection<Document> collection = db.getCollection("grades");

        Iterator<Document> iter = collection.find(new Document("type", "homework"))
                .sort(new Document("student_id", 1).append("score", -1))
                .iterator();

        Document temp = iter.next();
        int student = temp.getInteger("student_id");
        while (iter.hasNext()) {
            Document current = iter.next();
            int currStudentID = current.getInteger("student_id");

            if (currStudentID == student) {
                collection.deleteOne(current);
            } else
                student = currStudentID;
        }

        iter = collection.find().iterator();
        showCollection(iter);
    }

    private static void showCollection(Iterator iter) {
        while (iter.hasNext()) {
            System.out.println(iter.next());
        }
    }
}

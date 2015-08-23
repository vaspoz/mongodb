import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

/**
 * Created by Василий on 23.08.2015.
 */
public class BlogPostDAO {
    MongoCollection<Document> postsCollection;

    public BlogPostDAO(final MongoDatabase blogDatabase) {
        postsCollection = blogDatabase.getCollection("posts");
    }

    public Document findByPermalink(String permalink) {
        Document post = null;

        post = postsCollection.find(eq("permalink", permalink)).first();

        return post;
    }

    public List<Document> findByDateDescending(int limit) {
        List<Document> posts = new ArrayList<>();

        postsCollection
                .find()
                .sort(descending("date"))
                .limit(limit).into(posts);

        return posts;
    }

    public String addPost(String title, String body, List tags, String username) {

        System.out.println("Inserting blog entry " + title + " " + body);

        String permalink = title.trim().replaceAll("\\s", "_");
        permalink = permalink.replaceAll("\\W", "");
        permalink = permalink.toLowerCase();

        Document post = new Document()
                .append("title", title)
                .append("author", username)
                .append("body", body)
                .append("permalink", permalink)
                .append("tags", tags)
                .append("comments", new ArrayList<>())
                .append("date", new Date());

        postsCollection.insertOne(post);

        return permalink;
    }

    public void addPostComment(final String name, final String email,
                               final String body,
                               final String permalink) {

        // XXX HW 3.3, Work Here
        // Hints:
        // - email is optional and may come in NULL. Check for that.
        // - best solution uses an update command to the database and a suitable
        //   operator to append the comment on to any existing list of comments

        Document editedPost = findByPermalink(permalink);
        Document comment = new Document()
                .append("author", name)
                .append("body", body);
        if (email != null) {
            comment.append("email", email);
        }

        postsCollection.updateOne(eq("permalink", permalink),
                new Document("$push", new Document("comments", comment)));

    }
}

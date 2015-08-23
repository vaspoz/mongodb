import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.bson.Document;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class BlogController {
    private final Configuration cfg;
    private final UserDAO userDAO;
    private final SessionDAO sessionDAO;
    private final BlogPostDAO blogPostDAO;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            new BlogController("mongodb://localhost");
        } else {
            new BlogController(args[0]);
        }
    }

    public BlogController(String mongoURIString) throws IOException {
        final MongoClient mongoClient = new MongoClient(
                new MongoClientURI(mongoURIString));
        final MongoDatabase blogDatabase = mongoClient.getDatabase("blog");

        blogPostDAO = new BlogPostDAO(blogDatabase);
        userDAO = new UserDAO(blogDatabase);
        sessionDAO = new SessionDAO(blogDatabase);

        cfg = createFreemarkerConfiguration();
        setPort(8082);
        initializeRoutes();
    }

    abstract class FreemarkerBasedRoute extends Route {
        final Template template;

        protected FreemarkerBasedRoute(final String path, final String templateName) throws IOException {
            super(path);
            template = cfg.getTemplate(templateName);
        }

        @Override
        public Object handle(Request request, Response response) {
            StringWriter writer = new StringWriter();
            try {
                doHandle(request, response, writer);
            } catch (Exception e) {
                e.printStackTrace();
                response.redirect("/internal_error");
            }
            return writer;
        }

        protected abstract void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException;


    }

    private void initializeRoutes() throws IOException {

        get(new FreemarkerBasedRoute("/", "blog_template.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                String username = sessionDAO.findUsernameBySessionId(getSessionCookie(request));

                List<Document> posts = blogPostDAO.findByDateDescending(10);
                SimpleHash root = new SimpleHash();

                root.put("myposts", posts);
                if (username != null) {
                    root.put("username", username);
                } else {
                    response.redirect("/login");
                }
                template.process(root, writer);
            }
        });

        get(new FreemarkerBasedRoute("/post/:permalink", "entry_template.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                String permalink = request.params(":permalink");

                System.out.println("/post: get " + permalink);

                Document post = blogPostDAO.findByPermalink(permalink);
                if (post == null) {
                    response.redirect("/post_not_found");
                } else {

                    SimpleHash newComment = new SimpleHash();
                    newComment.put("name", "");
                    newComment.put("email", "");
                    newComment.put("body", "");

                    SimpleHash root = new SimpleHash();

                    root.put("post", post);
                    root.put("comment", newComment);

                    template.process(root, writer);
                }
            }
        });

        get(new FreemarkerBasedRoute("/newpost", "newpost_template.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                String username = sessionDAO.findUsernameBySessionId(getSessionCookie(request));

                if (username == null) {
                    response.redirect("/login");
                } else {
                    SimpleHash root = new SimpleHash();
                    root.put("username", username);

                    template.process(root, writer);
                }
            }
        });
        post(new FreemarkerBasedRoute("/newpost", "newpost_template.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                String title = StringEscapeUtils.escapeHtml4(request.queryParams("subject"));
                String post = StringEscapeUtils.escapeHtml4(
                        request.queryParams("body")
                );
                String tags = StringEscapeUtils.escapeHtml4(
                        request.queryParams("tags")
                );

                String username = sessionDAO.findUsernameBySessionId(
                        getSessionCookie(request));

                if (username == null) {
                    response.redirect("/login");
                } else {
                    if (title.equals("") || post.equals("")) {
                        Map<String, String> root = new HashMap<>();

                        root.put("errors", "Post must contain a title and blog entry.");
                        root.put("subject", title);
                        root.put("username", username);
                        root.put("tags", tags);
                        root.put("body", post);

                        template.process(root, writer);
                    } else {
                        List<String> tagsArray = extractTags(tags);

                        post = post.replaceAll("\\r?\\n", "<p>");

                        String permalink = blogPostDAO.addPost(title, post,
                                tagsArray, username);

                        response.redirect("/post/" + permalink);
                    }
                }
            }
        });

        post(new FreemarkerBasedRoute("/newcomment", "entry_template.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                String name = StringEscapeUtils.escapeHtml4(request.queryParams("commentName"));
                String email = StringEscapeUtils.escapeHtml4(request.queryParams("commentEmail"));
                String body = StringEscapeUtils.escapeHtml4(request.queryParams("commentBody"));
                String permalink = request.queryParams("permalink");

                Document post = blogPostDAO.findByPermalink(permalink);
                if (post == null) {
                    response.redirect("/post_not_found");
                }
                // check that comment is good
                else if (name.equals("") || body.equals("")) {
                    // bounce this back to the user for correction
                    SimpleHash root = new SimpleHash();
                    SimpleHash comment = new SimpleHash();

                    comment.put("name", name);
                    comment.put("email", email);
                    comment.put("body", body);
                    root.put("comment", comment);
                    root.put("post", post);
                    root.put("errors", "Post must contain your name and an actual comment");

                    template.process(root, writer);
                } else {
                    blogPostDAO.addPostComment(name, email, body, permalink);
                    response.redirect("/post/" + permalink);
                }
            }
        });

        post(new FreemarkerBasedRoute("/signup", "signup.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                String email = request.queryParams("email");
                String username = request.queryParams("username");
                String password = request.queryParams("password");
                String verify = request.queryParams("verify");

                Map<String, String> root = new HashMap<>();
                root.put("username", StringEscapeUtils.escapeHtml4(username));
                root.put("email", StringEscapeUtils.escapeHtml4(email));

                if (validateSignup(username, password, verify, email, root)) {
                    if (!userDAO.addUser(username, password, email)) {
                        root.put("username_error", "Username already in use.");
                        template.process(root, writer);
                    } else {
                        String sessionID = sessionDAO.startSession(username);

                        response.raw().addCookie(new Cookie("session", sessionID));
                        response.redirect("/welcome");
                    }
                } else {
                    template.process(root, writer);
                }
            }
        });
        get(new FreemarkerBasedRoute("/signup", "signup.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                SimpleHash root = new SimpleHash();

                root.put("username", "");
                root.put("password", "");
                root.put("email", "");
                root.put("password_error", "");
                root.put("username_error", "");
                root.put("email_error", "");
                root.put("verify_error", "");

                template.process(root, writer);
            }
        });

        get(new FreemarkerBasedRoute("/welcome", "welcome.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {

                String cookie = getSessionCookie(request);
                String username = sessionDAO.findUsernameBySessionId(cookie);

                if (username == null) {
                    response.redirect("/signup");
                } else {
                    SimpleHash root = new SimpleHash();

                    root.put("username", username);

                    template.process(root, writer);
                }
            }
        });

        get(new FreemarkerBasedRoute("/login", "login.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                SimpleHash root = new SimpleHash();

                root.put("username", "");
                root.put("login_error", "");

                template.process(root, writer);
            }
        });
        post(new FreemarkerBasedRoute("/login", "login.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                String username = request.queryParams("username");
                String password = request.queryParams("password");

                Document user = userDAO.validateLogin(username, password);

                if (user != null) {

                    String sessionID = sessionDAO.startSession(user.get("_id").toString());

                    if (sessionID == null) {
                        response.redirect("/internal_error");
                    } else {

                        response.raw().addCookie(new Cookie("session", sessionID));

                        response.redirect("/welcome");
                    }
                } else {
                    SimpleHash root = new SimpleHash();

                    root.put("username", StringEscapeUtils.escapeHtml4(username));
                    root.put("password", "");
                    root.put("login_error", "Invalid login");

                    template.process(root, writer);
                }
            }
        });

        get(new FreemarkerBasedRoute("/logout", "signup.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {

                String sessionID = getSessionCookie(request);

                if (sessionID == null) {
                    response.redirect("/login");
                } else {

                    sessionDAO.endSession(sessionID);

                    Cookie c = getSessionCookieActual(request);
                    c.setMaxAge(0);

                    response.raw().addCookie(c);

                    response.redirect("/login");
                }
            }
        });

        get(new FreemarkerBasedRoute("/post_not_found", "post_not_found.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                template.process(new SimpleHash(), writer);
            }
        });

        get(new FreemarkerBasedRoute("/internal_error", "error_template.ftl") {
            @Override
            protected void doHandle(Request request, Response response, Writer writer) throws IOException, TemplateException {
                SimpleHash root = new SimpleHash();

                root.put("error", "System has encountered an error.");
                template.process(root, writer);
            }
        });
    }

    public boolean validateSignup(String username, String password, String verify, String email,
                                  Map<String, String> errors) {
        String USER_RE = "^[a-zA-Z0-9_-]{3,20}$";
        String PASS_RE = "^.{3,20}$";
        String EMAIL_RE = "^[\\S]+@[\\S]+\\.[\\S]+$";

        errors.put("username_error", "");
        errors.put("password_error", "");
        errors.put("verify_error", "");
        errors.put("email_error", "");

        if (!username.matches(USER_RE)) {
            errors.put("username_error", "invalid username. try just letters and numbers");
            return false;
        }

        if (!password.matches(PASS_RE)) {
            errors.put("password_error", "invalid password.");
            return false;
        }


        if (!password.equals(verify)) {
            errors.put("verify_error", "password must match");
            return false;
        }

        if (!email.equals("")) {
            if (!email.matches(EMAIL_RE)) {
                errors.put("email_error", "Invalid Email Address");
                return false;
            }
        }

        return true;
    }

    private String getSessionCookie(final Request request) {
        if (request.raw().getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.raw().getCookies()) {
            if (cookie.getName().equals("session")) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private Cookie getSessionCookieActual(final Request request) {
        if (request.raw().getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.raw().getCookies()) {
            if (cookie.getName().equals("session")) {
                return cookie;
            }
        }
        return null;
    }

    private Configuration createFreemarkerConfiguration() {
        Configuration retVal = new Configuration();
        retVal.setClassForTemplateLoading(BlogController.class, "/freemarker");
        return retVal;
    }

    private List<String> extractTags(String tags) {
        tags = tags.replaceAll("\\s", "");
        String[] tagArray = tags.split(",");

        List<String> clenedList = new ArrayList<>();
        for (String tag : tagArray) {
            if (!tag.equals("") && !clenedList.contains(tag)) {
                clenedList.add(tag);
            }
        }

        return clenedList;
    }
}

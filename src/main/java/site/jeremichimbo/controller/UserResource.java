package site.jeremichimbo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import site.jeremichimbo.api.openlib.Book;
import site.jeremichimbo.api.tomcat.User;
import site.jeremichimbo.model.tomcat.UserDAO;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Objects;


@RestController
@CrossOrigin("*")
@RequestMapping("/booklist")
public class UserResource {
    private final UserDAO db;
    @Value("${book.url.customProperty}")
    private String bookUrl;

    @Autowired
    public UserResource(UserDAO db) {
        this.db = db;
    }
    Logger logger = LoggerFactory.getLogger(UserResource.class);



    @GetMapping
    public ResponseEntity<ArrayList<Book>> getUserBookList(@AuthenticationPrincipal OAuth2User principal) throws MalformedURLException, JsonProcessingException {
          String email = Objects.requireNonNull(principal.getAttribute("email")).toString().replaceAll(" ", "_");
            if (db.existsByEmail(email)) {
                ArrayList<String> isbn_list = new ArrayList<>();
                for (User a : db.findIsbnByEmail(email)) {
                    isbn_list.add(a.getIsbn());
                }
                User userWithBook_list = new User(email, isbn_list);
                userWithBook_list.setBookUrl(bookUrl);
                logger.info(email + "'s book list was found for " + principal.getAttribute("email") );
                ArrayList<Book> bookArrayList = userWithBook_list.getBook_list();
                return new ResponseEntity<>(bookArrayList, HttpStatusCode.valueOf(200));
            }else {
                logger.info(email + "'s book list was not found for " + principal.getAttribute("email") );

                return new ResponseEntity<>( HttpStatusCode.valueOf(404));
            }
    }
    @PostMapping(value = "/{isbn}", produces = "application/json")
    public ResponseEntity<User> addToBookList(@PathVariable("isbn") String isbn, @AuthenticationPrincipal OAuth2User principal) {
        String email = Objects.requireNonNull(principal.getAttribute("email")).toString().replaceAll(" ", "_");
        if (!isbn.isEmpty() && !isbn.isBlank()) {
                    User user = new User(email, isbn);
                    db.save(user);
                    logger.info(isbn +" was added to "+ email + "'s book list by" + principal.getAttribute("email") );
                    return new ResponseEntity<>(user, HttpStatusCode.valueOf(201));
                } else {
                    logger.info(isbn +" was not added to "+ email + "'s book list by" + principal.getAttribute("email") );
                    return new ResponseEntity<>(HttpStatusCode.valueOf(404));
                }
    }

    @Transactional
    @PostMapping(value = "/delete/{isbn}", produces = "application/json")
    public ResponseEntity<User> removeBook( @PathVariable("isbn") String isbn, @AuthenticationPrincipal OAuth2User principal) {
        String email = Objects.requireNonNull(principal.getAttribute("email")).toString().replaceAll(" ", "_");
            if (db.existsByEmail(email)) {
                if (!isbn.isEmpty())    {
                    db.deleteByEmailAndIsbn(email, isbn);
                    logger.info(isbn +" was deleted from "+ email + "'s book list by" + principal.getAttribute("email") );
                    return new ResponseEntity<>(new User(email, isbn), HttpStatusCode.valueOf(200));
                } else {
                    logger.error(isbn +" was not deleted from "+ email + "'s book list by" + principal.getAttribute("email" + "because of 404 Error") );
                }
            }
            return new ResponseEntity<>(HttpStatusCode.valueOf(404));
        }



}

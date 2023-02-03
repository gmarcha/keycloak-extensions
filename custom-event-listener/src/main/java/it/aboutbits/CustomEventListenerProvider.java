package it.aboutbits;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(CustomEventListenerProvider.class);

    private final KeycloakSession session;
    private final RealmProvider model;

    public CustomEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public void onEvent(Event event) {

        System.out.println("Hey, I'm in the onEvent method!");

        if (EventType.LOGIN.equals(event.getType())) {
            log.infof("## NEW %s EVENT", event.getType());
            log.info("-----------------------------------------------------------");

            RealmModel realm = this.model.getRealm(event.getRealmId());
            UserModel newRegisteredUser = this.session.users().getUserById(realm, event.getUserId());

            log.infof("## USER ID: %s, USERNAME: %s, EMAIL: %s, FIRST NAME: %s, LAST NAME: %s", newRegisteredUser.getId(), newRegisteredUser.getUsername(), newRegisteredUser.getEmail(), newRegisteredUser.getFirstName(), newRegisteredUser.getLastName());
            log.info("-----------------------------------------------------------");

            try {
              String query = "mutation insertUser ($user: user_insert_input!) { insert_user_one(object: $user, on_conflict: { constraint: user_pkey, update_columns: [username, email, displayname] }) { id }}";
              String userObject = "{\"id\":\"" + newRegisteredUser.getId() + "\", \"username\":\"" + newRegisteredUser.getUsername() + "\", \"email\":\"" + newRegisteredUser.getEmail() + "\"}";
              String userMutation = "{\"query\":\"" + query + "\",\"variables\":{\"user\":" + variables + "}}";
              Object requestBody = new ObjectMapper().readValue(userMutation, Object.class);
              SimpleHttp request = SimpleHttp.doPost("http://hasura.hasura/v1/graphql", session).json(requestBody);
              // if (sharedSecret != null) {
              //   request.header(
              //       "X-Keycloak-Signature",
              //       hmacFor(task.getEvent(), sharedSecret, algorithm.orElse(HMAC_SHA256_ALGORITHM)));
              // }
              SimpleHttp.Response response = request.asResponse();
              int status = response.getStatus();
              String body = response.asString();

              log.infof("## HTTP RESPONSE STATUS: %d", status);
              log.infof("## HTTP RESPONSE BODY: %s", body);
              log.info("-----------------------------------------------------------");
            }
            catch (Exception e) {
                log.infof("## ERROR: %s", e.getMessage());
                log.info("-----------------------------------------------------------");
            }
        }

        if (EventType.REGISTER.equals(event.getType())) {
            log.infof("## NEW %s EVENT", event.getType());
            log.info("-----------------------------------------------------------");

            RealmModel realm = this.model.getRealm(event.getRealmId());
            UserModel newRegisteredUser = this.session.users().getUserById(realm, event.getUserId());

            log.infof("## USER_ID: %s, USERNAME: %s", newRegisteredUser.getId(), newRegisteredUser.getUsername());
            log.info("-----------------------------------------------------------");
        }

    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {

    }
}

package com.bcp.workshop.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final Logger LOGGER = Logger.getLogger(AuthResource.class.getName());
    private static final String INTERNAL_ERROR_MESSAGE = "Authentication failed";
    private static final String INTERNAL_ERROR_TYPE = "InternalServerError";
    private static final String AUTHENTICATED_DEBUG_VALUE = "authenticated-with-legacy-flow";

    @Inject
    AuthService authService;

    @Inject
    AuthTokenService tokenService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        Response validationFailure = validateRequest(request);
        if (validationFailure != null) {
            return validationFailure;
        }

        try {
            UserRecord user = authService.authenticate(request.username, request.password);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "Invalid user " + request.username + " or password"))
                        .build();
            }

            return Response.ok(createLoginResponse(user)).build();
        } catch (RuntimeException error) {
            LOGGER.log(Level.SEVERE, "Authentication request failed", error);
            return Response.status(500).entity(Map.of(
                    "message", INTERNAL_ERROR_MESSAGE,
                    "exception", INTERNAL_ERROR_TYPE
            )).build();
        }
    }

    private Response validateRequest(LoginRequest request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Request is null"))
                    .build();
        }
        if (request.username == null || request.username.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Username is required"))
                    .build();
        }
        if (request.password == null || request.password.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Password is required"))
                    .build();
        }
        return null;
    }

    private LoginResponse createLoginResponse(UserRecord user) {
        LoginUserResponse userResponse = new LoginUserResponse(
                user.username(),
                StringEscapeUtils.escapeHtml4(user.displayName()),
                user.role()
        );
        return new LoginResponse(
                tokenService.create(user.username(), user.role()),
                AuthTokenService.EXPIRATION_SECONDS,
                userResponse,
                AUTHENTICATED_DEBUG_VALUE
        );
    }
}

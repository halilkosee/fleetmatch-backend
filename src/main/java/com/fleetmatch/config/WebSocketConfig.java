package com.fleetmatch.config;

import com.fleetmatch.messaging.repository.ConversationRepository;
import com.fleetmatch.security.jwt.JwtService;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.security.user.CustomUserDetailsService;
import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.user.service.UserVerificationGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.Arrays;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final UserVerificationGuard userVerificationGuard;

    @Value("${fleetmatch.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(origin -> !origin.isBlank())
                                .toArray(String[]::new)
                );
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    authenticate(accessor);
                }

                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    validateSubscription(accessor);
                }

                return message;
            }
        });
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing websocket bearer token");
        }

        String email = jwtService.extractUsername(authorization.substring(7));
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService
                .loadUserByUsername(email);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        accessor.setUser(authentication);
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        String destination = accessor.getDestination();

        if (!(principal instanceof UsernamePasswordAuthenticationToken authentication) ||
                !(authentication.getPrincipal() instanceof CustomUserDetails currentUser)) {
            throw new AccessDeniedException("Unauthenticated websocket subscription");
        }

        if (destination == null ||
                !destination.startsWith("/topic/conversations/")) {
            return;
        }

        UUID conversationId = UUID.fromString(
                destination.substring("/topic/conversations/".length())
        );
        validateConversationAccess(conversationId, currentUser);
    }

    private void validateConversationAccess(
            UUID conversationId,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        if (user.getCompany() == null) {
            throw new AccessDeniedException("Only company users can access conversations");
        }

        if (user.getCompanyUserRole() == CompanyUserRole.DRIVER) {
            throw new AccessDeniedException("Drivers cannot use messaging");
        }

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        var conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AccessDeniedException("Conversation not found"));

        boolean participant =
                conversation.getBrokerCompany().getId().equals(user.getCompany().getId()) ||
                        conversation.getFleetCompany().getId().equals(user.getCompany().getId());

        if (!participant) {
            throw new AccessDeniedException(
                    "You can only access conversations for your company"
            );
        }
    }
}

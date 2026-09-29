package com.rhpaie.chat.service;

import com.rhpaie.chat.context.InMemoryConversationContextStore;
import com.rhpaie.chat.context.ReadFollowUpResolver;
import com.rhpaie.chat.dto.IntentPrediction;
import com.rhpaie.chat.entity.ChatIntent;
import com.rhpaie.chat.faq.repository.ChatFaqDocumentRepository;
import com.rhpaie.chat.repository.ChatActionDraftRepository;
import com.rhpaie.chat.repository.ChatMessageRepository;
import com.rhpaie.leave.service.LeaveRequestService;
import com.rhpaie.user.entity.Role;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagRetrievalTest {

    @Mock private ChatFaqDocumentRepository chatFaqDocumentRepository;
    @Mock private EmbeddingClient embeddingClient;
    @Mock private UserRepository userRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatActionDraftRepository chatActionDraftRepository;
    @Mock private IntentClassifierClient intentClassifierClient;
    @Mock private ChatToolService chatToolService;
    @Mock private OllamaClient ollamaClient;
    @Mock private LeaveRequestService leaveRequestService;
    @Mock private AdminLeaveReviewService adminLeaveReviewService;

    private RagService ragService;

    @BeforeEach
    void setUp() {
        when(chatFaqDocumentRepository.findAll()).thenReturn(List.of());
        when(embeddingClient.embed(any())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            List<double[]> vectors = new ArrayList<>();
            for (String text : texts) {
                double[] vector = new double[4];
                if (text.startsWith("Solde de congé")) {
                    vector[0] = 1;
                } else if (text.startsWith("Comment demander")) {
                    vector[1] = 1;
                } else {
                    vector[2] = 1;
                }
                vectors.add(vector);
            }
            return vectors;
        });
        ragService = new RagService(chatFaqDocumentRepository, embeddingClient);
        ragService.reload();
    }

    @Test
    void strongMatchReturnsTheExpectedSection() {
        when(embeddingClient.embedOne(anyString())).thenReturn(new double[]{1, 0, 0, 0});
        String retrieved = ragService.retrieve("jours restants", ChatIntent.FAQ_LEAVE);
        assertTrue(retrieved.startsWith("Solde de congé"));
    }

    @Test
    void weakAmbiguousTopPairAbstains() {
        when(embeddingClient.embedOne(anyString())).thenReturn(new double[]{0.36, 0.35, 0, 1});
        assertEquals("", ragService.retrieve("rtt", ChatIntent.FAQ_LEAVE));
    }

    @Test
    void weakButSeparatedTopMatchIsAccepted() {
        when(embeddingClient.embedOne(anyString())).thenReturn(new double[]{0.36, 0.05, 0, 1});
        String retrieved = ragService.retrieve("jours acquis", ChatIntent.FAQ_LEAVE);
        assertTrue(retrieved.startsWith("Solde de congé"));
    }

    @Test
    void weakMatchAbstains() {
        when(embeddingClient.embedOne(anyString())).thenReturn(new double[]{0, 0, 0, 1});
        assertEquals("", ragService.retrieve("taux CNSS", ChatIntent.FAQ_PAYROLL));
    }

    @Test
    void retrievalReturnsAtMostThreeSections() {
        when(embeddingClient.embedOne(anyString())).thenReturn(new double[]{0, 0, 1, 0});
        String retrieved = ragService.retrieve("guide", ChatIntent.FAQ_LEAVE);
        assertEquals(3, retrieved.split("\n\n").length);
    }

    @Test
    void blankFaqContextDoesNotCallOllama() {
        UserAccount ali = UserAccount.builder().id(1L).username("ali").role(Role.EMPLOYE).build();
        when(userRepository.findByUsername("ali")).thenReturn(Optional.of(ali));
        when(chatMessageRepository.findTop3ByUser_IdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(chatActionDraftRepository.findByUser_Id(any())).thenReturn(Optional.empty());
        when(intentClassifierClient.predictForRole(anyString(), eq(Role.EMPLOYE)))
                .thenReturn(new IntentPrediction(ChatIntent.FAQ_LEAVE.name(), 0.9));
        when(embeddingClient.embedOne(anyString())).thenReturn(new double[]{0, 0, 0, 1});

        ChatService chatService = new ChatService(
                userRepository,
                chatMessageRepository,
                chatActionDraftRepository,
                intentClassifierClient,
                chatToolService,
                ragService,
                ollamaClient,
                new ChatMemorySupport(),
                new LeaveDraftSlotFiller(new JavaLeaveDateExtractor()),
                leaveRequestService,
                new KeywordIntentFallback(),
                adminLeaveReviewService,
                new InMemoryConversationContextStore(),
                new ReadFollowUpResolver()
        );

        var response = chatService.reply("ali", "Quelle est la procédure décrite pour un justificatif ?");
        assertTrue(response.reply().contains("Je n'ai pas trouvé cette procédure"));
        verify(ollamaClient, never()).generate(anyString(), anyString(), anyString());
    }
}

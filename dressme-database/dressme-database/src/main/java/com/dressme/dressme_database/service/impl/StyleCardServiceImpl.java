package com.dressme.dressme_database.service.impl;
 
import com.dressme.dressme_database.model.StyleCard;
import com.dressme.dressme_database.model.User;
import com.dressme.dressme_database.model.UserOnboardingSelection;
import com.dressme.dressme_database.repository.StyleCardRepository;
import com.dressme.dressme_database.repository.UserOnboardingSelectionRepository;
import com.dressme.dressme_database.repository.UserRepository;
import com.dressme.dressme_database.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_database.schema.dto.StyleCardDTO;
import com.dressme.dressme_database.schema.dto.StyleCardEmbeddingResponse;
import com.dressme.dressme_database.service.StyleCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class StyleCardServiceImpl implements StyleCardService {
 
    private final StyleCardRepository styleCardRepository;
    private final UserOnboardingSelectionRepository selectionRepository;
    private final UserRepository userRepository;
 
    @Override
    public List<StyleCardDTO> getActiveStyleCards() {
        log.info("StyleCardService: Consultando tarjetas activas");
        return styleCardRepository
                .findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
 
    @Override
    public List<StyleCardEmbeddingResponse> getEmbeddingsByIds(List<UUID> ids) {
        log.info("StyleCardService: Consultando embeddings para {} tarjetas", ids.size());
        return styleCardRepository.findByIdIn(ids)
                .stream()
                .map(card -> new StyleCardEmbeddingResponse(
                        card.getId(),
                        card.getName(),
                        card.getEmbeddingVector()
                ))
                .collect(Collectors.toList());
    }
 
    @Override
    @Transactional
    public void saveSelections(OnboardingSelectionRequest request) {
        log.info("StyleCardService: Guardando {} selecciones para usuario {}",
                request.selections().size(), request.userId());
 
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado con ID: " + request.userId()));
 
        // Re-calibración: borrar selecciones previas antes de guardar las nuevas
        selectionRepository.deleteByUserId(request.userId());
        selectionRepository.flush();
 
        // Cargar las style cards de una sola vez para evitar N+1 queries
        List<UUID> cardIds = request.selections().stream()
                .map(OnboardingSelectionRequest.SelectionItem::styleCardId)
                .collect(Collectors.toList());
 
        Map<UUID, StyleCard> cardsById = styleCardRepository.findByIdIn(cardIds)
                .stream()
                .collect(Collectors.toMap(StyleCard::getId, Function.identity()));
 
        List<UserOnboardingSelection> entities = request.selections().stream()
                .map(item -> {
                    StyleCard card = cardsById.get(item.styleCardId());
                    if (card == null) {
                        throw new RuntimeException(
                                "StyleCard no encontrada con ID: " + item.styleCardId());
                    }
                    return UserOnboardingSelection.builder()
                            .user(user)
                            .styleCard(card)
                            .reaction(item.reaction())
                            .build();
                })
                .collect(Collectors.toList());
 
        selectionRepository.saveAll(entities);
        log.info("StyleCardService: {} selecciones guardadas correctamente", entities.size());
    }

        @Override
        @Transactional(readOnly = true)
        public List<UUID> getSelectedStyleCardIds(UUID userId) {
                log.info("StyleCardService: Consultando style cards seleccionadas para usuario {}", userId);
                return selectionRepository.findByUserId(userId).stream()
                                .map(selection -> selection.getStyleCard().getId())
                                .toList();
        }
 
    // ── Mapper privado ────────────────────────────────────────────────────────
 
    private StyleCardDTO toDTO(StyleCard card) {
        return new StyleCardDTO(
                card.getId(),
                card.getName(),
                card.getSemanticDescription(),
                card.getImageUrl(),
                card.getTags(),
                card.getDisplayOrder()
        );
    }
}